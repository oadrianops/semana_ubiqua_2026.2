package br.com.nandesk.submission.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.commission.application.LedgerService;
import br.com.nandesk.file.application.FileService;
import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.report.application.ReportService;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.config.CacheConfig;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.security.SegregationOfDuties;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.stall.domain.StallOffering;
import br.com.nandesk.submission.application.SubmissionEvents.SubmissionReviewed;
import br.com.nandesk.submission.domain.ChangeSet;
import br.com.nandesk.submission.domain.ChangeSet.OfferingChange;
import br.com.nandesk.submission.domain.ChangeSetConflicts;
import br.com.nandesk.submission.domain.ChangeSetValidator;
import br.com.nandesk.submission.domain.PhotoRole;
import br.com.nandesk.submission.domain.RejectionReason;
import br.com.nandesk.submission.domain.Submission;
import br.com.nandesk.submission.domain.SubmissionPhoto;
import br.com.nandesk.submission.domain.SubmissionStatus;
import br.com.nandesk.submission.infra.SubmissionRepository;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.task.domain.CheckinFlag;
import br.com.nandesk.task.domain.TaskAssignment;
import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.task.domain.UpdateTask;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moderação (RF-21, RF-22): fila do mais antigo para o mais novo, comparação antes/depois, alertas (check-in, foto
 * repetida, reputação), aprovação (aplica o changeset, credita a comissão — RN-07 — e audita) e rejeição com motivo.
 * Ninguém modera o próprio envio (RN-11); mudanças concorrentes no box aparecem como conflito (RN-26).
 */
@Service
public class ModerationService {

    private final SubmissionRepository submissions;
    private final TaskService tasks;
    private final UserAccountService users;
    private final StallAdminService stalls;
    private final MarketQueryService markets;
    private final CatalogQueryService catalog;
    private final ChangeSetApplier applier;
    private final LedgerService ledger;
    private final ReportService reports;
    private final FileService files;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;
    private final MeterRegistry meters;
    private final Clock clock;

    public ModerationService(
            SubmissionRepository submissions,
            TaskService tasks,
            UserAccountService users,
            StallAdminService stalls,
            MarketQueryService markets,
            CatalogQueryService catalog,
            ChangeSetApplier applier,
            LedgerService ledger,
            ReportService reports,
            FileService files,
            AuditService audit,
            ApplicationEventPublisher events,
            AppProperties properties,
            MeterRegistry meters,
            Clock clock) {
        this.submissions = submissions;
        this.tasks = tasks;
        this.users = users;
        this.stalls = stalls;
        this.markets = markets;
        this.catalog = catalog;
        this.applier = applier;
        this.ledger = ledger;
        this.reports = reports;
        this.files = files;
        this.audit = audit;
        this.events = events;
        this.properties = properties;
        this.meters = meters;
        this.clock = clock;
    }

    public record QueueItem(
            UUID publicId,
            TaskType type,
            String marketName,
            String stallCode,
            String stallName,
            String collaboratorName,
            Instant createdAt,
            CheckinFlag checkinFlag) {}

    public record FieldDiff(String field, Object before, Object after) {}

    public record OfferingDiff(
            String productSlug,
            String productName,
            String action,
            Map<String, Object> before,
            Map<String, Object> after,
            boolean conflict) {}

    public record PhotoView(
            UUID fileId, String url, String thumbUrl, PhotoRole role, String productSlug, int duplicateCount) {}

    public record CheckInView(Integer distanceMeters, Integer accuracyMeters, CheckinFlag flag, Instant checkedInAt) {}

    public record SubmissionDetail(
            UUID publicId,
            UUID taskId,
            TaskType type,
            SubmissionStatus status,
            String marketName,
            String marketSlug,
            UUID stallId,
            String stallCode,
            String collaboratorName,
            int collaboratorReputation,
            boolean ownSubmission,
            MoneyDto commission,
            CheckInView checkIn,
            List<FieldDiff> stallDiff,
            List<OfferingDiff> offerings,
            List<PhotoView> photos,
            List<String> conflicts,
            String notes,
            ChangeSet changeSet,
            Instant createdAt,
            RejectionReason rejectionReason,
            String reviewComment) {}

    @Transactional(readOnly = true)
    public PageResponse<QueueItem> queue(String marketSlug, TaskType type, int page, int size) {
        Long marketId = marketSlug == null
                ? null
                : markets.findActive(marketSlug).map(m -> m.getId()).orElse(null);
        return PageResponse.of(
                submissions.queue(
                        SubmissionStatus.PENDING,
                        marketId,
                        type,
                        PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                s -> new QueueItem(
                        s.getPublicId(),
                        s.getTask().getType(),
                        s.getTask().getMarket().getName(),
                        s.getTask().getStall() == null
                                ? stallCodeOf(s.getChangeset())
                                : s.getTask().getStall().getCode(),
                        s.getTask().getStall() == null
                                ? displayNameOf(s.getChangeset())
                                : s.getTask().getStall().getDisplayName(),
                        s.getCollaborator().getName(),
                        s.getCreatedAt(),
                        s.getAssignment().getCheckinFlag()));
    }

    @Transactional(readOnly = true)
    public SubmissionDetail detail(UUID submissionId, UUID moderatorId) {
        Submission s = require(submissionId);
        User moderator = users.require(moderatorId);
        AppLocale locale = AppLocale.current();
        UpdateTask task = s.getTask();
        Stall stall = task.getStall();
        Map<String, StallOffering> current = currentOfferings(stall);
        List<String> conflicts = conflicts(s, stall, current);

        List<FieldDiff> stallDiff = new ArrayList<>();
        ChangeSet cs = s.getChangeset();
        if (task.getType() == TaskType.NEW_STALL && cs.stall() != null) {
            stallDiff.add(new FieldDiff("code", null, cs.stall().code()));
            stallDiff.add(new FieldDiff("displayName", null, cs.stall().displayName()));
            stallDiff.add(new FieldDiff("sectorSlug", null, cs.stall().sectorSlug()));
            stallDiff.add(new FieldDiff("vendorName", null, cs.stall().vendorName()));
            if (cs.stall().consent() != null) {
                stallDiff.add(new FieldDiff(
                        "consent.publish", null, cs.stall().consent().publish()));
                stallDiff.add(new FieldDiff(
                        "consent.showVendorName", null, cs.stall().consent().showVendorName()));
                stallDiff.add(new FieldDiff(
                        "consent.method", null, cs.stall().consent().method()));
            }
        } else if (stall != null && cs.stall() != null && cs.stall().changes() != null) {
            cs.stall()
                    .changes()
                    .forEach((field, value) -> stallDiff.add(new FieldDiff(
                            field,
                            switch (field) {
                                case "displayName" -> stall.getDisplayName();
                                case "sectorSlug" ->
                                    stall.getSector() == null
                                            ? null
                                            : stall.getSector().getSlug();
                                default -> null;
                            },
                            value)));
        }

        List<OfferingDiff> offeringDiffs = new ArrayList<>();
        for (OfferingChange change : cs.offeringsOrEmpty()) {
            String slug = change.productSlug();
            StallOffering before = slug == null ? null : current.get(slug);
            String name = slug == null
                    ? change.suggestion().namePt()
                    : catalog.findBySlug(slug).map(p -> p.name(locale)).orElse(slug);
            offeringDiffs.add(new OfferingDiff(
                    slug,
                    name,
                    change.action().name(),
                    before == null ? null : offeringMap(before),
                    afterMap(change),
                    slug != null && conflicts.contains("offering:" + slug)));
        }

        List<PhotoView> photos = s.getPhotos().stream()
                .map(p -> new PhotoView(
                        p.getFile().getPublicId(),
                        p.getFile().url(false),
                        p.getFile().url(true),
                        p.getRole(),
                        p.getProductSlug(),
                        files.duplicatesOf(p.getFile()).size()))
                .toList();
        TaskAssignment a = s.getAssignment();
        int reputation = users.findProfile(s.getCollaborator().getId())
                .map(p -> p.getReputationScore())
                .orElse(properties.reputation().initialScore());
        return new SubmissionDetail(
                s.getPublicId(),
                task.getPublicId(),
                task.getType(),
                s.getStatus(),
                task.getMarket().getName(),
                task.getMarket().getSlug(),
                stall == null ? null : stall.getPublicId(),
                stall == null ? stallCodeOf(cs) : stall.getCode(),
                s.getCollaborator().getName(),
                reputation,
                s.getCollaborator().getId().equals(moderator.getId()),
                MoneyDto.brl(task.getCommissionAmount()),
                new CheckInView(
                        a.getCheckinDistanceMeters(),
                        a.getCheckinAccuracyMeters(),
                        a.getCheckinFlag(),
                        a.getCheckedInAt()),
                stallDiff,
                offeringDiffs,
                photos,
                conflicts,
                cs.notes(),
                cs,
                s.getCreatedAt(),
                s.getRejectionReason(),
                s.getReviewComment());
    }

    /**
     * Aprova: aplica o changeset (ou o ajustado pelo moderador), atualiza a verificação, credita a comissão e
     * audita, tudo numa transação; notificação e reputação seguem depois do commit.
     */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.SEARCH, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SUGGESTIONS, allEntries = true),
                @CacheEvict(cacheNames = CacheConfig.SNAPSHOT, allEntries = true)
            })
    public void approve(UUID submissionId, UUID moderatorId, ChangeSet adjusted, boolean acceptConflicts) {
        Submission s = requirePending(submissionId);
        User moderator = users.require(moderatorId);
        SegregationOfDuties.checkNotSelf(moderator.getId(), s.getCollaborator().getId());
        UpdateTask task = s.getTask();
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        List<String> conflicts = conflicts(s, task.getStall(), currentOfferings(task.getStall()));
        if (!conflicts.isEmpty() && !acceptConflicts) {
            throw new BusinessException(ErrorCode.SUBMISSION_CONFLICT, Map.of("conflicts", conflicts));
        }
        ChangeSet finalChangeSet = s.getChangeset();
        if (adjusted != null) {
            ChangeSetValidator.validate(
                    adjusted, task.getType().name(), 0, properties.photos().maxPerSubmission());
            finalChangeSet = new ChangeSet(
                    adjusted.taskType(),
                    adjusted.stall(),
                    adjusted.offerings(),
                    s.getChangeset().photos(),
                    adjusted.notes(),
                    adjusted.verification());
            s.setFinalChangeset(finalChangeSet);
        }
        Stall stall = applier.apply(s, finalChangeSet);
        Instant now = clock.instant();
        s.setStatus(SubmissionStatus.APPROVED);
        s.setReviewedBy(moderator.getId());
        s.setReviewedAt(now);
        tasks.complete(task, s.getAssignment());
        ledger.creditCommission(
                s.getCollaborator().getId(),
                task.getId(),
                task.getCommissionAmount(),
                "Tarefa " + task.getType().name() + " — box " + stall.getCode() + " ("
                        + task.getMarket().getName() + ")");
        if (task.getType() == TaskType.VERIFY_REPORT) {
            reports.resolveForTask(task.getId());
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("stall", stall.getPublicId().toString());
        after.put("adjusted", adjusted != null);
        after.put("conflictsAccepted", conflicts);
        audit.record(moderator.getId(), "SUBMISSION_APPROVED", "submission", s.getPublicId(), null, after);
        meters.timer("nandesk.moderation.duration").record(Duration.between(s.getCreatedAt(), now));
        events.publishEvent(new SubmissionReviewed(
                s.getCollaborator().getId(),
                s.getPublicId(),
                task.getPublicId(),
                true,
                task.getCommissionAmount(),
                null));
    }

    /** Rejeita com motivo obrigatório; a tarefa volta para OPEN ou é cancelada (RF-22). Rejeição não credita (RN-07). */
    @Transactional
    public void reject(
            UUID submissionId, UUID moderatorId, RejectionReason reason, String comment, boolean reopenTask) {
        if (reason == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Submission s = requirePending(submissionId);
        User moderator = users.require(moderatorId);
        SegregationOfDuties.checkNotSelf(moderator.getId(), s.getCollaborator().getId());
        UpdateTask task = s.getTask();
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        Instant now = clock.instant();
        s.setStatus(SubmissionStatus.REJECTED);
        s.setRejectionReason(reason);
        s.setReviewComment(comment == null || comment.isBlank() ? null : comment.trim());
        s.setReviewedBy(moderator.getId());
        s.setReviewedAt(now);
        for (SubmissionPhoto photo : s.getPhotos()) {
            photo.getFile().setDeleteAfter(now.plus(properties.retention().rejectedPhotos()));
        }
        tasks.reject(task, s.getAssignment(), reopenTask);
        audit.record(
                moderator.getId(),
                "SUBMISSION_REJECTED",
                "submission",
                s.getPublicId(),
                null,
                Map.of("reason", reason.name(), "reopenTask", reopenTask));
        meters.timer("nandesk.moderation.duration").record(Duration.between(s.getCreatedAt(), now));
        events.publishEvent(new SubmissionReviewed(
                s.getCollaborator().getId(), s.getPublicId(), task.getPublicId(), false, null, reason.name()));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return submissions.countByStatus(SubmissionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Submission> reviewedSince(Instant since) {
        return submissions.reviewedSince(since);
    }

    private List<String> conflicts(Submission s, Stall stall, Map<String, StallOffering> current) {
        if (stall == null) {
            return List.of();
        }
        Map<String, Integer> versions = new HashMap<>();
        current.forEach((slug, o) -> versions.put(slug, o.getVersion()));
        return ChangeSetConflicts.detect(s.getChangeset(), stall.getVersion(), versions);
    }

    private Map<String, StallOffering> currentOfferings(Stall stall) {
        Map<String, StallOffering> map = new HashMap<>();
        if (stall != null) {
            stalls.offeringsOf(stall).forEach(o -> map.put(o.getProduct().getSlug(), o));
        }
        return map;
    }

    private static Map<String, Object> offeringMap(StallOffering o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("price", MoneyDto.brl(o.getReferencePrice()));
        m.put("priceUnit", o.getPriceUnit());
        m.put("availability", o.getAvailability());
        m.put("lastVerifiedAt", o.getLastVerifiedAt());
        m.put("version", o.getVersion());
        return m;
    }

    private static Map<String, Object> afterMap(OfferingChange c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("price", c.referencePrice());
        m.put("priceUnit", c.priceUnit());
        m.put("availability", c.availability());
        return m;
    }

    private static String stallCodeOf(ChangeSet cs) {
        return cs.stall() == null ? null : cs.stall().code();
    }

    private static String displayNameOf(ChangeSet cs) {
        return cs.stall() == null ? null : cs.stall().displayName();
    }

    private Submission require(UUID id) {
        return submissions.findByPublicId(id).orElseThrow(BusinessException::notFound);
    }

    private Submission requirePending(UUID id) {
        Submission s = require(id);
        if (s.getStatus() != SubmissionStatus.PENDING) {
            throw new BusinessException(ErrorCode.TASK_NOT_AVAILABLE);
        }
        return s;
    }
}
