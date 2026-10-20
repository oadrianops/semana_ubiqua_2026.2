package br.com.nandesk.task.domain;

/** Resultado de uma tentativa (9.7); NULL no banco = em andamento. */
public enum AssignmentOutcome {
    RELEASED,
    EXPIRED,
    APPROVED,
    REJECTED
}
