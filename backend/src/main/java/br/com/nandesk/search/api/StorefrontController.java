package br.com.nandesk.search.api;

import br.com.nandesk.catalog.application.CatalogViews.CategoryProducts;
import br.com.nandesk.catalog.application.CatalogViews.CategoryView;
import br.com.nandesk.market.application.MarketViews.MarketDetail;
import br.com.nandesk.market.application.MarketViews.MarketSummary;
import br.com.nandesk.search.application.DiscoveryService;
import br.com.nandesk.search.application.SearchViews.ProductPage;
import br.com.nandesk.search.application.SnapshotViews.MarketSnapshot;
import br.com.nandesk.search.application.StorefrontService;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.HttpCaching;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallViews.StallSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/** Vitrine pública: feiras, snapshot, categorias e produtos (RF-01, RF-04, RF-05, RF-36). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Vitrine pública")
public class StorefrontController {

    private final StorefrontService storefront;
    private final DiscoveryService discovery;
    private final AppProperties properties;

    public StorefrontController(StorefrontService storefront, DiscoveryService discovery, AppProperties properties) {
        this.storefront = storefront;
        this.discovery = discovery;
        this.properties = properties;
    }

    @Operation(summary = "Feiras ativas (RF-01)")
    @GetMapping("/markets")
    ResponseEntity<List<MarketSummary>> markets() {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(cache(), locale.tag(), storefront.listMarkets(locale));
    }

    @Operation(summary = "Detalhe da feira com setores (RF-01)")
    @GetMapping("/markets/{slug}")
    ResponseEntity<MarketDetail> market(@PathVariable String slug) {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(cache(), locale.tag(), storefront.marketDetail(slug, locale));
    }

    @Operation(summary = "Boxes publicados da feira (RF-01)")
    @GetMapping("/markets/{slug}/stalls")
    ResponseEntity<PageResponse<StallSummary>> stalls(
            @PathVariable String slug,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(
                cache(), locale.tag(), storefront.marketStalls(slug, sector, page, size, locale));
    }

    /**
     * Snapshot para uso sem conexão (RF-36). O ETag vem de {@code dataVersion} + idioma: o {@code If-None-Match}
     * é respondido com 304 sem montar o snapshot.
     */
    @Operation(summary = "Snapshot da feira para uso sem conexão (RF-36)")
    @GetMapping("/markets/{slug}/snapshot")
    ResponseEntity<MarketSnapshot> snapshot(@PathVariable String slug, WebRequest request) {
        AppLocale locale = AppLocale.current();
        long version = storefront.dataVersion(slug).orElseThrow(BusinessException::notFound);
        return HttpCaching.versioned(
                request,
                "v" + version + "-" + locale.tag(),
                properties.snapshot().cacheControl(),
                locale.tag(),
                () -> storefront.snapshot(slug, locale, version));
    }

    @Operation(summary = "Categorias (RF-05)")
    @GetMapping("/categories")
    ResponseEntity<List<CategoryView>> categories() {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(cache(), locale.tag(), discovery.categories(locale));
    }

    @Operation(summary = "Produtos da categoria (RF-05)")
    @GetMapping("/categories/{slug}/products")
    ResponseEntity<CategoryProducts> categoryProducts(@PathVariable String slug) {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(cache(), locale.tag(), discovery.categoryProducts(slug, locale));
    }

    @Operation(summary = "Produto e onde encontrar (RF-04)")
    @GetMapping("/products/{slug}")
    ResponseEntity<ProductPage> product(@PathVariable String slug) {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(cache(), locale.tag(), discovery.productPage(slug, locale));
    }

    private String cache() {
        return properties.http().publicCacheControl();
    }
}
