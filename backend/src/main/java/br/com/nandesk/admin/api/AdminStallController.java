package br.com.nandesk.admin.api;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.file.application.FileService;
import br.com.nandesk.market.application.MarketAdminService;
import br.com.nandesk.market.application.MarketAdminService.AdminMarketView;
import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.application.StallAdminService.AdminStallDetail;
import br.com.nandesk.stall.application.StallAdminService.OfferingCommand;
import br.com.nandesk.stall.domain.Availability;
import br.com.nandesk.stall.domain.ConsentMethod;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Cadastro de boxes conferidos em campo pelo painel (RF-29): o termo do feirante é assinado na feira e a equipe
 * lança o box, as ofertas e as fotos aqui. Fotos do painel já nascem públicas; tirar uma foto agenda a remoção do
 * arquivo (RN-13).
 */
@RestController
@Tag(name = "Administração")
public class AdminStallController {

    private final StallAdminService stalls;
    private final MarketAdminService markets;
    private final MarketQueryService marketQuery;
    private final CatalogQueryService catalog;
    private final FileService files;
    private final UserAccountService users;
    private final AuditService audit;
    private final Clock clock;

    public AdminStallController(
            StallAdminService stalls,
            MarketAdminService markets,
            MarketQueryService marketQuery,
            CatalogQueryService catalog,
            FileService files,
            UserAccountService users,
            AuditService audit,
            Clock clock) {
        this.stalls = stalls;
        this.markets = markets;
        this.marketQuery = marketQuery;
        this.catalog = catalog;
        this.files = files;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    public record StallCreateRequest(
            @NotBlank String marketSlug,
            String sectorSlug,
            @NotBlank @Size(max = 20) String code,
            @Size(max = 120) String displayName,
            @Size(max = 120) String vendorName,
            boolean showVendorName,
            @NotNull ConsentMethod consentMethod,
            @Size(max = 80) String instagram,
            BigDecimal latitude,
            BigDecimal longitude) {}

    /** Preço de referência como string decimal (RN-24); vazio = sem preço (RN-15). */
    public record OfferingRequest(
            @Pattern(regexp = "^\\d{1,8}(\\.\\d{1,2})?$") String price,
            @NotNull PriceUnit priceUnit,
            @NotNull Availability availability) {}

    @Operation(summary = "Cadastrar box conferido em campo, com o consentimento do feirante (RF-29, RN-13)")
    @PostMapping("/api/v1/admin/stalls")
    ResponseEntity<AdminStallDetail> create(@Valid @RequestBody StallCreateRequest body) {
        Market market = markets.require(body.marketSlug());
        Sector sector = body.sectorSlug() == null
                ? null
                : marketQuery.sector(market, body.sectorSlug()).orElseThrow(BusinessException::notFound);
        User actor = actor();
        Stall stall = stalls.createStall(
                market,
                sector,
                body.code().trim(),
                blankToNull(body.displayName()),
                blankToNull(body.vendorName()),
                body.showVendorName(),
                body.consentMethod(),
                actor,
                body.instagram(),
                false);
        if (body.latitude() != null || body.longitude() != null) {
            stalls.setLocation(stall.getPublicId(), body.latitude(), body.longitude());
        }
        audit.record(
                actor.getId(),
                "STALL_CREATED",
                "stall",
                stall.getPublicId(),
                null,
                Map.of("method", body.consentMethod().name()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(stalls.detail(stall.getPublicId(), AppLocale.PT_BR));
    }

    @Operation(summary = "Box com ofertas e fotos para edição no painel (RF-29)")
    @GetMapping("/api/v1/admin/stalls/{publicId}")
    ResponseEntity<AdminStallDetail> detail(@PathVariable UUID publicId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(stalls.detail(publicId, AppLocale.PT_BR));
    }

    @Operation(summary = "Criar ou atualizar oferta conferida em campo; renova a verificação do box (RF-29, RN-12)")
    @PutMapping("/api/v1/admin/stalls/{publicId}/offerings/{productSlug}")
    ResponseEntity<AdminStallDetail> saveOffering(
            @PathVariable UUID publicId, @PathVariable String productSlug, @Valid @RequestBody OfferingRequest body) {
        BigDecimal price = body.price() == null || body.price().isBlank()
                ? null
                : new BigDecimal(body.price()).setScale(2, RoundingMode.HALF_EVEN);
        stalls.saveOffering(
                publicId,
                new OfferingCommand(
                        catalog.requirePublicProduct(productSlug), price, body.priceUnit(), body.availability()));
        audit.record(actor().getId(), "OFFERING_SAVED", "stall", publicId, null, Map.of("product", productSlug));
        return ResponseEntity.ok(stalls.detail(publicId, AppLocale.PT_BR));
    }

    @Operation(summary = "Remover oferta do box (RF-29)")
    @DeleteMapping("/api/v1/admin/stalls/{publicId}/offerings/{productSlug}")
    ResponseEntity<AdminStallDetail> deleteOffering(@PathVariable UUID publicId, @PathVariable String productSlug) {
        stalls.deleteOffering(publicId, catalog.findBySlug(productSlug).orElseThrow(BusinessException::notFound));
        audit.record(actor().getId(), "OFFERING_REMOVED", "stall", publicId, null, Map.of("product", productSlug));
        return ResponseEntity.ok(stalls.detail(publicId, AppLocale.PT_BR));
    }

    @Operation(summary = "Enviar foto do box: principal ou da galeria; sem pessoas de rosto (RF-03, RN-06, RN-22)")
    @PostMapping(value = "/api/v1/admin/stalls/{publicId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AdminStallDetail> addPhoto(
            @PathVariable UUID publicId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean main)
            throws IOException {
        stalls.require(publicId);
        stalls.addPhoto(publicId, files.storePublic("stalls", file.getBytes()), main);
        audit.record(
                actor().getId(), "STALL_PHOTO_ADDED", "stall", publicId, null, Map.of("main", String.valueOf(main)));
        return ResponseEntity.status(HttpStatus.CREATED).body(stalls.detail(publicId, AppLocale.PT_BR));
    }

    @Operation(summary = "Tirar foto do box; o arquivo é apagado pela retenção (RN-13)")
    @DeleteMapping("/api/v1/admin/stalls/{publicId}/photos/{fileId}")
    ResponseEntity<AdminStallDetail> removePhoto(@PathVariable UUID publicId, @PathVariable UUID fileId) {
        stalls.removePhoto(publicId, fileId);
        audit.record(
                actor().getId(), "STALL_PHOTO_REMOVED", "stall", publicId, null, Map.of("file", fileId.toString()));
        return ResponseEntity.ok(stalls.detail(publicId, AppLocale.PT_BR));
    }

    @Operation(summary = "Trocar a foto de capa da feira (RF-01, RF-29)")
    @PostMapping(value = "/api/v1/admin/markets/{slug}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<AdminMarketView> cover(
            @PathVariable String slug,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String credit,
            @RequestParam(required = false) String creditUrl)
            throws IOException {
        markets.require(slug);
        if ((credit != null && credit.length() > 255)
                || (creditUrl != null && !creditUrl.isBlank() && !creditUrl.matches("^https?://\\S{1,490}$"))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        AdminMarketView view = markets.setCover(
                slug,
                files.storePublic("markets", file.getBytes()),
                blankToNull(credit),
                creditUrl == null || creditUrl.isBlank() ? null : creditUrl.trim(),
                clock.instant());
        audit.record(actor().getId(), "MARKET_COVER_SAVED", "market", slug);
        return ResponseEntity.ok(view);
    }

    private User actor() {
        return users.require(CurrentUser.require().publicId());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > 120) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value.trim();
    }
}
