package br.com.nandesk.shared.web;

import org.springframework.http.HttpStatus;

/** Catálogo de códigos de erro de negócio (especificação 11.4). Cada código tem mensagem nos três idiomas. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN),
    SELF_MODERATION_FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    TASK_ALREADY_TAKEN(HttpStatus.CONFLICT),
    TASK_NOT_AVAILABLE(HttpStatus.CONFLICT),
    RESERVATION_EXPIRED(HttpStatus.CONFLICT),
    CHECKIN_REQUIRED(HttpStatus.CONFLICT),
    SUBMISSION_CONFLICT(HttpStatus.CONFLICT),
    STALE_VERSION(HttpStatus.CONFLICT),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),
    ACTIVE_TASK_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT),
    DAILY_SUBMISSION_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT),
    REPUTATION_TOO_LOW(HttpStatus.UNPROCESSABLE_CONTENT),
    CHECKIN_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_CONTENT),
    INVALID_PHOTO(HttpStatus.UNPROCESSABLE_CONTENT),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT),
    UPLOAD_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT),
    CONSENT_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT),
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_CONTENT),
    PAYOUT_BELOW_MINIMUM(HttpStatus.UNPROCESSABLE_CONTENT),
    BUDGET_EXHAUSTED(HttpStatus.UNPROCESSABLE_CONTENT),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    ACCOUNT_LOCKED_TEMPORARILY(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    /** Chave do título em messages*.properties. */
    public String titleKey() {
        return "error." + name() + ".title";
    }

    /** Chave do detalhe em messages*.properties. */
    public String detailKey() {
        return "error." + name() + ".detail";
    }

    /** URI estável do tipo do problema (RFC 9457). */
    public String typeUri() {
        return "https://nandesk.com.br/errors/" + name().toLowerCase().replace('_', '-');
    }
}
