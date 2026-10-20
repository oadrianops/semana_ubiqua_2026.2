package br.com.nandesk.submission.application;

import br.com.nandesk.file.application.FileService;
import br.com.nandesk.file.domain.FileStatus;
import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.crypto.Hashing;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.submission.domain.ChangeSet;
import br.com.nandesk.submission.domain.ChangeSet.PhotoRef;
import br.com.nandesk.submission.domain.ChangeSetValidator;
import br.com.nandesk.submission.domain.PhotoRole;
import br.com.nandesk.submission.domain.RejectionReason;
import br.com.nandesk.submission.domain.Submission;
import br.com.nandesk.submission.domain.SubmissionPhoto;
import br.com.nandesk.submission.domain.SubmissionStatus;
import br.com.nandesk.submission.infra.SubmissionRepository;
import br.com.nandesk.task.application.TaskAdminQueries;
import br.com.nandesk.task.application.TaskService;
import br.com.nandesk.task.domain.AssignmentOutcome;
import br.com.nandesk.task.domain.TaskAssignment;
import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.task.domain.UpdateTask;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Envio da atualização (RF-18) — passo 3 de 3. JSON pequeno com os {@code fileId} das fotos que já subiram uma a
 * uma; a {@code Idempotency-Key} garante que repetir não duplica (RNF-03): mesma chave e mesmo conteúdo devolvem o
 * envio já criado; conteúdo diferente responde {@code IDEMPOTENCY_KEY_REUSED}.
 */
@Service
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final TaskService tasks;
    private final TaskAdminQueries taskQueries;
    private final UserAccountService users;
    private final FileService files;
    private final JsonMapper json;
    private final AppProperties properties;

    public SubmissionService(
            SubmissionRepository submissions,
            TaskService tasks,
            TaskAdminQueries taskQueries,
            UserAccountService users,
            FileService files,
            JsonMapper json,
            AppProperties properties) {
        this.submissions = submissions;
        this.tasks = tasks;
        this.taskQueries = taskQueries;
        this.users = users;
        this.files = files;
        this.json = json;
        this.properties = properties;
    }

    public record SubmissionReceipt(
            UUID submissionId, UUID taskId, SubmissionStatus status, Instant receivedAt, boolean created) {}

    public record HistoryItem(
            UUID taskId,
            TaskType type,
            TaskStatus taskStatus,
            String marketName,
            String stallCode,
            String stallName,
            MoneyDto commission,
            Instant acceptedAt,
            Instant submittedAt,
            AssignmentOutcome outcome,
            Instant outcomeAt,
            SubmissionStatus submissionStatus,
            RejectionReason rejectionReason,
            String reviewComment) {}

    @Transactional(noRollbackFor = BusinessException.class)
    public SubmissionReceipt submit(UUID taskId, UUID userId, UUID idempotencyKey, ChangeSet changeSet) {
        User user = users.require(userId);
        UpdateTask task = tasks.requireTask(taskId);
        String hash = requestHash(changeSet);

        Optional<Submission> existing = submissions.findByCollaboratorIdAndIdempotencyKey(user.getId(), idempotencyKey);
        if (existing.isPresent()) {
            Submission s = existing.get();
            if (!s.getRequestHash().equals(hash) || !s.getTask().getId().equals(task.getId())) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            return receipt(s, false);
        }

        TaskAssignment assignment = tasks.requireSubmittable(task, user);
        AppProperties.Photos photoRules = properties.photos();
        ChangeSetValidator.validate(
                changeSet, task.getType().name(), photoRules.minPerSubmission(), photoRules.maxPerSubmission());

        // Valida todas as fotos antes de alterar qualquer uma (nada fica vinculado pela metade).
        Set<UUID> used = new HashSet<>();
        List<StoredFile> photos = changeSet.photosOrEmpty().stream()
                .map(ref -> attachable(ref.fileId(), user, used))
                .toList();
        Submission submission = new Submission(task, assignment, user, changeSet, idempotencyKey, hash);
        for (int i = 0; i < photos.size(); i++) {
            PhotoRef ref = changeSet.photosOrEmpty().get(i);
            StoredFile file = photos.get(i);
            file.setStatus(FileStatus.ATTACHED);
            file.setDeleteAfter(null);
            submission.getPhotos().add(new SubmissionPhoto(submission, file, role(ref.role()), ref.productSlug(), i));
        }
        Submission saved = submissions.saveAndFlush(submission);
        tasks.markSubmitted(task, assignment);
        return receipt(saved, true);
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(UUID userId) {
        User user = users.require(userId);
        Map<Long, Submission> byAssignment = new HashMap<>();
        submissions
                .findAllByCollaboratorIdOrderByCreatedAtDesc(user.getId())
                .forEach(s -> byAssignment.put(s.getAssignment().getId(), s));
        return taskQueries.assignmentsOf(user.getId()).stream()
                .map(a -> {
                    UpdateTask t = a.getTask();
                    Submission s = byAssignment.get(a.getId());
                    return new HistoryItem(
                            t.getPublicId(),
                            t.getType(),
                            t.getStatus(),
                            t.getMarket().getName(),
                            t.getStall() == null ? null : t.getStall().getCode(),
                            t.getStall() == null ? null : t.getStall().getDisplayName(),
                            MoneyDto.brl(t.getCommissionAmount()),
                            a.getAcceptedAt(),
                            a.getSubmittedAt(),
                            a.getOutcome(),
                            a.getOutcomeAt(),
                            s == null ? null : s.getStatus(),
                            s == null ? null : s.getRejectionReason(),
                            s == null ? null : s.getReviewComment());
                })
                .toList();
    }

    /** Foto TEMP do próprio colaborador, ainda sem vínculo e dentro do prazo (RN-27). */
    private StoredFile attachable(String fileId, User owner, Set<UUID> used) {
        UUID id;
        try {
            id = UUID.fromString(fileId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UPLOAD_NOT_AVAILABLE);
        }
        StoredFile file = files.find(id).orElseThrow(() -> new BusinessException(ErrorCode.UPLOAD_NOT_AVAILABLE));
        boolean ownedTemp = file.getStatus() == FileStatus.TEMP
                && file.getOwner() != null
                && file.getOwner().getId().equals(owner.getId());
        if (!ownedTemp || !used.add(id)) {
            throw new BusinessException(ErrorCode.UPLOAD_NOT_AVAILABLE);
        }
        return file;
    }

    private String requestHash(ChangeSet changeSet) {
        return Hashing.sha256Hex(json.writeValueAsString(changeSet));
    }

    private static PhotoRole role(String role) {
        try {
            return PhotoRole.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            return PhotoRole.EVIDENCE;
        }
    }

    private static SubmissionReceipt receipt(Submission s, boolean created) {
        return new SubmissionReceipt(
                s.getPublicId(), s.getTask().getPublicId(), s.getStatus(), s.getCreatedAt(), created);
    }
}
