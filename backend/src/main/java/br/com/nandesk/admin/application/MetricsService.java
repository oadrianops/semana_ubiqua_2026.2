package br.com.nandesk.admin.application;

import br.com.nandesk.commission.application.BudgetService;
import br.com.nandesk.commission.application.LedgerService;
import br.com.nandesk.search.application.SearchMetrics;
import br.com.nandesk.shared.time.BelemTime;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.stall.application.StallMetrics;
import br.com.nandesk.submission.application.ModerationService;
import br.com.nandesk.submission.domain.Submission;
import br.com.nandesk.submission.domain.SubmissionStatus;
import br.com.nandesk.task.application.TaskAdminQueries;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.task.domain.TaskStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Painel de indicadores (RF-32): leitura agregada, sem dados pessoais. Responde aos objetivos OBJ-1 a OBJ-5. */
@Service
@Transactional(readOnly = true)
public class MetricsService {

    private final StallMetrics stalls;
    private final TaskAdminQueries tasks;
    private final TaskService taskService;
    private final ModerationService moderation;
    private final LedgerService ledger;
    private final BudgetService budgets;
    private final SearchMetrics search;
    private final Clock clock;

    public MetricsService(
            StallMetrics stalls,
            TaskAdminQueries tasks,
            TaskService taskService,
            ModerationService moderation,
            LedgerService ledger,
            BudgetService budgets,
            SearchMetrics search,
            Clock clock) {
        this.stalls = stalls;
        this.tasks = tasks;
        this.taskService = taskService;
        this.moderation = moderation;
        this.ledger = ledger;
        this.budgets = budgets;
        this.search = search;
        this.clock = clock;
    }

    public record Overview(
            long publishedStalls,
            double verifiedLast30DaysPct,
            double withPhotoPct,
            Map<TaskStatus, Long> tasksByStatus,
            long pendingSubmissions,
            Double avgModerationHours,
            Double approvalRatePct,
            MoneyDto creditedThisMonth,
            MoneyDto paidThisMonth,
            BudgetService.Overview budget,
            long searchesLast7Days,
            double searchHitRatePct,
            List<Map<String, Object>> topZeroResultTerms,
            Map<String, Long> searchesByLocale,
            long activeCollaboratorsThisMonth,
            Instant generatedAt) {}

    public Overview overview() {
        Instant now = clock.instant();
        long published = stalls.publicStalls();
        Instant since30 = now.minus(Duration.ofDays(30));
        List<Submission> reviewed = moderation.reviewedSince(since30);
        Double avgHours = reviewed.isEmpty()
                ? null
                : reviewed.stream()
                                .mapToLong(s -> Duration.between(s.getCreatedAt(), s.getReviewedAt())
                                        .toMinutes())
                                .average()
                                .orElse(0)
                        / 60.0;
        Double approval = reviewed.isEmpty()
                ? null
                : 100.0
                        * reviewed.stream()
                                .filter(s -> s.getStatus() == SubmissionStatus.APPROVED)
                                .count()
                        / reviewed.size();
        YearMonth month = YearMonth.from(BelemTime.today(clock));
        Instant monthStart = BelemTime.startOfDay(month.atDay(1));
        Instant monthEnd = BelemTime.startOfDay(month.plusMonths(1).atDay(1));
        Instant since7 =
                BelemTime.startOfDay(LocalDate.from(BelemTime.today(clock)).minusDays(6));
        long searches = search.countSince(since7);
        long hits = search.countWithResultsSince(since7);
        return new Overview(
                published,
                pct(stalls.publicVerifiedSince(since30), published),
                pct(stalls.publicWithPhoto(), published),
                tasks.countByStatus(),
                moderation.pendingCount(),
                avgHours,
                approval,
                MoneyDto.brl(ledger.creditedBetween(monthStart, monthEnd)),
                MoneyDto.brl(ledger.paidBetween(monthStart, monthEnd)),
                budgets.overview(null, taskService.committedAmount()),
                searches,
                pct(hits, searches),
                search.topZeroResultSince(since30, 10),
                search.byLocaleSince(since30),
                tasks.activeCollaboratorsSince(monthStart),
                now);
    }

    private static double pct(long part, long total) {
        return total == 0 ? 0 : Math.round(1000.0 * part / total) / 10.0;
    }
}
