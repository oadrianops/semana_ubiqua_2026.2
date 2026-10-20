package br.com.nandesk.notification.domain;

/** Eventos que geram notificação (RF-14). */
public enum NotificationType {
    NEW_TASKS,
    SUBMISSION_APPROVED,
    SUBMISSION_REJECTED,
    PAYOUT_REGISTERED,
    RESERVATION_EXPIRING,
    RESERVATION_EXPIRED
}
