package br.com.nandesk.catalog.application;

import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.ProductAlias;
import br.com.nandesk.catalog.domain.ProductSearchTerm;
import br.com.nandesk.catalog.domain.ProductSearchTerm.Source;
import br.com.nandesk.catalog.domain.ProductTranslation;
import br.com.nandesk.catalog.domain.SearchNormalizer;
import br.com.nandesk.catalog.infra.ProductSearchTermRepository;
import br.com.nandesk.shared.config.CacheConfig;
import java.util.HashSet;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantém {@code product_search_terms} em dia: uma linha por nome traduzido e por sinônimo, em cada idioma, com o
 * termo normalizado (9.10). Chamado sempre que o catálogo muda; invalida o cache da busca.
 */
@Service
public class SearchTermIndexer {

    private final ProductSearchTermRepository terms;

    public SearchTermIndexer(ProductSearchTermRepository terms) {
        this.terms = terms;
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SUGGESTIONS, allEntries = true)
            })
    public void reindex(Product product) {
        terms.deleteByProductId(product.getId());
        Set<String> seen = new HashSet<>();
        for (ProductTranslation t : product.getTranslations()) {
            add(product, t.getLocale(), t.getName(), Source.NAME, seen);
        }
        for (ProductAlias a : product.getAliases()) {
            add(product, a.getLocale(), a.getAlias(), Source.ALIAS, seen);
        }
    }

    private void add(Product product, String locale, String term, Source source, Set<String> seen) {
        String normalized = SearchNormalizer.normalize(term);
        if (normalized.isEmpty() || !seen.add(locale + "|" + normalized)) {
            return;
        }
        String trimmed = term.length() > 120 ? term.substring(0, 120) : term;
        String normalizedTrimmed = normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
        terms.save(new ProductSearchTerm(product, locale, trimmed, normalizedTrimmed, source));
    }
}
