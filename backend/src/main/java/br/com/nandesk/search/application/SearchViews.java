package br.com.nandesk.search.application;

import br.com.nandesk.catalog.application.CatalogViews.ProductRef;
import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.market.application.MarketViews.MarketRef;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.stall.domain.Availability;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resultado da busca agrupado por produto → feira → boxes (RF-02, exemplo 11.3). */
public final class SearchViews {

    private SearchViews() {}

    public record StallHit(
            UUID publicId,
            String code,
            String name,
            String sector,
            MoneyDto price,
            PriceUnit priceUnit,
            Availability availability,
            Instant lastVerifiedAt,
            boolean demo) {}

    public record MarketHits(MarketRef market, List<StallHit> stalls) {}

    public record SearchItem(ProductRef product, List<MarketHits> markets) {}

    public record SearchResult(
            String query,
            String locale,
            List<SearchItem> items,
            int page,
            int size,
            long totalItems,
            int totalPages,
            List<String> didYouMean) {}

    public record Suggestion(String slug, String name, String categorySlug) {}

    /** Página do produto: o que é + onde encontrar (RF-04). */
    public record ProductPage(
            ProductRef product,
            String description,
            List<Integer> seasonMonths,
            PriceUnit defaultUnit,
            List<MarketHits> markets) {}
}
