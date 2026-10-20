package br.com.nandesk.catalog.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Termo normalizado de busca, mantido pela aplicação sempre que o catálogo muda (9.10, ADR-0007). */
@Entity
@Table(name = "product_search_terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSearchTerm extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "term", nullable = false, length = 120)
    private String term;

    @Column(name = "normalized_term", nullable = false, length = 120)
    private String normalizedTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private Source source;

    public ProductSearchTerm(Product product, String locale, String term, String normalizedTerm, Source source) {
        this.product = product;
        this.locale = locale;
        this.term = term;
        this.normalizedTerm = normalizedTerm;
        this.source = source;
    }

    public enum Source {
        NAME,
        ALIAS
    }
}
