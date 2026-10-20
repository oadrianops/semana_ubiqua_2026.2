package br.com.nandesk.submission.api;

import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.submission.application.ModerationService;
import br.com.nandesk.submission.application.ModerationService.QueueItem;
import br.com.nandesk.submission.application.ModerationService.SubmissionDetail;
import br.com.nandesk.submission.application.SubmissionService;
import br.com.nandesk.submission.application.SubmissionService.HistoryItem;
import br.com.nandesk.submission.application.SubmissionService.SubmissionReceipt;
import br.com.nandesk.submission.domain.ChangeSet;
import br.com.nandesk.submission.domain.RejectionReason;
import br.com.nandesk.task.domain.TaskType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Envio da atualização (RF-18), histórico (RF-20) e moderação (RF-21, RF-22). */
@RestController
@Tag(name = "Colaboração")
public class SubmissionController {

    private final SubmissionService submissions;
    private final ModerationService moderation;

    public SubmissionController(SubmissionService submissions, ModerationService moderation) {
        this.submissions = submissions;
        this.moderation = moderation;
    }

    public record ApproveRequest(ChangeSet finalChangeSet, boolean acceptConflicts) {}

    public record RejectRequest(
            @NotNull RejectionReason reason,
            @Size(max = 500) String comment,
            boolean reopenTask) {}

    @Operation(summary = "Enviar atualização — passo 3 de 3, idempotente (RF-18, RNF-03)")
    @PostMapping("/api/v1/tasks/{publicId}/submission")
    ResponseEntity<SubmissionReceipt> submit(
            @PathVariable UUID publicId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ChangeSet changeSet) {
        SubmissionReceipt receipt =
                submissions.submit(publicId, CurrentUser.require().publicId(), parse(idempotencyKey), changeSet);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(receipt);
    }

    @Operation(summary = "Histórico de tarefas do colaborador (RF-20)")
    @GetMapping("/api/v1/me/tasks")
    ResponseEntity<List<HistoryItem>> history() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(submissions.history(CurrentUser.require().publicId()));
    }

    @Operation(summary = "Fila de moderação (RF-21)")
    @GetMapping("/api/v1/moderation/submissions")
    ResponseEntity<PageResponse<QueueItem>> queue(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) TaskType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(moderation.queue(market, type, page, size));
    }

    @Operation(summary = "Detalhe do envio com antes/depois, check-in e alertas (RF-21)")
    @GetMapping("/api/v1/moderation/submissions/{publicId}")
    ResponseEntity<SubmissionDetail> detail(@PathVariable UUID publicId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(moderation.detail(publicId, CurrentUser.require().publicId()));
    }

    @Operation(summary = "Aprovar envio, opcionalmente com ajustes (RF-22)")
    @PostMapping("/api/v1/moderation/submissions/{publicId}/approve")
    ResponseEntity<Void> approve(@PathVariable UUID publicId, @RequestBody(required = false) ApproveRequest body) {
        moderation.approve(
                publicId,
                CurrentUser.require().publicId(),
                body == null ? null : body.finalChangeSet(),
                body != null && body.acceptConflicts());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rejeitar envio com motivo (RF-22)")
    @PostMapping("/api/v1/moderation/submissions/{publicId}/reject")
    ResponseEntity<Void> reject(@PathVariable UUID publicId, @Valid @RequestBody RejectRequest body) {
        moderation.reject(publicId, CurrentUser.require().publicId(), body.reason(), body.comment(), body.reopenTask());
        return ResponseEntity.noContent().build();
    }

    private static UUID parse(String key) {
        try {
            return UUID.fromString(key.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
