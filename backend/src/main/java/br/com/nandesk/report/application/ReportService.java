package br.com.nandesk.report.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.report.domain.Report;
import br.com.nandesk.report.domain.ReportReason;
import br.com.nandesk.report.domain.ReportStatus;
import br.com.nandesk.report.domain.ReportThreshold;
import br.com.nandesk.report.infra.ReportRepository;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.stall.application.StallQueryService;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.task.domain.TaskOrigin;
import br.com.nandesk.task.domain.TaskType;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Denúncias do público (RF-07): N denúncias abertas sobre o mesmo box numa janela geram uma tarefa
 * {@code VERIFY_REPORT}, se não houver tarefa ativa (RN-16, RN-25). O IP serve só para a limitação de taxa.
 */
@Service
public class ReportService {

    private final ReportRepository reports;
    private final StallQueryService stalls;
    private final CatalogQueryService catalog;
    private final TaskService tasks;
    private final AuditService audit;
    private final ReportThreshold threshold;
    private final AppProperties properties;
    private final Clock clock;

    public ReportService(
            ReportRepository reports,
            StallQueryService stalls,
            CatalogQueryService catalog,
            TaskService tasks,
            AuditService audit,
            AppProperties properties,
            Clock clock) {
        this.reports = reports;
        this.stalls = stalls;
        this.catalog = catalog;
        this.tasks = tasks;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
        this.threshold = new ReportThreshold(properties.reports().threshold());
    }

    public record ReportView(
            UUID publicId,
            UUID stallId,
            String stallCode,
            String marketSlug,
            String marketName,
            String productName,
            ReportReason reason,
            String comment,
            ReportStatus status,
            Instant createdAt) {}

    @Transactional
    public UUID create(UUID stallId, String productSlug, ReportReason reason, String comment, Long reporterUserId) {
        Stall stall = stalls.findByPublicId(stallId).filter(Stall::isPublic).orElseThrow(BusinessException::notFound);
        Product product =
                productSlug == null ? null : catalog.findBySlug(productSlug).orElse(null);
        String text = comment == null || comment.isBlank() ? null : comment.trim();
        Report report = reports.save(new Report(stall, product, reason, text, reporterUserId));
        Instant windowStart = clock.instant().minus(properties.reports().window());
        long open = reports.countByStallIdAndStatusAndCreatedAtAfter(stall.getId(), ReportStatus.OPEN, windowStart);
        if (threshold.shouldCreateTask(open, tasks.stallHasActiveTask(stall.getId()))) {
            tasks.createAutomatic(TaskType.VERIFY_REPORT, TaskOrigin.REPORTS, stall, summary(stall))
                    .ifPresent(task -> {
                        for (Report r : reports.findAllByStallIdAndStatus(stall.getId(), ReportStatus.OPEN)) {
                            r.setStatus(ReportStatus.TASK_CREATED);
                            r.setTaskId(task.getId());
                        }
                    });
        }
        return report.getPublicId();
    }

    @Transactional
    public void dismiss(UUID reportId, Long moderatorId) {
        Report report = reports.findByPublicId(reportId).orElseThrow(BusinessException::notFound);
        report.setStatus(ReportStatus.DISMISSED);
        report.setResolvedAt(clock.instant());
        audit.record(moderatorId, "REPORT_DISMISSED", "report", report.getPublicId());
    }

    /** Envio de verificação aprovado: as denúncias da tarefa ficam resolvidas. */
    @Transactional
    public void resolveForTask(Long taskId) {
        Instant now = clock.instant();
        reports.findAllByTaskId(taskId).forEach(r -> {
            r.setStatus(ReportStatus.RESOLVED);
            r.setResolvedAt(now);
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportView> list(ReportStatus status, int page, int size) {
        AppLocale locale = AppLocale.current();
        return PageResponse.of(
                reports.findAllByStatusOrderByCreatedAtDesc(
                        status == null ? ReportStatus.OPEN : status,
                        PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                r -> new ReportView(
                        r.getPublicId(),
                        r.getStall().getPublicId(),
                        r.getStall().getCode(),
                        r.getStall().getMarket().getSlug(),
                        r.getStall().getMarket().getName(),
                        r.getProduct() == null ? null : r.getProduct().name(locale),
                        r.getReason(),
                        r.getComment(),
                        r.getStatus(),
                        r.getCreatedAt()));
    }

    private String summary(Stall stall) {
        return "Verificar denúncias do box " + stall.getCode();
    }
}
