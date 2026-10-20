package br.com.nandesk.lgpd.api;

import br.com.nandesk.lgpd.application.PrivacyService;
import br.com.nandesk.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exportar meus dados e excluir minha conta (RF-12, LGPD art. 18). */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Conta")
public class PrivacyController {

    private final PrivacyService privacy;

    public PrivacyController(PrivacyService privacy) {
        this.privacy = privacy;
    }

    @Operation(summary = "Exportar meus dados em JSON (RF-12)")
    @GetMapping("/export")
    ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nandesk-meus-dados.json\"")
                .body(privacy.export(CurrentUser.require().publicId()));
    }

    @Operation(summary = "Pedir a exclusão da conta (RF-12, RN-20)")
    @PostMapping("/deletion-request")
    ResponseEntity<Map<String, Object>> delete() {
        return ResponseEntity.accepted()
                .body(privacy.requestDeletion(CurrentUser.require().publicId()));
    }
}
