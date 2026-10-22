package br.com.nandesk.admin.api;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.market.application.MarketAdminService;
import br.com.nandesk.market.application.MarketAdminService.AdminMarketView;
import br.com.nandesk.market.application.MarketAdminService.MarketCommand;
import br.com.nandesk.market.application.MarketAdminService.SectorCommand;
import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.application.StallAdminService.AdminStallView;
import br.com.nandesk.stall.domain.ConsentMethod;
import br.com.nandesk.stall.domain.StallStatus;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Feiras, setores, boxes e consentimentos no painel (RF-29, RN-13). */
@RestController
@Tag(name = "Administração")
public class AdminMarketController {

    private final MarketAdminService markets;
    private final MarketQueryService marketQuery;
    private final StallAdminService stalls;
    private final UserAccountService users;
    private final AuditService audit;
    private final AppProperties properties;

    public AdminMarketController(
            MarketAdminService markets,
            MarketQueryService marketQuery,
            StallAdminService stalls,
            UserAccountService users,
            AuditService audit,
            AppProperties properties) {
        this.markets = markets;
        this.marketQuery = marketQuery;
        this.stalls = stalls;
        this.users = users;
        this.audit = audit;
        this.properties = properties;
    }

    public record StallUpdateRequest(
            @Size(max = 120) String displayName,
            @Size(max = 120) String vendorName,
            boolean showVendorName,
            String sectorSlug,
            @NotNull StallStatus status,
            @Size(max = 80) String instagram,
            Boolean demo,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            int version) {}

    public record ConsentRequest(@NotNull ConsentMethod method) {}

    @Operation(summary = "Feiras (RF-29)")
    @GetMapping("/api/v1/admin/markets")
    ResponseEntity<List<AdminMarketView>> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(markets.listAll());
    }

    @Operation(summary = "Feira (RF-29)")
    @GetMapping("/api/v1/admin/markets/{slug}")
    ResponseEntity<AdminMarketView> get(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(markets.get(slug));
    }

    @Operation(summary = "Criar ou atualizar feira: posição no mapa, raio, horários e traduções (RF-29)")
    @PutMapping("/api/v1/admin/markets/{slug}")
    ResponseEntity<AdminMarketView> save(@PathVariable String slug, @Valid @RequestBody MarketCommand body) {
        markets.upsert(new MarketCommand(
                slug,
                body.name(),
                body.address(),
                body.latitude(),
                body.longitude(),
                body.radiusMeters(),
                body.openingHours(),
                body.active(),
                body.translations(),
                body.expectedVersion()));
        audit.record(actor().getId(), "MARKET_SAVED", "market", slug);
        return ResponseEntity.ok(markets.get(slug));
    }

    @Operation(summary = "Criar ou atualizar setor (RF-29)")
    @PutMapping("/api/v1/admin/markets/{slug}/sectors/{sectorSlug}")
    ResponseEntity<AdminMarketView> saveSector(
            @PathVariable String slug, @PathVariable String sectorSlug, @Valid @RequestBody SectorCommand body) {
        markets.upsertSector(slug, new SectorCommand(sectorSlug, body.sortOrder(), body.active(), body.names()));
        audit.record(actor().getId(), "SECTOR_SAVED", "sector", slug + "/" + sectorSlug);
        return ResponseEntity.ok(markets.get(slug));
    }

    @Operation(summary = "Boxes (RF-29)")
    @GetMapping("/api/v1/admin/stalls")
    ResponseEntity<PageResponse<AdminStallView>> stalls(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Market m = market == null ? null : markets.require(market);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stalls.search(m, q, page, size));
    }

    @Operation(summary = "Editar box; desativar oculta e cancela as tarefas ativas (RF-29, RN-13)")
    @PatchMapping("/api/v1/admin/stalls/{publicId}")
    ResponseEntity<AdminStallView> update(@PathVariable UUID publicId, @Valid @RequestBody StallUpdateRequest body) {
        var stall = stalls.require(publicId);
        Sector sector = body.sectorSlug() == null
                ? null
                : marketQuery.sector(stall.getMarket(), body.sectorSlug()).orElse(null);
        AdminStallView view = stalls.update(
                publicId,
                body.displayName(),
                body.vendorName(),
                body.showVendorName(),
                sector,
                body.status(),
                body.instagram(),
                body.demo(),
                body.latitude(),
                body.longitude(),
                body.version());
        audit.record(
                actor().getId(),
                "STALL_UPDATED",
                "stall",
                publicId,
                null,
                Map.of("status", body.status().name()));
        return ResponseEntity.ok(view);
    }

    @Operation(summary = "Revogar consentimento do feirante: oculta na hora (RN-13)")
    @PostMapping("/api/v1/admin/stalls/{publicId}/consent/revoke")
    ResponseEntity<AdminStallView> revoke(@PathVariable UUID publicId) {
        AdminStallView view =
                stalls.revokeConsent(publicId, properties.retention().revokedStallPhotos());
        audit.record(actor().getId(), "CONSENT_REVOKED", "stall", publicId);
        return ResponseEntity.ok(view);
    }

    @Operation(summary = "Registrar consentimento do feirante (RN-13)")
    @PostMapping("/api/v1/admin/stalls/{publicId}/consent/grant")
    ResponseEntity<AdminStallView> grant(@PathVariable UUID publicId, @Valid @RequestBody ConsentRequest body) {
        User actor = actor();
        AdminStallView view = stalls.grantConsent(publicId, body.method(), actor);
        audit.record(
                actor.getId(),
                "CONSENT_GRANTED",
                "stall",
                publicId,
                null,
                Map.of("method", body.method().name()));
        return ResponseEntity.ok(view);
    }

    private User actor() {
        return users.require(CurrentUser.require().publicId());
    }
}
