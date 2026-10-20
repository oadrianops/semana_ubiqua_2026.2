package br.com.nandesk.task.domain;

import static br.com.nandesk.task.domain.TaskStatus.ACCEPTED;
import static br.com.nandesk.task.domain.TaskStatus.CANCELLED;
import static br.com.nandesk.task.domain.TaskStatus.CHECKED_IN;
import static br.com.nandesk.task.domain.TaskStatus.COMPLETED;
import static br.com.nandesk.task.domain.TaskStatus.ON_HOLD;
import static br.com.nandesk.task.domain.TaskStatus.OPEN;
import static br.com.nandesk.task.domain.TaskStatus.SUBMITTED;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Máquina de estados da tarefa (9.7): transição fora da tabela lança {@code TASK_NOT_AVAILABLE} (409). */
public final class TaskStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED.put(ON_HOLD, EnumSet.of(OPEN, CANCELLED));
        ALLOWED.put(OPEN, EnumSet.of(ACCEPTED, CANCELLED));
        ALLOWED.put(ACCEPTED, EnumSet.of(OPEN, CHECKED_IN));
        ALLOWED.put(CHECKED_IN, EnumSet.of(OPEN, SUBMITTED));
        ALLOWED.put(SUBMITTED, EnumSet.of(COMPLETED, OPEN, CANCELLED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(TaskStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(TaskStatus.class));
    }

    private TaskStateMachine() {}

    public static boolean canTransition(TaskStatus from, TaskStatus to) {
        return ALLOWED.get(from).contains(to);
    }

    public static void require(TaskStatus from, TaskStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
    }
}
