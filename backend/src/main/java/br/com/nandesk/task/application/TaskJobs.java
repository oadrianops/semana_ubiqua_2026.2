package br.com.nandesk.task.application;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.application.StallAdminService.StallHidden;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.task.domain.TaskOrigin;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.user.application.UserEvents.UserBlocked;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jobs das tarefas (9.11) — expiração de reservas, frescor dos boxes e publicação de ON_HOLD — e reações a eventos
 * de outros módulos (box oculto, usuário bloqueado). Com mais de uma réplica, proteger com ShedLock (RNF-14).
 */
@Component
public class TaskJobs {

    private static final Logger log = LoggerFactory.getLogger(TaskJobs.class);

    private final TaskService tasks;
    private final StallAdminService stalls;
    private final AppProperties properties;
    private final Clock clock;

    public TaskJobs(TaskService tasks, StallAdminService stalls, AppProperties properties, Clock clock) {
        this.tasks = tasks;
        this.stalls = stalls;
        this.properties = properties;
        this.clock = clock;
    }

    /** RN-02 e RF-19: a cada 5 minutos. */
    @Scheduled(cron = "0 */5 * * * *", zone = "America/Belem")
    public void expireReservations() {
        int expired = tasks.expireReservations();
        if (expired > 0) {
            log.info("Reservas expiradas: {}", expired);
        }
    }

    /** RN-12 e RN-25: diário, 06:00. */
    @Scheduled(cron = "0 0 6 * * *", zone = "America/Belem")
    @Transactional
    public void createStaleStallTasks() {
        Instant before = clock.instant().minus(properties.tasks().staleAfter());
        int created = 0;
        for (Stall stall : stalls.staleStalls(before)) {
            if (tasks.createAutomatic(TaskType.UPDATE_STALL, TaskOrigin.STALE_DATA, stall, null)
                    .isPresent()) {
                created++;
            }
        }
        log.info("Tarefas de frescor criadas: {}", created);
    }

    /** RN-10: de hora em hora. */
    @Scheduled(cron = "0 0 * * * *", zone = "America/Belem")
    public void publishOnHold() {
        tasks.publishOnHold();
    }

    /** RN-13: box oculto cancela as tarefas ativas, na mesma transação. */
    @EventListener
    public void onStallHidden(StallHidden event) {
        tasks.cancelActiveForStall(event.stallId(), event.reason());
    }

    /** RF-31 e RN-20: suspensão ou exclusão cancela as reservas ativas. */
    @EventListener
    public void onUserBlocked(UserBlocked event) {
        tasks.releaseAllOf(event.userId());
    }
}
