package br.com.nandesk.market.application;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.OpeningHours;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.market.infra.MarketRepository;
import br.com.nandesk.market.infra.SectorRepository;
import br.com.nandesk.shared.config.CacheConfig;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de feiras e setores (RF-29). Toda mudança publicada incrementa {@code dataVersion}. */
@Service
public class MarketAdminService {

    private final MarketRepository markets;
    private final SectorRepository sectors;

    public MarketAdminService(MarketRepository markets, SectorRepository sectors) {
        this.markets = markets;
        this.sectors = sectors;
    }

    public record Translation(String description, String howToGetThere) {}

    public record MarketCommand(
            String slug,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int radiusMeters,
            List<OpeningHours> openingHours,
            boolean active,
            Map<String, Translation> translations,
            Integer expectedVersion) {}

    public record SectorCommand(String slug, int sortOrder, boolean active, Map<String, String> names) {}

    public record AdminMarketView(
            String slug,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int radiusMeters,
            List<OpeningHours> openingHours,
            boolean active,
            long dataVersion,
            int version,
            Map<String, Translation> translations,
            List<AdminSectorView> sectors,
            String coverThumbUrl,
            String coverCredit,
            String coverCreditUrl) {}

    public record AdminSectorView(String slug, int sortOrder, boolean active, Map<String, String> names) {}

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public Market upsert(MarketCommand cmd) {
        Market market = markets.findBySlug(cmd.slug()).orElse(null);
        if (market == null) {
            market = new Market(
                    cmd.slug(),
                    cmd.name(),
                    cmd.address(),
                    scale(cmd.latitude()),
                    scale(cmd.longitude()),
                    cmd.radiusMeters());
        } else {
            if (cmd.expectedVersion() != null && cmd.expectedVersion() != market.getVersion()) {
                throw new BusinessException(ErrorCode.STALE_VERSION);
            }
            market.setName(cmd.name());
            market.setAddress(cmd.address());
            market.setLatitude(scale(cmd.latitude()));
            market.setLongitude(scale(cmd.longitude()));
            market.setGeofenceRadiusMeters(cmd.radiusMeters());
        }
        market.setOpeningHours(cmd.openingHours() == null ? List.of() : cmd.openingHours());
        market.setActive(cmd.active());
        if (!cmd.translations().containsKey(AppLocale.PT_BR.tag())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Market target = market;
        cmd.translations().forEach((locale, t) -> target.putTranslation(locale, t.description(), t.howToGetThere()));
        market.bumpDataVersion();
        return markets.save(market);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public Sector upsertSector(String marketSlug, SectorCommand cmd) {
        Market market = markets.findBySlug(marketSlug).orElseThrow(BusinessException::notFound);
        Sector sector = sectors.findByMarketIdAndSlug(market.getId(), cmd.slug())
                .orElseGet(() -> new Sector(market, cmd.slug(), cmd.sortOrder()));
        sector.setSortOrder(cmd.sortOrder());
        sector.setActive(cmd.active());
        cmd.names().forEach(sector::putName);
        Sector saved = sectors.save(sector);
        markets.bumpDataVersion(market.getId());
        return saved;
    }

    /**
     * Capa da feira enviada pelo painel (RF-01, RF-29). A anterior fica para a retenção apagar, já que nenhuma
     * outra tela a usa.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public AdminMarketView setCover(String slug, StoredFile cover, String credit, String creditUrl, Instant now) {
        Market market = require(slug);
        StoredFile previous = market.getCover();
        if (previous != null) {
            previous.setDeleteAfter(now);
        }
        market.setCover(cover);
        market.setCoverCredit(credit);
        market.setCoverCreditUrl(creditUrl);
        market.bumpDataVersion();
        return view(market);
    }

    /** Seed: capa só quando a feira ainda não tem uma (não sobrescreve a escolhida no painel). */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public void coverIfMissing(
            String slug, java.util.function.Supplier<StoredFile> cover, String credit, String creditUrl) {
        Market market = require(slug);
        if (market.getCover() == null) {
            market.setCover(cover.get());
            market.setCoverCredit(credit);
            market.setCoverCreditUrl(creditUrl);
            market.bumpDataVersion();
        }
    }

    @Transactional(readOnly = true)
    public List<AdminMarketView> listAll() {
        return markets.findAllByOrderByNameAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public AdminMarketView get(String slug) {
        return view(markets.findBySlug(slug).orElseThrow(BusinessException::notFound));
    }

    @Transactional(readOnly = true)
    public Market require(String slug) {
        return markets.findBySlug(slug).orElseThrow(BusinessException::notFound);
    }

    private AdminMarketView view(Market m) {
        Map<String, Translation> translations = new java.util.LinkedHashMap<>();
        m.getTranslations()
                .forEach(t ->
                        translations.put(t.getLocale(), new Translation(t.getDescription(), t.getHowToGetThere())));
        List<AdminSectorView> sectorViews = sectors.findAllByMarketIdOrderBySortOrderAsc(m.getId()).stream()
                .map(s -> {
                    Map<String, String> names = new java.util.LinkedHashMap<>();
                    s.getTranslations().forEach(t -> names.put(t.getLocale(), t.getName()));
                    return new AdminSectorView(s.getSlug(), s.getSortOrder(), s.isActive(), names);
                })
                .toList();
        return new AdminMarketView(
                m.getSlug(),
                m.getName(),
                m.getAddress(),
                m.getLatitude(),
                m.getLongitude(),
                m.getGeofenceRadiusMeters(),
                m.getOpeningHours(),
                m.isActive(),
                m.getDataVersion(),
                m.getVersion(),
                translations,
                sectorViews,
                m.getCover() == null ? null : m.getCover().url(true),
                m.getCoverCredit(),
                m.getCoverCreditUrl());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_EVEN);
    }
}
