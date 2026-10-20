package br.com.nandesk.report.api;

import br.com.nandesk.report.application.ReportService;
import br.com.nandesk.report.application.ReportService.ReportView;
import br.com.nandesk.report.domain.ReportReason;
import br.com.nandesk.report.domain.ReportStatus;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Denúncias: envio público (login opcional) e triagem pela moderação (RF-07). */
@RestController
@Tag(name = "Denúncias")
public class ReportController {

    private final ReportService reports;
    private final UserAccountService users;

    public ReportController(ReportService reports, UserAccountService users) {
        this.reports = reports;
        this.users = users;
    }

    /** {@code website} é o campo-armadilha (honeypot): humano não preenche; robô sim (RN-16). */
    public record ReportRequest(
            @NotNull UUID stallId,
            String productSlug,
            @NotNull ReportReason reason,
            @Size(max = 500) String comment,
            String website) {}

    @Operation(summary = "Denunciar informação incorreta (RF-07)")
    @PostMapping("/api/v1/reports")
    ResponseEntity<Map<String, Object>> create(@Valid @RequestBody ReportRequest body) {
        if (body.website() != null && !body.website().isBlank()) {
            // Honeypot: responde como sucesso, sem gravar nada.
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("received", true));
        }
        Long reporter = CurrentUser.find()
                .flatMap(u -> users.findByPublicId(u.publicId()))
                .map(u -> u.getId())
                .orElse(null);
        UUID id = reports.create(body.stallId(), body.productSlug(), body.reason(), body.comment(), reporter);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("received", true, "reportId", id));
    }

    @Operation(summary = "Denúncias para triagem (RF-07)")
    @GetMapping("/api/v1/moderation/reports")
    ResponseEntity<PageResponse<ReportView>> list(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reports.list(status, page, size));
    }

    @Operation(summary = "Descartar denúncia (RF-07)")
    @PostMapping("/api/v1/moderation/reports/{publicId}/dismiss")
    ResponseEntity<Void> dismiss(@PathVariable UUID publicId) {
        reports.dismiss(
                publicId, users.require(CurrentUser.require().publicId()).getId());
        return ResponseEntity.noContent().build();
    }
}
