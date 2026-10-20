package br.com.nandesk.task.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.commission.application.BudgetService;
import br.com.nandesk.commission.application.CommissionRuleService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.geo.GeoDistance;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.time.BelemTime;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.task.application.TaskEvents.AssignmentClosed;
import br.com.nandesk.task.application.TaskEvents.ReservationExpired;
import br.com.nandesk.task.application.TaskEvents.ReservationExpiring;
import br.com.nandesk.task.application.TaskEvents.TaskPublished;
import br.com.nandesk.task.application.TaskViews.AcceptResult;
import br.com.nandesk.task.application.TaskViews.AvailableTasks;
import br.com.nandesk.task.application.TaskViews.CheckInResult;
import br.com.nandesk.task.application.TaskViews.MarketInfo;
import br.com.nandesk.task.application.TaskViews.MyAssignment;
import br.com.nandesk.task.application.TaskViews.StallInfo;
import br.com.nandesk.task.application.TaskViews.TaskDetail;
import br.com.nandesk.task.application.TaskViews.TaskSummary;
import br.com.nandesk.task.domain.AcceptancePolicy;
import br.com.nandesk.task.domain.AssignmentOutcome;
import br.com.nandesk.task.domain.CheckInPolicy;
import br.com.nandesk.task.domain.TaskAssignment;
import br.com.nandesk.task.domain.TaskOrigin;
import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.task.domain.UpdateTask;
import br.com.nandesk.task.infra.TaskAssignmentRepository;
import br.com.nandesk.task.infra.UpdateTaskRepository;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import br.com.nandesk.user.domain.UserStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida da tarefa (9.7): criação (RF-13), aceite (RF-16), check-in (RF-17), desistência e expiração (RF-19).
 * O aceite concorrente é resolvido por bloqueio otimista (RN-01).
 */
@Service
public class TaskService {

    private final UpdateTaskRepository tasks;
    private final TaskAssignmentRepository assignments;
    private final UserAccountService users;
    private final CommissionRuleService commissionRules;
    private final BudgetService budgets;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meters;
    private final Clock clock;
    private final AcceptancePolicy acceptance;
    private final CheckInPolicy checkIn;
    private final int minReputation;

    public TaskService(
            UpdateTaskRepository tasks,
            TaskAssignmentRepository assignments,
            UserAccountService users,
            CommissionRuleService commissionRules,
            BudgetService budgets,
            AuditService audit,
            ApplicationEventPublisher events,
            MeterRegistry meters,
            AppProperties properties,
            Clock clock) {
        this.tasks = tasks;
        this.assignments = assignments;
        this.users = users;
        this.commissionRules = commissionRules;
        this.budgets = budgets;
        this.audit = audit;
        this.events = events;
        this.meters = meters;
        this.clock = clock;
        AppProperties.Tasks t = properties.tasks();
        this.minReputation = properties.reputation().minToAccept();
        this.acceptance = new AcceptancePolicy(
                t.maxActivePerCollaborator(),
                t.maxSubmissionsPerDay(),
                minReputation,
                t.checkinDeadline(),
                t.submissionDeadline(),
                t.expiryWarning());
        this.checkIn = new CheckInPolicy(properties.geo().accuracyToleranceMeters());
    }

    public AcceptancePolicy acceptancePolicy() {
        return acceptance;
    }

    // ---------------------------------------------------------------- criação (RF-13)

    /**
     * Cria a tarefa com a comissão da regra vigente (RN-07). Publica (OPEN) se houver orçamento; senão fica
     * ON_HOLD (RN-10). Respeita uma tarefa ativa por box (RN-25).
     */
    @Transactional
    public UpdateTask create(
            TaskType type,
            TaskOrigin origin,
            Market market,
            Stall stall,
            Sector sector,
            String instructions,
            int priority,
            Long createdBy) {
        if (type != TaskType.NEW_STALL && stall == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (stall != null && tasks.existsByStallIdAndStatusIn(stall.getId(), TaskStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        BigDecimal commission = commissionRules.currentAmount(type.name());
        UpdateTask task =
                new UpdateTask(type, origin, market, stall, sector, instructions, priority, commission, createdBy);
        Instant now = clock.instant();
        boolean publish = budgets.canPublish(tasks.committedAmount(), commission);
        if (publish) {
            task.moveTo(TaskStatus.OPEN, now);
        }
        UpdateTask saved;
        try {
            saved = tasks.saveAndFlush(task);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        if (createdBy != null) {
            audit.record(
                    createdBy,
                    "TASK_CREATED",
                    "update_task",
                    saved.getPublicId(),
                    null,
                    Map.of(
                            "type",
                            type.name(),
                            "status",
                            saved.getStatus().name(),
                            "commission",
                            commission.toPlainString()));
        }
        if (publish) {
            published(saved);
        }
        return saved;
    }

    /** Tarefa automática (job de frescor ou denúncias): ignora em silêncio se o box já tiver tarefa ativa. */
    @Transactional
    public Optional<UpdateTask> createAutomatic(TaskType type, TaskOrigin origin, Stall stall, String instructions) {
        if (tasks.existsByStallIdAndStatusIn(stall.getId(), TaskStatus.ACTIVE)) {
            return Optional.empty();
        }
        return Optional.of(create(type, origin, stall.getMarket(), stall, stall.getSector(), instructions, 2, null));
    }

    /** Publica uma tarefa ON_HOLD: com orçamento (RN-10) ou com comissão zero (modo voluntário). */
    @Transactional
    public UpdateTask publish(UUID taskId, boolean volunteer, Long actorId) {
        UpdateTask task = requireTask(taskId);
        if (task.getStatus() != TaskStatus.ON_HOLD) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        if (volunteer) {
            task.setCommissionAmount(BigDecimal.ZERO.setScale(2));
        } else if (!budgets.canPublish(tasks.committedAmount(), task.getCommissionAmount())) {
            throw new BusinessException(ErrorCode.BUDGET_EXHAUSTED);
        }
        task.moveTo(TaskStatus.OPEN, clock.instant());
        audit.record(
                actorId,
                "TASK_PUBLISHED",
                "update_task",
                task.getPublicId(),
                null,
                Map.of(
                        "volunteer",
                        volunteer,
                        "commission",
                        task.getCommissionAmount().toPlainString()));
        published(task);
        return task;
    }

    /** Publica as tarefas ON_HOLD quando há orçamento (job de hora em hora — RN-10). */
    @Transactional
    public int publishOnHold() {
        int published = 0;
        for (UpdateTask task : tasks.findAllByStatusOrderByCreatedAtAsc(TaskStatus.ON_HOLD)) {
            if (budgets.canPublish(tasks.committedAmount(), task.getCommissionAmount())) {
                task.moveTo(TaskStatus.OPEN, clock.instant());
                tasks.flush();
                published(task);
                published++;
            }
        }
        return published;
    }

    @Transactional
    public void cancel(UUID taskId, String reason, Long actorId) {
        UpdateTask task = requireTask(taskId);
        cancelTask(task, reason);
        audit.record(
                actorId,
                "TASK_CANCELLED",
                "update_task",
                task.getPublicId(),
                null,
                Map.of("reason", reason == null ? "" : reason));
    }

    /** Cancela as tarefas ativas de um box que saiu da vitrine (RN-13). */
    @Transactional
    public int cancelActiveForStall(Long stallId, String reason) {
        List<UpdateTask> active = tasks.findAllByStallIdAndStatusIn(stallId, TaskStatus.ACTIVE);
        active.forEach(t -> cancelTask(t, reason));
        return active.size();
    }

    /** Cancela as reservas de um usuário suspenso ou em exclusão (RF-31, RN-20). */
    @Transactional
    public int releaseAllOf(Long userId) {
        List<TaskAssignment> active = assignments.findActiveOf(userId);
        Instant now = clock.instant();
        for (TaskAssignment a : active) {
            UpdateTask task = a.getTask();
            if (task.getStatus() == TaskStatus.ACCEPTED || task.getStatus() == TaskStatus.CHECKED_IN) {
                a.close(AssignmentOutcome.RELEASED, now);
                transition(task, TaskStatus.OPEN, now);
                published(task);
            }
        }
        return active.size();
    }

    private void cancelTask(UpdateTask task, String reason) {
        Instant now = clock.instant();
        if (task.getStatus() == TaskStatus.ACCEPTED || task.getStatus() == TaskStatus.CHECKED_IN) {
            assignments
                    .findFirstByTaskIdAndOutcomeIsNull(task.getId())
                    .ifPresent(a -> a.close(AssignmentOutcome.RELEASED, now));
            transition(task, TaskStatus.OPEN, now);
        }
        if (task.getStatus().isActive()) {
            task.setCancelReason(reason);
            transition(task, TaskStatus.CANCELLED, now);
        }
    }

    // ---------------------------------------------------------------- consulta (RF-15)

    @Transactional(readOnly = true)
    public AvailableTasks available(
            UUID userId, Long marketId, TaskType type, Double lat, Double lng, AppLocale locale) {
        User user = users.require(userId);
        int reputation = users.profileOf(user).getReputationScore();
        String blocked = null;
        if (reputation < minReputation) {
            blocked = ErrorCode.REPUTATION_TOO_LOW.name();
        } else if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            blocked = ErrorCode.ACCOUNT_NOT_VERIFIED.name();
        }
        List<TaskSummary> items = tasks.findOpen(marketId, type).stream()
                .map(t -> summary(t, lat, lng, locale))
                .sorted(
                        lat == null || lng == null
                                ? Comparator.comparing((TaskSummary s) -> 0)
                                : Comparator.comparing(
                                        s -> s.distanceMeters() == null ? Integer.MAX_VALUE : s.distanceMeters()))
                .toList();
        return new AvailableTasks(items, reputation, minReputation, blocked == null, blocked);
    }

    @Transactional(readOnly = true)
    public TaskDetail detail(UUID taskId, UUID userId, AppLocale locale) {
        UpdateTask task = requireTask(taskId);
        User user = users.require(userId);
        Optional<TaskAssignment> mine = assignments
                .findFirstByTaskIdOrderByAcceptedAtDesc(task.getId())
                .filter(a -> a.getCollaborator().getId().equals(user.getId()));
        if (task.getStatus() != TaskStatus.OPEN && mine.isEmpty()) {
            throw BusinessException.notFound();
        }
        return detail(task, mine.orElse(null), locale);
    }

    // ---------------------------------------------------------------- aceite (RF-16, RN-01)

    @Transactional
    public AcceptResult accept(UUID taskId, UUID userId) {
        User user = requireActiveCollaborator(userId);
        UpdateTask task = requireTask(taskId);
        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_TAKEN);
        }
        Instant now = clock.instant();
        acceptance.checkCanAccept(
                assignments.countByCollaboratorIdAndOutcomeIsNull(user.getId()),
                submissionsToday(user.getId()),
                users.profileOf(user).getReputationScore());
        TaskAssignment assignment = new TaskAssignment(task, user, now, acceptance.checkinDeadline(now));
        try {
            transition(task, TaskStatus.ACCEPTED, now);
            tasks.saveAndFlush(task);
            assignments.saveAndFlush(assignment);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.TASK_ALREADY_TAKEN);
        }
        return new AcceptResult(
                task.getPublicId(),
                task.getStatus(),
                MoneyDto.brl(task.getCommissionAmount()),
                assignment.getCheckinDeadlineAt());
    }

    // ---------------------------------------------------------------- check-in (RF-17, RN-05)

    @Transactional(noRollbackFor = BusinessException.class)
    public CheckInResult checkIn(UUID taskId, UUID userId, double latitude, double longitude, double accuracyMeters) {
        User user = users.require(userId);
        UpdateTask task = requireTask(taskId);
        TaskAssignment assignment = requireOwnOpenAssignment(task, user);
        if (task.getStatus() != TaskStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        Instant now = clock.instant();
        if (acceptance.isExpired(assignment.getCheckinDeadlineAt(), now)) {
            expire(assignment, now);
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }
        Market market = task.getMarket();
        CheckInPolicy.Result result = checkIn.evaluate(
                market.getLatitude().doubleValue(),
                market.getLongitude().doubleValue(),
                market.getGeofenceRadiusMeters(),
                latitude,
                longitude,
                accuracyMeters);
        if (!result.accepted()) {
            throw new BusinessException(
                    ErrorCode.CHECKIN_OUT_OF_RANGE, Map.of("distanceMeters", result.distanceMeters()));
        }
        assignment.setCheckedInAt(now);
        assignment.setSubmissionDeadlineAt(acceptance.submissionDeadline(now));
        assignment.setCheckinLatitude(BigDecimal.valueOf(latitude).setScale(6, java.math.RoundingMode.HALF_EVEN));
        assignment.setCheckinLongitude(BigDecimal.valueOf(longitude).setScale(6, java.math.RoundingMode.HALF_EVEN));
        assignment.setCheckinAccuracyMeters((int) Math.round(accuracyMeters));
        assignment.setCheckinDistanceMeters(result.distanceMeters());
        assignment.setCheckinFlag(result.flag());
        transition(task, TaskStatus.CHECKED_IN, now);
        return new CheckInResult(
                task.getStatus(), result.distanceMeters(), result.flag(), assignment.getSubmissionDeadlineAt());
    }

    // ---------------------------------------------------------------- desistência e expiração (RF-19, RN-02)

    @Transactional
    public void release(UUID taskId, UUID userId) {
        User user = users.require(userId);
        UpdateTask task = requireTask(taskId);
        TaskAssignment assignment = requireOwnOpenAssignment(task, user);
        if (task.getStatus() != TaskStatus.ACCEPTED && task.getStatus() != TaskStatus.CHECKED_IN) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        Instant now = clock.instant();
        assignment.close(AssignmentOutcome.RELEASED, now);
        transition(task, TaskStatus.OPEN, now);
        events.publishEvent(new AssignmentClosed(user.getId()));
        published(task);
    }

    /** Expira reservas vencidas e avisa 15 min antes (job a cada 5 min — RN-02). */
    @Transactional
    public int expireReservations() {
        Instant now = clock.instant();
        int expired = 0;
        for (TaskAssignment a : assignments.findAllOpenReservations()) {
            Instant deadline = a.currentDeadline();
            if (deadline == null) {
                continue;
            }
            if (acceptance.isExpired(deadline, now)) {
                expire(a, now);
                expired++;
            } else if (a.getExpiryWarnedAt() == null && acceptance.shouldWarn(deadline, now)) {
                a.setExpiryWarnedAt(now);
                events.publishEvent(new ReservationExpiring(
                        a.getCollaborator().getId(),
                        a.getTask().getPublicId(),
                        a.getTask().getMarket().getName(),
                        deadline));
            }
        }
        return expired;
    }

    private void expire(TaskAssignment assignment, Instant now) {
        UpdateTask task = assignment.getTask();
        assignment.close(AssignmentOutcome.EXPIRED, now);
        if (task.getStatus() == TaskStatus.ACCEPTED || task.getStatus() == TaskStatus.CHECKED_IN) {
            transition(task, TaskStatus.OPEN, now);
            published(task);
        }
        events.publishEvent(new AssignmentClosed(assignment.getCollaborator().getId()));
        events.publishEvent(new ReservationExpired(
                assignment.getCollaborator().getId(),
                task.getPublicId(),
                task.getMarket().getName()));
    }

    // ---------------------------------------------------------------- integração com envios e moderação

    /** Valida que a tarefa pode receber o envio do colaborador (RF-18, RN-02, RN-04). */
    @Transactional(noRollbackFor = BusinessException.class)
    public TaskAssignment requireSubmittable(UpdateTask task, User user) {
        Optional<TaskAssignment> open = assignments
                .findFirstByTaskIdAndOutcomeIsNull(task.getId())
                .filter(a -> a.getCollaborator().getId().equals(user.getId()));
        if (open.isEmpty()) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        TaskAssignment assignment = open.get();
        if (task.getStatus() == TaskStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.CHECKIN_REQUIRED);
        }
        if (task.getStatus() != TaskStatus.CHECKED_IN) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        Instant now = clock.instant();
        if (acceptance.isExpired(assignment.getSubmissionDeadlineAt(), now)) {
            expire(assignment, now);
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }
        acceptance.checkCanSubmit(submissionsToday(user.getId()));
        return assignment;
    }

    @Transactional
    public void markSubmitted(UpdateTask task, TaskAssignment assignment) {
        Instant now = clock.instant();
        assignment.setSubmittedAt(now);
        transition(task, TaskStatus.SUBMITTED, now);
    }

    /** Envio aprovado: tarefa concluída e tentativa APPROVED (RF-22). */
    @Transactional
    public void complete(UpdateTask task, TaskAssignment assignment) {
        Instant now = clock.instant();
        assignment.close(AssignmentOutcome.APPROVED, now);
        transition(task, TaskStatus.COMPLETED, now);
        events.publishEvent(new AssignmentClosed(assignment.getCollaborator().getId()));
    }

    /** Envio rejeitado: tarefa volta para OPEN ou é cancelada, a critério do moderador (RF-22). */
    @Transactional
    public void reject(UpdateTask task, TaskAssignment assignment, boolean reopen) {
        Instant now = clock.instant();
        assignment.close(AssignmentOutcome.REJECTED, now);
        transition(task, reopen ? TaskStatus.OPEN : TaskStatus.CANCELLED, now);
        events.publishEvent(new AssignmentClosed(assignment.getCollaborator().getId()));
        if (reopen) {
            published(task);
        }
    }

    @Transactional(readOnly = true)
    public UpdateTask requireTask(UUID publicId) {
        return tasks.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
    }

    @Transactional(readOnly = true)
    public boolean stallHasActiveTask(Long stallId) {
        return tasks.existsByStallIdAndStatusIn(stallId, TaskStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public BigDecimal committedAmount() {
        return tasks.committedAmount();
    }

    @Transactional(readOnly = true)
    public long submissionsToday(Long userId) {
        LocalDate today = BelemTime.today(clock);
        return assignments.countByCollaboratorIdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
                userId, BelemTime.startOfDay(today), BelemTime.startOfNextDay(today));
    }

    public TaskDetail detail(UpdateTask task, TaskAssignment assignment, AppLocale locale) {
        MyAssignment mine = assignment == null
                ? null
                : new MyAssignment(
                        assignment.getAcceptedAt(),
                        assignment.getCheckinDeadlineAt(),
                        assignment.getCheckedInAt(),
                        assignment.getSubmissionDeadlineAt(),
                        assignment.getCheckinDistanceMeters(),
                        assignment.getCheckinFlag(),
                        assignment.getOutcome());
        Sector suggested = task.getSuggestedSector();
        return new TaskDetail(
                task.getPublicId(),
                task.getType(),
                task.getStatus(),
                marketInfo(task.getMarket()),
                stallInfo(task.getStall(), locale),
                suggested == null ? null : suggested.name(locale),
                suggested == null ? null : suggested.getSlug(),
                task.getInstructions(),
                MoneyDto.brl(task.getCommissionAmount()),
                task.getPublishedAt(),
                mine);
    }

    public TaskSummary summary(UpdateTask t, Double lat, Double lng, AppLocale locale) {
        Integer distance = lat == null || lng == null
                ? null
                : (int) Math.round(GeoDistance.meters(
                        lat,
                        lng,
                        t.getMarket().getLatitude().doubleValue(),
                        t.getMarket().getLongitude().doubleValue()));
        return new TaskSummary(
                t.getPublicId(),
                t.getType(),
                t.getStatus(),
                marketInfo(t.getMarket()),
                stallInfo(t.getStall(), locale),
                t.getSuggestedSector() == null ? null : t.getSuggestedSector().name(locale),
                MoneyDto.brl(t.getCommissionAmount()),
                t.getPriority(),
                t.getPublishedAt(),
                distance);
    }

    private static MarketInfo marketInfo(Market m) {
        return new MarketInfo(m.getSlug(), m.getName(), m.getLatitude(), m.getLongitude(), m.getGeofenceRadiusMeters());
    }

    private static StallInfo stallInfo(Stall s, AppLocale locale) {
        return s == null
                ? null
                : new StallInfo(
                        s.getPublicId(),
                        s.getCode(),
                        s.getDisplayName(),
                        s.getSector() == null ? null : s.getSector().name(locale),
                        s.getVersion());
    }

    private User requireActiveCollaborator(UUID userId) {
        User user = users.require(userId);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        return user;
    }

    private TaskAssignment requireOwnOpenAssignment(UpdateTask task, User user) {
        return assignments
                .findFirstByTaskIdAndOutcomeIsNull(task.getId())
                .filter(a -> a.getCollaborator().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_AVAILABLE));
    }

    private void transition(UpdateTask task, TaskStatus next, Instant now) {
        TaskStatus from = task.getStatus();
        task.moveTo(next, now);
        meters.counter("nandesk.tasks.transitions", "from", from.name(), "to", next.name())
                .increment();
    }

    private void published(UpdateTask task) {
        events.publishEvent(new TaskPublished(
                task.getId(),
                task.getPublicId(),
                task.getMarket().getId(),
                task.getMarket().getName(),
                task.getType().name()));
    }
}
