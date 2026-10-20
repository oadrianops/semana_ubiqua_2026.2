package br.com.nandesk.admin.api;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.commission.application.BudgetService;
import br.com.nandesk.commission.application.BudgetService.BudgetRow;
import br.com.nandesk.commission.application.BudgetService.Overview;
import br.com.nandesk.commission.application.CommissionRuleService;
import br.com.nandesk.commission.application.CommissionRuleService.RuleView;
import br.com.nandesk.commission.application.LedgerService;
import br.com.nandesk.commission.application.LedgerService.EntryView;
import br.com.nandesk.commission.domain.EntryType;
import br.com.nandesk.commission.domain.PayoutMethod;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.application.UserAdminService;
import br.com.nandesk.user.application.UserAdminService.AdminUserView;
import br.com.nandesk.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comissões, saldos, repasses, ajustes e orçamento (RF-25 a RF-28). Só ADMIN. A plataforma não executa pagamentos:
 * o repasse é feito por fora e registrado aqui (P2).
 */
@RestController
@Tag(name = "Administração")
public class AdminFinanceController {

    private final CommissionRuleService rules;
    private final LedgerService ledger;
    private final BudgetService budgets;
    private final TaskService tasks;
    private final UserAccountService users;
    private final UserAdminService userAdmin;
    private final AuditService audit;

    public AdminFinanceController(
            CommissionRuleService rules,
            LedgerService ledger,
            BudgetService budgets,
            TaskService tasks,
            UserAccountService users,
            UserAdminService userAdmin,
            AuditService audit) {
        this.rules = rules;
        this.ledger = ledger;
        this.budgets = budgets;
        this.tasks = tasks;
        this.users = users;
        this.userAdmin = userAdmin;
        this.audit = audit;
    }

    public record RuleRequest(
            @NotBlank String taskType, @NotNull MoneyDto amount, Instant validFrom) {}

    public record BalanceView(AdminUserView collaborator, MoneyDto balance, boolean hasPixKey) {}

    public record PayoutRequest(
            @NotNull UUID collaboratorId,
            @NotNull MoneyDto amount,
            @NotNull PayoutMethod method,
            @Size(max = 120) String externalReference,
            Instant paidAt,
            @Size(max = 255) String notes) {}

    public record AdjustmentRequest(
            @NotNull UUID collaboratorId,
            @NotNull EntryType type,
            MoneyDto amount,
            UUID reversesEntryId,
            @NotBlank @Size(max = 255) String justification) {}

    public record BudgetRequest(
            @NotBlank String period,
            @NotBlank @Size(max = 120) String sponsorName,
            @NotNull MoneyDto amount) {}

    @Operation(summary = "Regras de comissão (RF-25)")
    @GetMapping("/api/v1/admin/commission-rules")
    ResponseEntity<List<RuleView>> rules() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(rules.list());
    }

    @Operation(summary = "Nova regra de comissão com início de vigência (RF-25)")
    @PostMapping("/api/v1/admin/commission-rules")
    ResponseEntity<RuleView> createRule(@Valid @RequestBody RuleRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rules.create(body.taskType(), body.amount().toBigDecimal(), body.validFrom(), admin()));
    }

    @Operation(summary = "Colaboradores com saldo acima do mínimo (RF-27)")
    @GetMapping("/api/v1/admin/balances")
    ResponseEntity<List<BalanceView>> balances() {
        var rows = ledger.balancesAtLeastMinimum();
        Map<Long, AdminUserView> byId = new java.util.HashMap<>();
        userAdmin
                .byIds(rows.stream()
                        .map(LedgerService.BalanceRow::collaboratorId)
                        .toList())
                .forEach(u -> byId.put(u.id(), u));
        List<BalanceView> views = rows.stream()
                .map(r -> new BalanceView(
                        byId.get(r.collaboratorId()),
                        r.balance(),
                        users.findProfile(r.collaboratorId())
                                .map(p -> p.getPixKeyEncrypted() != null)
                                .orElse(false)))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(views);
    }

    @Operation(summary = "Consultar a chave PIX do colaborador — acesso auditado (RF-27)")
    @GetMapping("/api/v1/admin/collaborators/{publicId}/pix-key")
    ResponseEntity<Map<String, String>> pixKey(@PathVariable UUID publicId) {
        User admin = admin();
        User collaborator =
                users.findByPublicId(publicId).orElseThrow(br.com.nandesk.shared.web.BusinessException::notFound);
        String key = users.revealPixKey(collaborator.getId()).orElse("");
        String type = users.findProfile(collaborator.getId())
                .map(p -> p.getPixKeyType() == null ? "" : p.getPixKeyType().name())
                .orElse("");
        audit.record(admin.getId(), "PIX_KEY_VIEWED", "user", publicId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("type", type, "key", key));
    }

    @Operation(summary = "Registrar repasse feito fora da plataforma (RF-27, RN-09, RN-11)")
    @PostMapping("/api/v1/admin/payouts")
    ResponseEntity<Map<String, UUID>> payout(@Valid @RequestBody PayoutRequest body) {
        User admin = admin();
        User collaborator = users.findByPublicId(body.collaboratorId())
                .orElseThrow(br.com.nandesk.shared.web.BusinessException::notFound);
        UUID id = ledger.registerPayout(
                admin.getId(),
                collaborator.getId(),
                body.amount().toBigDecimal(),
                body.method(),
                body.externalReference(),
                body.paidAt(),
                body.notes());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("payoutId", id));
    }

    @Operation(summary = "Repasses registrados (RF-27)")
    @GetMapping("/api/v1/admin/payouts")
    ResponseEntity<PageResponse<Map<String, Object>>> payouts(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ledger.payouts(page, size));
    }

    @Operation(summary = "Estorno ou ajuste com justificativa (RF-26, RN-08)")
    @PostMapping("/api/v1/admin/ledger-adjustments")
    ResponseEntity<EntryView> adjust(@Valid @RequestBody AdjustmentRequest body) {
        User admin = admin();
        User collaborator = users.findByPublicId(body.collaboratorId())
                .orElseThrow(br.com.nandesk.shared.web.BusinessException::notFound);
        BigDecimal amount =
                body.amount() == null ? BigDecimal.ZERO : body.amount().toBigDecimal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ledger.adjust(
                        admin.getId(),
                        collaborator.getId(),
                        body.type(),
                        amount,
                        body.reversesEntryId(),
                        body.justification()));
    }

    @Operation(summary = "Extrato de um colaborador (RF-26)")
    @GetMapping("/api/v1/admin/collaborators/{publicId}/ledger")
    ResponseEntity<LedgerService.Statement> collaboratorLedger(@PathVariable UUID publicId) {
        User collaborator =
                users.findByPublicId(publicId).orElseThrow(br.com.nandesk.shared.web.BusinessException::notFound);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ledger.statement(collaborator.getId(), 0, 100));
    }

    @Operation(summary = "Orçamentos (RF-28)")
    @GetMapping("/api/v1/admin/budgets")
    ResponseEntity<Map<String, Object>> budgets(@RequestParam(required = false) String period) {
        Overview overview = budgets.overview(period, tasks.committedAmount());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("overview", overview, "all", budgets.list()));
    }

    @Operation(summary = "Criar ou atualizar orçamento do mês por patrocinador (RF-28)")
    @PutMapping("/api/v1/admin/budgets")
    ResponseEntity<BudgetRow> saveBudget(@Valid @RequestBody BudgetRequest body) {
        return ResponseEntity.ok(
                budgets.upsert(body.period(), body.sponsorName(), body.amount().toBigDecimal(), admin().getId()));
    }

    private User admin() {
        return users.require(CurrentUser.require().publicId());
    }
}
