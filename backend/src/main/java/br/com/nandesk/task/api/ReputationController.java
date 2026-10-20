package br.com.nandesk.task.api;

import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.task.application.ReputationService;
import br.com.nandesk.task.application.ReputationService.ReputationView;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reputação do colaborador com a explicação de como subir (RF-23). */
@RestController
@Tag(name = "Colaboração")
public class ReputationController {

    private final ReputationService reputation;
    private final UserAccountService users;

    public ReputationController(ReputationService reputation, UserAccountService users) {
        this.reputation = reputation;
        this.users = users;
    }

    @Operation(summary = "Minha reputação (RF-23, RN-17)")
    @GetMapping("/api/v1/me/reputation")
    ResponseEntity<ReputationView> mine() {
        Long userId = users.require(CurrentUser.require().publicId()).getId();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reputation.view(userId));
    }
}
