package br.com.nandesk.task.application;

import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.task.domain.AssignmentOutcome;
import br.com.nandesk.task.domain.CheckinFlag;
import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.domain.TaskType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Respostas das tarefas do colaborador (RF-15 a RF-19, exemplos 11.3). */
public final class TaskViews {

    private TaskViews() {}

    public record MarketInfo(String slug, String name, BigDecimal latitude, BigDecimal longitude, int radiusMeters) {}

    public record StallInfo(UUID publicId, String code, String name, String sectorName, int version) {}

    public record TaskSummary(
            UUID publicId,
            TaskType type,
            TaskStatus status,
            MarketInfo market,
            StallInfo stall,
            String suggestedSector,
            MoneyDto commission,
            int priority,
            Instant publishedAt,
            Integer distanceMeters) {}

    public record MyAssignment(
            Instant acceptedAt,
            Instant checkinDeadlineAt,
            Instant checkedInAt,
            Instant submissionDeadlineAt,
            Integer checkinDistanceMeters,
            CheckinFlag checkinFlag,
            AssignmentOutcome outcome) {}

    public record TaskDetail(
            UUID publicId,
            TaskType type,
            TaskStatus status,
            MarketInfo market,
            StallInfo stall,
            String suggestedSector,
            String suggestedSectorSlug,
            String instructions,
            MoneyDto commission,
            Instant publishedAt,
            MyAssignment myAssignment) {}

    public record AvailableTasks(
            List<TaskSummary> items, int reputation, int minimumReputation, boolean canAccept, String blockedReason) {}

    public record AcceptResult(UUID taskId, TaskStatus status, MoneyDto commission, Instant checkinDeadlineAt) {}

    public record CheckInResult(
            TaskStatus status, int distanceMeters, CheckinFlag flag, Instant submissionDeadlineAt) {}
}
