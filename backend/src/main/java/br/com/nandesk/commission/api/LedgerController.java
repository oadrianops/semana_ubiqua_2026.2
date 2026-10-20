package br.com.nandesk.commission.api;

import br.com.nandesk.commission.application.LedgerService;
import br.com.nandesk.commission.application.LedgerService.Statement;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Extrato e saldo do colaborador (RF-26). */
@RestController
@Tag(name = "Comissões")
public class LedgerController {

    private final LedgerService ledger;
    private final UserAccountService users;

    public LedgerController(LedgerService ledger, UserAccountService users) {
        this.ledger = ledger;
        this.users = users;
    }

    @Operation(summary = "Meu extrato e saldo (RF-26)")
    @GetMapping("/api/v1/me/ledger")
    ResponseEntity<Statement> statement(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Long userId = users.require(CurrentUser.require().publicId()).getId();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ledger.statement(userId, page, size));
    }
}
