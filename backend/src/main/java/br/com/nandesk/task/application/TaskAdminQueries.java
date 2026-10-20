package br.com.nandesk.task.application;

import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.task.application.TaskViews.TaskSummary;
import br.com.nandesk.task.domain.TaskAssignment;
import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.infra.TaskAssignmentRepository;
import br.com.nandesk.task.infra.UpdateTaskRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de leitura das tarefas para o painel, o histórico e os indicadores (RF-13, RF-20, RF-32). */
@Service
@Transactional(readOnly = true)
public class TaskAdminQueries {

    private final UpdateTaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final TaskService service;

    public TaskAdminQueries(UpdateTaskRepository tasks, TaskAssignmentRepository assignments, TaskService service) {
        this.tasks = tasks;
        this.assignments = assignments;
        this.service = service;
    }

    public PageResponse<TaskSummary> search(TaskStatus status, Long marketId, int page, int size, AppLocale locale) {
        return PageResponse.of(
                tasks.searchAdmin(status, marketId, PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                t -> service.summary(t, null, null, locale));
    }

    public Map<TaskStatus, Long> countByStatus() {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : tasks.countByStatus()) {
            counts.put((TaskStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    public List<TaskAssignment> assignmentsOf(Long userId) {
        return assignments.findAllByCollaboratorIdOrderByAcceptedAtDesc(userId);
    }

    public PageResponse<TaskAssignment> assignmentsPage(Long userId, int page, int size) {
        var result = assignments.findAllByCollaboratorIdOrderByAcceptedAtDesc(
                userId, PageRequest.of(Math.max(0, page), PageResponse.clampSize(size)));
        return PageResponse.of(result, a -> a);
    }

    public long activeCollaboratorsSince(Instant since) {
        return assignments.countActiveCollaboratorsSince(since);
    }

    @Transactional
    public int clearCheckinCoordinatesBefore(Instant before) {
        return assignments.clearCoordinatesBefore(before);
    }
}
