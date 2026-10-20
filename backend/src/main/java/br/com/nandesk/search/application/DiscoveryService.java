package br.com.nandesk.search.application;

import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.catalog.application.CatalogViews.CategoryProducts;
import br.com.nandesk.catalog.application.CatalogViews.CategoryView;
import br.com.nandesk.catalog.application.CatalogViews.ProductSummary;
import br.com.nandesk.catalog.domain.Category;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.market.application.MarketViews.MarketRef;
import br.com.nandesk.search.application.SearchViews.MarketHits;
import br.com.nandesk.search.application.SearchViews.ProductPage;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.stall.application.StallQueryService;
import br.com.nandesk.stall.domain.StallOffering;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Navegação por categorias (RF-05) e página do produto com onde encontrar (RF-04). */
@Service
@Transactional(readOnly = true)
public class DiscoveryService {

    private final CatalogQueryService catalog;
    private final StallQueryService stalls;

    public DiscoveryService(CatalogQueryService catalog, StallQueryService stalls) {
        this.catalog = catalog;
        this.stalls = stalls;
    }

    public List<CategoryView> categories(AppLocale locale) {
        Map<Long, Long> stallsByProduct = stalls.countPublicStallsByProduct();
        Map<Long, Long> productsByCategory = new LinkedHashMap<>();
        for (Product p : catalog.allActiveProducts()) {
            if (stallsByProduct.getOrDefault(p.getId(), 0L) > 0) {
                productsByCategory.merge(p.getCategory().getId(), 1L, Long::sum);
            }
        }
        return catalog.activeCategories().stream()
                .map(c -> view(c, locale, productsByCategory.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    public CategoryProducts categoryProducts(String slug, AppLocale locale) {
        Category category = catalog.requireCategory(slug);
        Map<Long, Long> stallsByProduct = stalls.countPublicStallsByProduct();
        List<ProductSummary> products = catalog.activeProductsOf(category).stream()
                .map(p -> new ProductSummary(
                        p.getSlug(),
                        p.name(locale),
                        p.getImage() == null ? null : p.getImage().url(true),
                        stallsByProduct.getOrDefault(p.getId(), 0L)))
                .sorted(Comparator.comparing(ProductSummary::stallCount)
                        .reversed()
                        .thenComparing(ProductSummary::name))
                .toList();
        long withStalls = products.stream().filter(p -> p.stallCount() > 0).count();
        return new CategoryProducts(view(category, locale, withStalls), products);
    }

    public ProductPage productPage(String slug, AppLocale locale) {
        Product product = catalog.requirePublicProduct(slug);
        List<StallOffering> offerings = stalls.publicOfferingsFor(List.of(product.getId()), null, false);
        Map<String, List<StallOffering>> byMarket = new LinkedHashMap<>();
        offerings.stream()
                .sorted(Comparator.comparing(
                                (StallOffering o) -> o.getStall().getMarket().getName())
                        .thenComparing(StallOffering::getLastVerifiedAt, Comparator.reverseOrder()))
                .forEach(o -> byMarket.computeIfAbsent(o.getStall().getMarket().getSlug(), k -> new ArrayList<>())
                        .add(o));
        List<MarketHits> markets = byMarket.values().stream()
                .map(list -> new MarketHits(
                        new MarketRef(
                                list.getFirst().getStall().getMarket().getSlug(),
                                list.getFirst().getStall().getMarket().getName()),
                        list.stream().map(o -> SearchService.hit(o, locale)).toList()))
                .toList();
        return new ProductPage(
                CatalogQueryService.ref(product, locale),
                product.description(locale),
                product.getSeasonMonths(),
                product.getDefaultUnit(),
                markets);
    }

    private static CategoryView view(Category c, AppLocale locale, long count) {
        return new CategoryView(c.getSlug(), c.name(locale), c.getIcon(), count);
    }
}
