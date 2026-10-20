package br.com.nandesk.admin.api;

import br.com.nandesk.admin.application.CatalogMergeService;
import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.catalog.application.CatalogAdminService;
import br.com.nandesk.catalog.application.CatalogAdminService.AdminProductView;
import br.com.nandesk.catalog.application.CatalogAdminService.CategoryCommand;
import br.com.nandesk.catalog.application.CatalogAdminService.ProductCommand;
import br.com.nandesk.catalog.domain.Category;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.ProductStatus;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo no painel (RF-30) e sugestões de produto na moderação (RF-24, RN-18). */
@RestController
@Tag(name = "Administração")
public class AdminCatalogController {

    private final CatalogAdminService catalog;
    private final CatalogMergeService merger;
    private final UserAccountService users;
    private final AuditService audit;

    public AdminCatalogController(
            CatalogAdminService catalog, CatalogMergeService merger, UserAccountService users, AuditService audit) {
        this.catalog = catalog;
        this.merger = merger;
        this.users = users;
        this.audit = audit;
    }

    public record MergeRequest(@NotBlank String targetProductSlug) {}

    public record CategoryView(String slug, String icon, int sortOrder, boolean active, Map<String, String> names) {}

    @Operation(summary = "Categorias (RF-30)")
    @GetMapping("/api/v1/admin/categories")
    ResponseEntity<List<CategoryView>> categories() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(catalog.allCategories().stream()
                        .map(AdminCatalogController::view)
                        .toList());
    }

    @Operation(summary = "Criar ou atualizar categoria (RF-30)")
    @PutMapping("/api/v1/admin/categories/{slug}")
    ResponseEntity<CategoryView> saveCategory(@PathVariable String slug, @Valid @RequestBody CategoryCommand body) {
        Category saved = catalog.upsertCategory(
                new CategoryCommand(slug, body.icon(), body.sortOrder(), body.active(), body.names()));
        audit.record(actor(), "CATEGORY_SAVED", "category", slug);
        return ResponseEntity.ok(view(saved));
    }

    @Operation(summary = "Produtos com traduções e completude (RF-30)")
    @GetMapping("/api/v1/admin/products")
    ResponseEntity<PageResponse<AdminProductView>> products(
            @RequestParam(defaultValue = "ACTIVE") ProductStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(catalog.list(status, q, page, size));
    }

    @Operation(summary = "Produto (RF-30)")
    @GetMapping("/api/v1/admin/products/{slug}")
    ResponseEntity<AdminProductView> product(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(catalog.get(slug));
    }

    @Operation(summary = "Criar ou atualizar produto, traduções e sinônimos (RF-30)")
    @PutMapping("/api/v1/admin/products/{slug}")
    ResponseEntity<AdminProductView> saveProduct(@PathVariable String slug, @Valid @RequestBody ProductCommand body) {
        Product saved = catalog.upsertProduct(new ProductCommand(
                slug,
                body.categorySlug(),
                body.defaultUnit(),
                body.seasonMonths(),
                body.status(),
                body.translations(),
                body.aliases(),
                body.expectedVersion()));
        audit.record(actor(), "PRODUCT_SAVED", "product", saved.getPublicId());
        return ResponseEntity.ok(catalog.get(saved.getSlug()));
    }

    @Operation(summary = "Mesclar produtos duplicados (RF-30)")
    @PostMapping("/api/v1/admin/products/{slug}/merge")
    ResponseEntity<AdminProductView> merge(@PathVariable String slug, @Valid @RequestBody MergeRequest body) {
        return ResponseEntity.ok(merger.merge(slug, body.targetProductSlug(), "PRODUCT_MERGED", actor()));
    }

    @Operation(summary = "Sugestões de produto pendentes (RF-24)")
    @GetMapping("/api/v1/moderation/product-suggestions")
    ResponseEntity<PageResponse<AdminProductView>> suggestions(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(catalog.pendingSuggestions(page, size));
    }

    @Operation(summary = "Aprovar sugestão (RF-24, RN-18)")
    @PostMapping("/api/v1/moderation/product-suggestions/{slug}/approve")
    ResponseEntity<AdminProductView> approve(@PathVariable String slug) {
        Product product = catalog.approveSuggestion(slug);
        audit.record(actor(), "PRODUCT_SUGGESTION_APPROVED", "product", product.getPublicId());
        return ResponseEntity.ok(catalog.get(slug));
    }

    @Operation(summary = "Mesclar sugestão com produto existente — o nome sugerido vira sinônimo (RF-24)")
    @PostMapping("/api/v1/moderation/product-suggestions/{slug}/merge")
    ResponseEntity<AdminProductView> mergeSuggestion(@PathVariable String slug, @Valid @RequestBody MergeRequest body) {
        return ResponseEntity.ok(merger.merge(slug, body.targetProductSlug(), "PRODUCT_SUGGESTION_MERGED", actor()));
    }

    private Long actor() {
        return users.require(CurrentUser.require().publicId()).getId();
    }

    private static CategoryView view(Category c) {
        Map<String, String> names = new LinkedHashMap<>();
        c.getTranslations().forEach(t -> names.put(t.getLocale(), t.getName()));
        return new CategoryView(c.getSlug(), c.getIcon(), c.getSortOrder(), c.isActive(), names);
    }
}
