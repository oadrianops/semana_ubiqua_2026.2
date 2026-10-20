package br.com.nandesk.admin.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.catalog.application.CatalogAdminService;
import br.com.nandesk.catalog.application.CatalogAdminService.AdminProductView;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.stall.application.StallAdminService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mescla de produtos (RF-30) e de sugestões (RF-24) numa transação: sinônimos vão para o produto mantido e as
 * ofertas migram (o módulo de boxes depende do de catálogo, por isso a orquestração fica aqui).
 */
@Service
public class CatalogMergeService {

    private final CatalogAdminService catalog;
    private final StallAdminService stalls;
    private final AuditService audit;

    public CatalogMergeService(CatalogAdminService catalog, StallAdminService stalls, AuditService audit) {
        this.catalog = catalog;
        this.stalls = stalls;
        this.audit = audit;
    }

    @Transactional
    public AdminProductView merge(String sourceSlug, String targetSlug, String action, Long actorId) {
        Product source = catalog.require(sourceSlug);
        Product target = catalog.merge(sourceSlug, targetSlug);
        stalls.migrateOfferings(source, target);
        audit.record(
                actorId,
                action,
                "product",
                target.getPublicId(),
                Map.of("source", sourceSlug),
                Map.of("target", targetSlug));
        return catalog.get(targetSlug);
    }
}
