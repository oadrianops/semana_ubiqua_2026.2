package br.com.nandesk.submission.infra;

import br.com.nandesk.submission.domain.Submission;
import br.com.nandesk.submission.domain.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** RN-22: a foto de evidência sai do envio quando a retenção vence; o envio segue no histórico. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SubmissionPhoto p where p.file.id in :fileIds")
    int deletePhotosByFileIds(@Param("fileIds") java.util.Collection<Long> fileIds);

    @EntityGraph(
            attributePaths = {"task", "task.market", "task.stall", "assignment", "collaborator", "photos", "photos.file"
            })
    Optional<Submission> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"task", "photos", "photos.file"})
    Optional<Submission> findByCollaboratorIdAndIdempotencyKey(Long collaboratorId, UUID idempotencyKey);

    Optional<Submission> findByAssignmentId(Long assignmentId);

    /** Fila de moderação: do mais antigo para o mais novo (RF-21). */
    @EntityGraph(attributePaths = {"task", "task.market", "task.stall", "collaborator"})
    @Query("select s from Submission s where s.status = :status"
            + " and (:marketId is null or s.task.market.id = :marketId)"
            + " and (:type is null or s.task.type = :type) order by s.createdAt asc")
    Page<Submission> queue(
            @Param("status") SubmissionStatus status,
            @Param("marketId") Long marketId,
            @Param("type") br.com.nandesk.task.domain.TaskType type,
            Pageable pageable);

    List<Submission> findAllByCollaboratorIdOrderByCreatedAtDesc(Long collaboratorId);

    long countByStatus(SubmissionStatus status);

    @Query("select s from Submission s where s.reviewedAt >= :since and s.reviewedAt is not null")
    List<Submission> reviewedSince(@Param("since") Instant since);
}
