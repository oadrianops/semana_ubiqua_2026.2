package br.com.nandesk.audit.application;

import br.com.nandesk.audit.domain.AuditLog;
import br.com.nandesk.audit.infra.AuditLogRepository;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.shared.web.RequestIdFilter;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trilha de auditoria (RF-33): registrada na mesma transação da ação. Nunca recebe dados sensíveis em claro
 * (chave PIX, senha, token, e-mail completo).
 */
@Service
public class AuditService {

    private final AuditLogRepository logs;
    private final Clock clock;

    public AuditService(AuditLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    public record AuditView(
            Long actorUserId,
            String action,
            String entityType,
            String entityId,
            Map<String, Object> before,
            Map<String, Object> after,
            String requestId,
            Instant createdAt) {}

    @Transactional
    public void record(
            Long actorUserId,
            String action,
            String entityType,
            Object entityId,
            Map<String, Object> before,
            Map<String, Object> after) {
        logs.save(new AuditLog(
                actorUserId,
                action,
                entityType,
                String.valueOf(entityId),
                before,
                after,
                RequestIdFilter.current(),
                clock.instant()));
    }

    @Transactional
    public void record(Long actorUserId, String action, String entityType, Object entityId) {
        record(actorUserId, action, entityType, entityId, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditView> search(String entityType, String action, Long actorId, int page, int size) {
        return PageResponse.of(
                logs.search(
                        entityType, action, actorId, PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                a -> new AuditView(
                        a.getActorUserId(),
                        a.getAction(),
                        a.getEntityType(),
                        a.getEntityId(),
                        a.getBeforeData(),
                        a.getAfterData(),
                        a.getRequestId(),
                        a.getCreatedAt()));
    }
}
