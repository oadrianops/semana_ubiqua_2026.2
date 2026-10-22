package br.com.nandesk.stall.application;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.market.application.MarketViews.MarketRef;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallViews.OfferingView;
import br.com.nandesk.stall.application.StallViews.PhotoView;
import br.com.nandesk.stall.application.StallViews.SectorRef;
import br.com.nandesk.stall.application.StallViews.StallDetail;
import br.com.nandesk.stall.application.StallViews.StallSummary;
import br.com.nandesk.stall.domain.FreshnessPolicy;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.stall.domain.StallOffering;
import br.com.nandesk.stall.infra.StallOfferingRepository;
import br.com.nandesk.stall.infra.StallPhotoRepository;
import br.com.nandesk.stall.infra.StallRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura dos boxes públicos: só boxes ativos e com consentimento aparecem (RN-13). */
@Service
@Transactional(readOnly = true)
public class StallQueryService {

    private final StallRepository stalls;
    private final StallOfferingRepository offerings;
    private final StallPhotoRepository photos;
    private final FreshnessPolicy freshness;
    private final Clock clock;

    public StallQueryService(
            StallRepository stalls,
            StallOfferingRepository offerings,
            StallPhotoRepository photos,
            AppProperties properties,
            Clock clock) {
        this.stalls = stalls;
        this.offerings = offerings;
        this.photos = photos;
        this.freshness = new FreshnessPolicy(
                properties.tasks().staleAfter(), properties.stalls().outdatedWarningAfter());
        this.clock = clock;
    }

    public FreshnessPolicy freshness() {
        return freshness;
    }

    /** Box público com ofertas; inativo ou sem consentimento responde "não encontrado" (RF-03, RN-13). */
    public StallDetail publicStall(UUID publicId, AppLocale locale) {
        Stall stall = stalls.findPublicByPublicId(publicId)
                .filter(s -> s.getMarket().isActive())
                .orElseThrow(BusinessException::notFound);
        List<OfferingView> views = offerings.findByStallWithProduct(stall.getId()).stream()
                .filter(o -> o.getProduct().isPublic())
                .sorted(Comparator.comparing(o -> o.getProduct().name(locale)))
                .map(o -> offering(o, locale))
                .toList();
        Market market = stall.getMarket();
        return new StallDetail(
                stall.getPublicId(),
                stall.getCode(),
                stall.getDisplayName(),
                stall.publicVendorName(),
                new MarketRef(market.getSlug(), market.getName()),
                sectorRef(stall.getSector(), locale),
                stall.getPhoto() == null ? null : stall.getPhoto().url(false),
                stall.getPhoto() == null ? null : stall.getPhoto().url(true),
                stall.getLastVerifiedAt(),
                freshness.isOutdated(stall.getLastVerifiedAt(), clock.instant()),
                stall.getVersion(),
                views,
                stall.isDemo(),
                stall.getInstagramHandle(),
                gallery(stall),
                stall.getLatitude(),
                stall.getLongitude());
    }

    /** Galeria pública do box, na ordem definida no painel (RF-03). */
    public List<PhotoView> gallery(Stall stall) {
        return photos.findByStallIdOrderBySortOrderAscIdAsc(stall.getId()).stream()
                .map(p -> photoView(p.getFile()))
                .toList();
    }

    public static PhotoView photoView(StoredFile file) {
        return new PhotoView(file.url(false), file.url(true), file.getWidth(), file.getHeight());
    }

    public PageResponse<StallSummary> publicStallsOf(
            Market market, Sector sector, int page, int size, AppLocale locale) {
        Page<Stall> result = stalls.findPublicByMarket(
                market.getId(),
                sector == null ? null : sector.getId(),
                // Boxes conferidos em campo antes dos de exemplo da demonstração (ADR-0018).
                PageRequest.of(
                        Math.max(0, page),
                        PageResponse.clampSize(size),
                        Sort.by("demo").and(Sort.by("code"))));
        Map<Long, Integer> counts = offeringCounts(result.getContent());
        return PageResponse.of(result, s -> summary(s, locale, counts.getOrDefault(s.getId(), 0)));
    }

    public List<Stall> allPublicStallsOf(Market market) {
        return stalls.findAllPublicByMarket(market.getId());
    }

    public List<StallOffering> publicOfferingsOfMarket(Market market) {
        return offerings.findPublicByMarket(market.getId());
    }

    public Map<Long, Long> countPublicByMarket() {
        return toMap(stalls.countPublicByMarket());
    }

    public Map<Long, Long> countPublicBySector(Long marketId) {
        return toMap(stalls.countPublicBySector(marketId));
    }

    public Map<Long, Long> countPublicStallsByProduct() {
        return toMap(offerings.countPublicStallsByProduct());
    }

    public List<StallOffering> publicOfferingsFor(
            Collection<Long> productIds, String marketSlug, boolean availableOnly) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return offerings.findPublicByProducts(productIds, marketSlug, availableOnly);
    }

    public Optional<Stall> findByPublicId(UUID publicId) {
        return stalls.findByPublicId(publicId);
    }

    public StallSummary summary(Stall s, AppLocale locale, int offeringCount) {
        return new StallSummary(
                s.getPublicId(),
                s.getCode(),
                s.getDisplayName(),
                sectorRef(s.getSector(), locale),
                s.getPhoto() == null ? null : s.getPhoto().url(true),
                s.getLastVerifiedAt(),
                freshness.isOutdated(s.getLastVerifiedAt(), clock.instant()),
                offeringCount,
                s.isDemo());
    }

    public static OfferingView offering(StallOffering o, AppLocale locale) {
        return new OfferingView(
                o.getPublicId(),
                o.getProduct().getSlug(),
                o.getProduct().name(locale),
                o.getProduct().getCategory().getSlug(),
                MoneyDto.brl(o.getReferencePrice()),
                o.getPriceUnit(),
                o.getAvailability(),
                o.getLastVerifiedAt(),
                o.getVersion());
    }

    public static SectorRef sectorRef(Sector sector, AppLocale locale) {
        return sector == null ? null : new SectorRef(sector.getSlug(), sector.name(locale));
    }

    /** Contagem de ofertas da página em uma única consulta (sem N+1). */
    private Map<Long, Integer> offeringCounts(List<Stall> page) {
        Map<Long, Integer> counts = new HashMap<>();
        if (page.isEmpty()) {
            return counts;
        }
        List<Long> ids = page.stream().map(Stall::getId).toList();
        for (Object[] row : offerings.countByStallIds(ids)) {
            counts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
