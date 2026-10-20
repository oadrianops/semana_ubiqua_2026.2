package br.com.nandesk.catalog.application;

import java.util.List;

/** Respostas de leitura do catálogo (RF-04, RF-05). */
public final class CatalogViews {

    private CatalogViews() {}

    public record CategoryRef(String slug, String name) {}

    public record CategoryView(String slug, String name, String icon, long productCount) {}

    public record ProductRef(String slug, String name, CategoryRef category, String imageUrl) {}

    public record ProductSummary(String slug, String name, String imageUrl, long stallCount) {}

    public record CategoryProducts(CategoryView category, List<ProductSummary> products) {}

    /** Termo normalizado candidato da busca. */
    public record TermHit(Long productId, String locale, String normalizedTerm) {}
}
