package br.com.nandesk.catalog.application;

import br.com.nandesk.catalog.domain.Category;
import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.ProductStatus;
import br.com.nandesk.catalog.infra.CategoryRepository;
import br.com.nandesk.catalog.infra.ProductRepository;
import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.shared.config.CacheConfig;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.user.domain.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão do catálogo: categorias, produtos, traduções, sinônimos e mescla (RF-30, RF-24, RN-18). */
@Service
public class CatalogAdminService {

    private static final Set<String> LOCALES = Set.of("pt-BR", "en", "es");

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final SearchTermIndexer indexer;

    public CatalogAdminService(CategoryRepository categories, ProductRepository products, SearchTermIndexer indexer) {
        this.categories = categories;
        this.products = products;
        this.indexer = indexer;
    }

    public record CategoryCommand(String slug, String icon, int sortOrder, boolean active, Map<String, String> names) {}

    public record ProductTranslationCommand(String name, String description) {}

    public record ProductCommand(
            String slug,
            String categorySlug,
            PriceUnit defaultUnit,
            List<Integer> seasonMonths,
            ProductStatus status,
            Map<String, ProductTranslationCommand> translations,
            Map<String, List<String>> aliases,
            Integer expectedVersion) {}

    public record AdminProductView(
            String slug,
            String categorySlug,
            PriceUnit defaultUnit,
            List<Integer> seasonMonths,
            ProductStatus status,
            int version,
            Map<String, ProductTranslationCommand> translations,
            Map<String, List<String>> aliases,
            Map<String, Boolean> completeness,
            String suggestedBy,
            String imageUrl) {}

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
    public Category upsertCategory(CategoryCommand cmd) {
        Category category = categories
                .findBySlug(cmd.slug())
                .orElseGet(() -> new Category(cmd.slug(), cmd.icon(), cmd.sortOrder()));
        category.setIcon(cmd.icon());
        category.setSortOrder(cmd.sortOrder());
        category.setActive(cmd.active());
        cmd.names().forEach(category::putName);
        return categories.save(category);
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public Product upsertProduct(ProductCommand cmd) {
        ProductTranslationCommand pt = cmd.translations().get(AppLocale.PT_BR.tag());
        if (pt == null || pt.name() == null || pt.name().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Category category = categories.findBySlug(cmd.categorySlug()).orElseThrow(BusinessException::notFound);
        Product product = products.findBySlug(cmd.slug())
                .orElseGet(() -> new Product(cmd.slug(), category, cmd.defaultUnit(), cmd.status()));
        if (cmd.expectedVersion() != null && product.getId() != null && cmd.expectedVersion() != product.getVersion()) {
            throw new BusinessException(ErrorCode.STALE_VERSION);
        }
        product.setCategory(category);
        product.setDefaultUnit(cmd.defaultUnit());
        product.setSeasonMonths(cmd.seasonMonths() == null || cmd.seasonMonths().isEmpty() ? null : cmd.seasonMonths());
        product.setStatus(cmd.status());
        cmd.translations().forEach((locale, t) -> {
            if (LOCALES.contains(locale) && t.name() != null && !t.name().isBlank()) {
                product.putTranslation(locale, t.name(), t.description());
            }
        });
        if (cmd.aliases() != null) {
            // Diferença em vez de limpar tudo: o Hibernate insere antes de apagar e violaria o índice único.
            Set<String> desired = new java.util.HashSet<>();
            cmd.aliases().forEach((locale, list) -> list.forEach(a -> desired.add(locale + "|" + a.toLowerCase())));
            product.getAliases()
                    .removeIf(a ->
                            !desired.contains(a.getLocale() + "|" + a.getAlias().toLowerCase()));
            cmd.aliases().forEach((locale, list) -> list.forEach(a -> product.addAlias(locale, a)));
        }
        Product saved = products.saveAndFlush(product);
        indexer.reindex(saved);
        return saved;
    }

    /** Sugestão de produto do colaborador: entra PENDING e só fica visível após aprovação (RN-18). */
    @Transactional
    public Product suggest(String namePt, String categorySlug, User suggestedBy, StoredFile photo) {
        Category category = categories.findBySlug(categorySlug).orElseThrow(BusinessException::notFound);
        String base =
                br.com.nandesk.catalog.domain.SearchNormalizer.normalize(namePt).replace(' ', '-');
        String slug = base.isEmpty() ? "sugestao" : base;
        int n = 1;
        while (products.findBySlug(slug).isPresent()) {
            slug = base + "-" + (++n);
        }
        Product product = new Product(slug, category, PriceUnit.UNIT, ProductStatus.PENDING);
        product.putTranslation(AppLocale.PT_BR.tag(), namePt, null);
        product.setSuggestedBy(suggestedBy);
        product.setImage(photo);
        Product saved = products.saveAndFlush(product);
        indexer.reindex(saved);
        return saved;
    }

    /** Aprova uma sugestão pendente (RF-24). */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
    public Product approveSuggestion(String slug) {
        Product product = products.findBySlug(slug).orElseThrow(BusinessException::notFound);
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        product.setStatus(ProductStatus.ACTIVE);
        return product;
    }

    /**
     * Mescla {@code sourceSlug} em {@code targetSlug}: os nomes do produto de origem viram sinônimos do destino e
     * ele fica {@code MERGED} (RF-30, RF-24). As ofertas migram no módulo de boxes.
     */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true)
            })
    public Product merge(String sourceSlug, String targetSlug) {
        Product source = products.findBySlug(sourceSlug).orElseThrow(BusinessException::notFound);
        Product target = products.findBySlug(targetSlug).orElseThrow(BusinessException::notFound);
        if (source.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        source.getTranslations().forEach(t -> target.addAlias(t.getLocale(), t.getName()));
        source.getAliases().forEach(a -> target.addAlias(a.getLocale(), a.getAlias()));
        source.setStatus(ProductStatus.MERGED);
        source.setMergedInto(target);
        products.saveAndFlush(target);
        indexer.reindex(target);
        indexer.reindex(source);
        return target;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductView> list(ProductStatus status, String q, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                products.searchByStatus(status, like, PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                this::view);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductView> pendingSuggestions(int page, int size) {
        return PageResponse.of(
                products.findAllByStatusOrderByCreatedAtAsc(
                        ProductStatus.PENDING, PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                this::view);
    }

    @Transactional(readOnly = true)
    public AdminProductView get(String slug) {
        return view(products.findBySlug(slug).orElseThrow(BusinessException::notFound));
    }

    @Transactional(readOnly = true)
    public Product require(String slug) {
        return products.findBySlug(slug).orElseThrow(BusinessException::notFound);
    }

    @Transactional(readOnly = true)
    public List<Category> allCategories() {
        return categories.findAllByOrderBySortOrderAsc();
    }

    private AdminProductView view(Product p) {
        Map<String, ProductTranslationCommand> translations = new LinkedHashMap<>();
        p.getTranslations()
                .forEach(t -> translations.put(
                        t.getLocale(), new ProductTranslationCommand(t.getName(), t.getDescription())));
        Map<String, List<String>> aliases = new LinkedHashMap<>();
        p.getAliases()
                .forEach(a -> aliases.computeIfAbsent(a.getLocale(), k -> new java.util.ArrayList<>())
                        .add(a.getAlias()));
        Map<String, Boolean> completeness = new LinkedHashMap<>();
        for (String locale : List.of("pt-BR", "en", "es")) {
            ProductTranslationCommand t = translations.get(locale);
            completeness.put(
                    locale,
                    t != null
                            && t.name() != null
                            && t.description() != null
                            && !t.description().isBlank());
        }
        return new AdminProductView(
                p.getSlug(),
                p.getCategory().getSlug(),
                p.getDefaultUnit(),
                p.getSeasonMonths(),
                p.getStatus(),
                p.getVersion(),
                translations,
                aliases,
                completeness,
                p.getSuggestedBy() == null ? null : p.getSuggestedBy().getName(),
                p.getImage() == null ? null : p.getImage().url(true));
    }
}
