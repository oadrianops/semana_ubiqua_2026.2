package br.com.nandesk.catalog.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Sinônimo de um produto em um idioma ("castanha-do-brasil", "brazil nuts"). */
@Entity
@Table(name = "product_aliases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductAlias extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "alias", nullable = false, length = 120)
    private String alias;

    ProductAlias(Product product, String locale, String alias) {
        this.product = product;
        this.locale = locale;
        this.alias = alias;
    }
}
