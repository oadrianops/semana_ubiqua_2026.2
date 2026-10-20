package br.com.nandesk.notification.application;

import br.com.nandesk.commission.application.LedgerService.PayoutRegistered;
import br.com.nandesk.notification.domain.NotificationType;
import br.com.nandesk.submission.application.SubmissionEvents.SubmissionReviewed;
import br.com.nandesk.task.application.TaskEvents.ReservationExpired;
import br.com.nandesk.task.application.TaskEvents.ReservationExpiring;
import br.com.nandesk.task.application.TaskEvents.TaskPublished;
import br.com.nandesk.user.application.UserAccountService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Traduz eventos de domínio em notificações (RF-14), na mesma transação do evento. Só colaboradores ativos
 * inscritos na feira recebem as de novas tarefas.
 */
@Component
public class NotificationListeners {

    private final NotificationService notifications;
    private final UserAccountService users;

    public NotificationListeners(NotificationService notifications, UserAccountService users) {
        this.notifications = notifications;
        this.users = users;
    }

    @EventListener
    public void onTaskPublished(TaskPublished e) {
        List<Long> subscribers = users.activeSubscriberIds(e.marketId());
        if (subscribers.isEmpty()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("market", e.marketName());
        params.put("type", e.type());
        notifications.createForAll(
                subscribers,
                NotificationType.NEW_TASKS,
                params,
                "/collaborate/tasks/" + e.taskPublicId(),
                "market:" + e.marketId());
    }

    @EventListener
    public void onSubmissionReviewed(SubmissionReviewed e) {
        Map<String, Object> params = new HashMap<>();
        if (e.approved()) {
            params.put(
                    "amount", e.commission() == null ? "0.00" : e.commission().toPlainString());
            notifications.create(
                    e.collaboratorId(), NotificationType.SUBMISSION_APPROVED, params, "/collaborate/earnings", null);
        } else {
            params.put("reason", e.reason());
            notifications.create(
                    e.collaboratorId(), NotificationType.SUBMISSION_REJECTED, params, "/collaborate/history", null);
        }
    }

    @EventListener
    public void onPayout(PayoutRegistered e) {
        notifications.create(
                e.collaboratorId(),
                NotificationType.PAYOUT_REGISTERED,
                Map.of("amount", e.amount().toPlainString()),
                "/collaborate/earnings",
                null);
    }

    @EventListener
    public void onExpiring(ReservationExpiring e) {
        notifications.create(
                e.userId(),
                NotificationType.RESERVATION_EXPIRING,
                Map.of("market", e.marketName(), "deadline", e.deadline().toString()),
                "/collaborate/tasks/" + e.taskPublicId(),
                null);
    }

    @EventListener
    public void onExpired(ReservationExpired e) {
        notifications.create(
                e.userId(),
                NotificationType.RESERVATION_EXPIRED,
                Map.of("market", e.marketName()),
                "/collaborate",
                null);
    }
}
