package br.com.nandesk.task.application;

import java.util.UUID;

/** Eventos de domínio das tarefas, consumidos depois do commit (notificações e reputação — 9.3). */
public final class TaskEvents {

    private TaskEvents() {}

    /** Tarefa publicada (OPEN): colaboradores inscritos na feira são notificados (RF-14). */
    public record TaskPublished(Long taskId, UUID taskPublicId, Long marketId, String marketName, String type) {}

    /** Reserva perto do vencimento: aviso 15 min antes (RN-02, RF-19). */
    public record ReservationExpiring(Long userId, UUID taskPublicId, String marketName, java.time.Instant deadline) {}

    /** Reserva vencida e tarefa devolvida (RN-02). */
    public record ReservationExpired(Long userId, UUID taskPublicId, String marketName) {}

    /** Uma tentativa terminou: a reputação do colaborador é recalculada (RN-17). */
    public record AssignmentClosed(Long userId) {}
}
