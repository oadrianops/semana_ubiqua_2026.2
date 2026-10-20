package br.com.nandesk.task.domain;

import java.util.EnumSet;
import java.util.Set;

/** Estados da tarefa (9.7). */
public enum TaskStatus {
    ON_HOLD,
    OPEN,
    ACCEPTED,
    CHECKED_IN,
    SUBMITTED,
    COMPLETED,
    CANCELLED;

    /** Estados que ocupam o box: no máximo uma tarefa assim por box (RN-25). */
    public static final Set<TaskStatus> ACTIVE = EnumSet.of(ON_HOLD, OPEN, ACCEPTED, CHECKED_IN, SUBMITTED);

    /** Estados de uma reserva em andamento do colaborador (RN-03). */
    public static final Set<TaskStatus> RESERVED = EnumSet.of(ACCEPTED, CHECKED_IN);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
