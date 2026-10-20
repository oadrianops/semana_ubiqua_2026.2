package br.com.nandesk.lgpd.application;

import br.com.nandesk.auth.application.AuthService;
import br.com.nandesk.commission.application.LedgerService;
import br.com.nandesk.commission.domain.LedgerEntry;
import br.com.nandesk.notification.application.NotificationService;
import br.com.nandesk.submission.application.SubmissionService;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.application.UserAdminService;
import br.com.nandesk.user.domain.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Direitos do titular (RF-12, 13.2): exportação em JSON e exclusão com anonimização (RN-20). Lançamentos financeiros
 * e auditoria permanecem, com a referência anonimizada.
 */
@Service
public class PrivacyService {

    private final UserAccountService users;
    private final UserAdminService userAdmin;
    private final SubmissionService submissions;
    private final LedgerService ledger;
    private final NotificationService notifications;
    private final AuthService auth;
    private final Clock clock;

    public PrivacyService(
            UserAccountService users,
            UserAdminService userAdmin,
            SubmissionService submissions,
            LedgerService ledger,
            NotificationService notifications,
            AuthService auth,
            Clock clock) {
        this.users = users;
        this.userAdmin = userAdmin;
        this.submissions = submissions;
        this.ledger = ledger;
        this.notifications = notifications;
        this.auth = auth;
        this.clock = clock;
    }

    /** Exportação com perfil, tarefas e envios (sem fotos de terceiros), extrato, notificações e consentimentos. */
    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userPublicId) {
        User user = users.require(userPublicId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", clock.instant());
        data.put("profile", users.profile(userPublicId));
        data.put(
                "consents",
                Map.of(
                        "termsVersion", user.getTermsVersion(),
                        "termsAcceptedAt", user.getTermsAcceptedAt(),
                        "adultDeclaredAt", user.getAdultDeclaredAt()));
        data.put("marketSubscriptionIds", users.subscribedMarketIds(user.getId()));
        data.put("tasks", submissions.history(userPublicId));
        List<Map<String, Object>> entries = ledger.allEntries(user.getId()).stream()
                .map(PrivacyService::entry)
                .toList();
        data.put("ledger", entries);
        data.put("balance", ledger.balance(user.getId()).toPlainString());
        data.put(
                "notifications",
                notifications.allOf(user.getId()).stream()
                        .map(n -> Map.of(
                                "type",
                                n.getType().name(),
                                "createdAt",
                                n.getCreatedAt(),
                                "read",
                                n.getReadAt() != null))
                        .toList());
        return data;
    }

    /** Pedido de exclusão: bloqueia o login, encerra sessões e cancela reservas (RN-20). */
    @Transactional
    public Map<String, Object> requestDeletion(UUID userPublicId) {
        User user = users.require(userPublicId);
        userAdmin.requestDeletion(user.getId());
        auth.revokeAllSessions(user.getId());
        BigDecimal balance = ledger.balance(user.getId());
        return Map.of("status", "DELETION_PENDING", "pendingBalance", balance.toPlainString());
    }

    /** Conclui exclusões sem saldo ou com prazo vencido (AnonymizationJob — RN-20). */
    @Transactional
    public int completePendingDeletions(java.time.Duration deadline) {
        Instant limit = clock.instant().minus(deadline);
        int done = 0;
        for (User user : userAdmin.pendingDeletion()) {
            boolean noBalance = ledger.balance(user.getId()).signum() <= 0;
            boolean expired = user.getDeletionRequestedAt() != null
                    && user.getDeletionRequestedAt().isBefore(limit);
            if (noBalance || expired) {
                notifications.deleteAllOf(user.getId());
                userAdmin.anonymize(user.getId());
                done++;
            }
        }
        return done;
    }

    private static Map<String, Object> entry(LedgerEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", e.getEntryType().name());
        m.put("amount", e.getAmount().toPlainString());
        m.put("description", e.getDescription());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
