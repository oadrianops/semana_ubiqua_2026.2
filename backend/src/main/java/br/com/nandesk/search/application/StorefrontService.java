package br.com.nandesk.search.application;

import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.catalog.application.CatalogViews.TermHit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.application.MarketViews.MarketDetail;
import br.com.nandesk.market.application.MarketViews.MarketSummary;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.search.application.SnapshotViews.MarketSnapshot;
import br.com.nandesk.search.application.SnapshotViews.SnapshotCategory;
import br.com.nandesk.search.application.SnapshotViews.SnapshotOffering;
import br.com.nandesk.search.application.SnapshotViews.SnapshotProduct;
import br.com.nandesk.search.application.SnapshotViews.SnapshotStall;
import br.com.nandesk.shared.config.CacheConfig;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallQueryService;
import br.com.nandesk.stall.application.StallViews.StallSummary;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.stall.domain.StallOffering;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vitrine das feiras (RF-01) e snapshot para uso sem conexão (RF-36). */
@Service
@Transactional(readOnly = true)
public class StorefrontService {

    private final MarketQueryService markets;
    private final StallQueryService stalls;
    private final CatalogQueryService catalog;
    private final Clock clock;

    public StorefrontService(
            MarketQueryService markets, StallQueryService stalls, CatalogQueryService catalog, Clock clock) {
        this.markets = markets;
        this.stalls = stalls;
        this.catalog = catalog;
        this.clock = clock;
    }

    public List<MarketSummary> listMarkets(AppLocale locale) {
        return markets.listActive(locale, stalls.countPublicByMarket());
    }

    public MarketDetail marketDetail(String slug, AppLocale locale) {
        Market market = markets.requireActive(slug);
        return detail(market, locale);
    }

    public PageResponse<StallSummary> marketStalls(
            String slug, String sectorSlug, int page, int size, AppLocale locale) {
        Market market = markets.requireActive(slug);
        Sector sector = null;
        if (sectorSlug != null && !sectorSlug.isBlank()) {
            sector = markets.sector(market, sectorSlug).orElseThrow(BusinessException::notFound);
        }
        return stalls.publicStallsOf(market, sector, page, size, locale);
    }

    public Optional<Long> dataVersion(String slug) {
        return markets.dataVersion(slug);
    }

    /** Snapshot por (feira, idioma, versão); a versão vem do ETag, então nunca serve dado velho. */
    @Cacheable(cacheNames = CacheConfig.SNAPSHOT, key = "#slug + '|' + #locale + '|' + #version")
    public MarketSnapshot snapshot(String slug, AppLocale locale, long version) {
        Market market = markets.requireActive(slug);
        List<StallOffering> offerings = stalls.publicOfferingsOfMarket(market);
        Map<Long, List<StallOffering>> byStall = new HashMap<>();
        offerings.forEach(o -> byStall.computeIfAbsent(o.getStall().getId(), k -> new ArrayList<>())
                .add(o));

        Set<Long> productIds =
                offerings.stream().map(o -> o.getProduct().getId()).collect(Collectors.toSet());
        Map<Long, Product> products = catalog.productsById(productIds);
        products.values().removeIf(p -> !p.isPublic());
        Map<Long, Set<String>> terms = new HashMap<>();
        for (TermHit hit : catalog.allActiveTerms()) {
            if (products.containsKey(hit.productId())) {
                terms.computeIfAbsent(hit.productId(), k -> new TreeSet<>()).add(hit.normalizedTerm());
            }
        }

        List<SnapshotStall> stallViews = stalls.allPublicStallsOf(market).stream()
                .map(s -> stall(s, byStall.getOrDefault(s.getId(), List.of()), products, locale))
                .toList();
        List<SnapshotProduct> productViews = products.values().stream()
                .sorted(Comparator.comparing(p -> p.name(locale)))
                .map(p -> new SnapshotProduct(
                        p.getSlug(),
                        p.name(locale),
                        p.getCategory().getSlug(),
                        p.getImage() == null ? null : p.getImage().url(true),
                        List.copyOf(terms.getOrDefault(p.getId(), Set.of()))))
                .toList();
        List<SnapshotCategory> categories = catalog.activeCategories().stream()
                .map(c -> new SnapshotCategory(c.getSlug(), c.name(locale)))
                .toList();
        return new MarketSnapshot(
                detail(market, locale), version, locale.tag(), clock.instant(), categories, productViews, stallViews);
    }

    private MarketDetail detail(Market market, AppLocale locale) {
        Map<Long, Long> bySector = stalls.countPublicBySector(market.getId());
        long total = stalls.countPublicByMarket().getOrDefault(market.getId(), 0L);
        return markets.detail(market, locale, bySector, total);
    }

    private SnapshotStall stall(Stall s, List<StallOffering> offerings, Map<Long, Product> products, AppLocale locale) {
        List<SnapshotOffering> offeringViews = offerings.stream()
                .filter(o -> products.containsKey(o.getProduct().getId()))
                .map(o -> new SnapshotOffering(
                        o.getProduct().getSlug(),
                        MoneyDto.brl(o.getReferencePrice()),
                        o.getPriceUnit(),
                        o.getAvailability(),
                        o.getLastVerifiedAt()))
                .toList();
        return new SnapshotStall(
                s.getPublicId(),
                s.getCode(),
                s.getDisplayName(),
                s.publicVendorName(),
                s.getSector() == null ? null : s.getSector().getSlug(),
                s.getSector() == null ? null : s.getSector().name(locale),
                s.getPhoto() == null ? null : s.getPhoto().url(true),
                s.getLastVerifiedAt(),
                stalls.freshness().isOutdated(s.getLastVerifiedAt(), clock.instant()),
                offeringViews);
    }
}
