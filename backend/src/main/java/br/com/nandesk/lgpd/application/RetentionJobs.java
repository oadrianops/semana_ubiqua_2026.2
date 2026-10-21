package br.com.nandesk.lgpd.application;

import br.com.nandesk.auth.application.AuthService;
import br.com.nandesk.file.application.FileService;
import br.com.nandesk.notification.application.NotificationService;
import br.com.nandesk.search.application.SearchMetrics;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.submission.application.SubmissionService;
import br.com.nandesk.task.application.TaskAdminQueries;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retenção automática (9.11, 13.4): coordenadas do check-in (RN-21), fotos vencidas (RN-22, RN-13), uploads órfãos
 * (RN-27), registros de busca, tokens expirados, notificações lidas antigas e exclusões de conta (RN-20).
 */
@Component
public class RetentionJobs {

    private static final Logger log = LoggerFactory.getLogger(RetentionJobs.class);

    private final TaskAdminQueries tasks;
    private final FileService files;
    private final SearchMetrics search;
    private final AuthService auth;
    private final NotificationService notifications;
    private final PrivacyService privacy;
    private final SubmissionService submissions;
    private final StallAdminService stalls;
    private final AppProperties properties;
    private final Clock clock;

    public RetentionJobs(
            TaskAdminQueries tasks,
            FileService files,
            SearchMetrics search,
            AuthService auth,
            NotificationService notifications,
            PrivacyService privacy,
            SubmissionService submissions,
            StallAdminService stalls,
            AppProperties properties,
            Clock clock) {
        this.tasks = tasks;
        this.files = files;
        this.search = search;
        this.auth = auth;
        this.notifications = notifications;
        this.privacy = privacy;
        this.submissions = submissions;
        this.stalls = stalls;
        this.properties = properties;
        this.clock = clock;
    }

    /** RetentionJob — diário, 03:00. */
    @Scheduled(cron = "0 0 3 * * *", zone = "America/Belem")
    public void retention() {
        AppProperties.Retention r = properties.retention();
        int coords = tasks.clearCheckinCoordinatesBefore(clock.instant().minus(r.checkinCoordinates()));
        java.util.List<Long> due = files.attachedDueForRetention();
        submissions.detachPhotos(due);
        stalls.clearPhotos(due);
        int photos = files.purgeAfterRetention();
        int searches = search.purgeOlderThan(r.searchLogs());
        int tokens = auth.purgeExpiredTokens();
        int read = notifications.purgeReadOlderThan(r.readNotifications());
        log.info(
                "Retenção: {} coordenadas, {} fotos, {} buscas, {} tokens, {} notificações",
                coords,
                photos,
                searches,
                tokens,
                read);
    }

    /** AnonymizationJob — diário, 04:00. */
    @Scheduled(cron = "0 0 4 * * *", zone = "America/Belem")
    public void anonymization() {
        int done = privacy.completePendingDeletions(properties.retention().accountDeletion());
        if (done > 0) {
            log.info("Contas anonimizadas: {}", done);
        }
    }

    /** OrphanUploadsJob — de hora em hora (RN-27). */
    @Scheduled(cron = "0 15 * * * *", zone = "America/Belem")
    public void orphanUploads() {
        int purged = files.purgeExpiredTemp();
        if (purged > 0) {
            log.info("Uploads temporários apagados: {}", purged);
        }
    }
}
