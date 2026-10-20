package br.com.nandesk.market.application;

import br.com.nandesk.market.application.MarketViews.MarketDetail;
import br.com.nandesk.market.application.MarketViews.MarketSummary;
import br.com.nandesk.market.application.MarketViews.SectorView;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.MarketTranslation;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.market.infra.MarketRepository;
import br.com.nandesk.market.infra.SectorRepository;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura pública das feiras (RF-01). As contagens de boxes vêm do módulo de boxes, como parâmetro. */
@Service
@Transactional(readOnly = true)
public class MarketQueryService {

    private final MarketRepository markets;
    private final SectorRepository sectors;

    public MarketQueryService(MarketRepository markets, SectorRepository sectors) {
        this.markets = markets;
        this.sectors = sectors;
    }

    public List<MarketSummary> listActive(AppLocale locale, Map<Long, Long> publicStallsByMarket) {
        return markets.findAllByActiveTrueOrderByNameAsc().stream()
                .map(m -> summary(m, locale, publicStallsByMarket.getOrDefault(m.getId(), 0L)))
                .toList();
    }

    public List<Market> listActiveEntities() {
        return markets.findAllByActiveTrueOrderByNameAsc();
    }

    public MarketDetail detail(Market market, AppLocale locale, Map<Long, Long> stallsBySector, long publicStalls) {
        MarketTranslation t = market.translation(locale);
        List<SectorView> sectorViews =
                sectors.findAllByMarketIdAndActiveTrueOrderBySortOrderAsc(market.getId()).stream()
                        .map(s ->
                                new SectorView(s.getSlug(), s.name(locale), stallsBySector.getOrDefault(s.getId(), 0L)))
                        .toList();
        return new MarketDetail(
                market.getSlug(),
                market.getName(),
                t == null ? null : t.getDescription(),
                t == null ? null : t.getHowToGetThere(),
                market.getAddress(),
                market.getLatitude(),
                market.getLongitude(),
                market.getGeofenceRadiusMeters(),
                market.getOpeningHours(),
                publicStalls,
                market.getCover() == null ? null : market.getCover().url(false),
                market.getDataVersion(),
                sectorViews);
    }

    /** Feira ativa pelo slug; inexistente ou inativa responde "não encontrado". */
    public Market requireActive(String slug) {
        return markets.findBySlugAndActiveTrue(slug).orElseThrow(BusinessException::notFound);
    }

    public Optional<Market> findActive(String slug) {
        return markets.findBySlugAndActiveTrue(slug);
    }

    public Optional<Long> dataVersion(String slug) {
        return markets.findActiveDataVersion(slug);
    }

    public Optional<Sector> sector(Market market, String sectorSlug) {
        return sectors.findByMarketIdAndSlug(market.getId(), sectorSlug);
    }

    public List<Sector> activeSectors(Market market) {
        return sectors.findAllByMarketIdAndActiveTrueOrderBySortOrderAsc(market.getId());
    }

    /** Incrementa a versão dos dados (ETag do snapshot) na transação que publica a mudança. */
    @Transactional
    public void bumpDataVersion(Long marketId) {
        markets.bumpDataVersion(marketId);
    }

    private static MarketSummary summary(Market m, AppLocale locale, long stalls) {
        MarketTranslation t = m.translation(locale);
        return new MarketSummary(
                m.getSlug(),
                m.getName(),
                t == null ? null : t.getDescription(),
                m.getAddress(),
                m.getLatitude(),
                m.getLongitude(),
                m.getGeofenceRadiusMeters(),
                m.getOpeningHours(),
                stalls,
                m.getCover() == null ? null : m.getCover().url(false),
                m.getCover() == null ? null : m.getCover().url(true));
    }
}
