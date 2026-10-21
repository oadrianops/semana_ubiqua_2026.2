package br.com.nandesk.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductAliasTest {

    @Test
    @DisplayName("RF-02: sinônimos que só diferem em acento ou maiúscula viram um só (como a collation do banco)")
    void deduplicatesLikeTheDatabaseCollation() {
        Product product = new Product("acai-polpa", null, PriceUnit.LITER, ProductStatus.ACTIVE);

        product.addAlias("pt-BR", "acai");
        product.addAlias("pt-BR", "Açaí");
        product.addAlias("es", "açaí");

        assertThat(product.getAliases())
                .extracting(ProductAlias::getLocale, ProductAlias::getAlias)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("pt-BR", "acai"),
                        org.assertj.core.groups.Tuple.tuple("es", "açaí"));
    }
}
