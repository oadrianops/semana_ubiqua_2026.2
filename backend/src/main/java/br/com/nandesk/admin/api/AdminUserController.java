package br.com.nandesk.admin.api;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.audit.application.AuditService.AuditView;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.application.UserAdminService;
import br.com.nandesk.user.application.UserAdminService.AdminUserView;
import br.com.nandesk.user.domain.Role;
import br.com.nandesk.user.domain.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Usuários, papéis, suspensão (RF-31) e trilha de auditoria (RF-33). Só ADMIN. */
@RestController
@Tag(name = "Administração")
public class AdminUserController {

    private final UserAdminService users;
    private final UserAccountService accounts;
    private final AuditService audit;

    public AdminUserController(UserAdminService users, UserAccountService accounts, AuditService audit) {
        this.users = users;
        this.accounts = accounts;
        this.audit = audit;
    }

    public record UserUpdateRequest(Role role, UserStatus status) {}

    @Operation(summary = "Buscar usuários (RF-31)")
    @GetMapping("/api/v1/admin/users")
    ResponseEntity<PageResponse<AdminUserView>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(users.search(q, role, page, size));
    }

    @Operation(summary = "Alterar papel, suspender ou reativar (RF-31)")
    @PatchMapping("/api/v1/admin/users/{publicId}")
    ResponseEntity<AdminUserView> update(@PathVariable UUID publicId, @RequestBody UserUpdateRequest body) {
        Long admin = accounts.require(CurrentUser.require().publicId()).getId();
        return ResponseEntity.ok(users.update(publicId, body.role(), body.status(), admin));
    }

    @Operation(summary = "Trilha de auditoria (RF-33)")
    @GetMapping("/api/v1/admin/audit-log")
    ResponseEntity<PageResponse<AuditView>> auditLog(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(audit.search(entityType, action, null, page, size));
    }
}
