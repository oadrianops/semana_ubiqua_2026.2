package br.com.nandesk.catalog.application;

import br.com.nandesk.catalog.application.CatalogViews.CategoryRef;
import br.com.nandesk.catalog.application.CatalogViews.ProductRef;
import br.com.nandesk.catalog.application.CatalogViews.TermHit;
import br.com.nandesk.catalog.domain.Category;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.ProductStatus;
import br.com.nandesk.catalog.infra.CategoryRepository;
import br.com.nandesk.catalog.infra.ProductRepository;
import br.com.nandesk.catalog.infra.ProductSearchTermRepository;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura do catálogo: categorias, produtos e termos candidatos da busca. */
@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

    /** Limite de candidatos da busca (9.10). */
    public static final int MAX_CANDIDATES = 200;

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductSearchTermRepository terms;

    public CatalogQueryService(
            CategoryRepository categories, ProductRepository products, ProductSearchTermRepository terms) {
        this.categories = categories;
        this.products = products;
        this.terms = terms;
    }

    public List<Category> activeCategories() {
        return categories.findAllByActiveTrueOrderBySortOrderAsc();
    }

    public Category requireCategory(String slug) {
        return categories.findBySlug(slug).filter(Category::isActive).orElseThrow(BusinessException::notFound);
    }

    public List<Product> activeProductsOf(Category category) {
        return products.findAllByCategoryIdAndStatus(category.getId(), ProductStatus.ACTIVE);
    }

    public List<Product> allActiveProducts() {
        return products.findAllByStatus(ProductStatus.ACTIVE);
    }

    public Product requirePublicProduct(String slug) {
        return products.findBySlug(slug).filter(Product::isPublic).orElseThrow(BusinessException::notFound);
    }

    public Optional<Product> findBySlug(String slug) {
        return products.findBySlug(slug);
    }

    public Map<Long, Product> productsById(Collection<Long> ids) {
        return products.findAllByIdIn(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    /** Termos candidatos para a consulta normalizada (prefixo do termo ou palavra mais longa contida). */
    public List<TermHit> termCandidates(String normalizedQuery, String longestToken) {
        return terms
                .findCandidates(normalizedQuery + "%", "%" + longestToken + "%", PageRequest.of(0, MAX_CANDIDATES * 2))
                .stream()
                .map(t -> new TermHit(t.getProduct().getId(), t.getLocale(), t.getNormalizedTerm()))
                .toList();
    }

    /** Todos os termos normalizados ativos ("Você quis dizer…?" e snapshot). */
    public List<TermHit> allActiveTerms() {
        return terms.findAllActive().stream()
                .map(t -> new TermHit(t.getProduct().getId(), t.getLocale(), t.getNormalizedTerm()))
                .toList();
    }

    public static ProductRef ref(Product p, AppLocale locale) {
        Category c = p.getCategory();
        return new ProductRef(
                p.getSlug(),
                p.name(locale),
                new CategoryRef(c.getSlug(), c.name(locale)),
                p.getImage() == null ? null : p.getImage().url(true));
    }
}
