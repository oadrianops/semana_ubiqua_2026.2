package br.com.nandesk.user.domain;

/** Situação da conta (10.3). {@code DELETION_PENDING} bloqueia o login de imediato (RN-20). */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DELETION_PENDING,
    ANONYMIZED
}
