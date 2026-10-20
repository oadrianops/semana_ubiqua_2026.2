package br.com.nandesk.user.application;

/** Eventos do módulo de usuários. */
public final class UserEvents {

    private UserEvents() {}

    /** Conta suspensa ou com exclusão pedida: as reservas ativas são canceladas (RF-31, RN-20). */
    public record UserBlocked(Long userId, String reason) {}
}
