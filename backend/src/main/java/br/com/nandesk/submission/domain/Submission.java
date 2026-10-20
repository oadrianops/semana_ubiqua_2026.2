package br.com.nandesk.submission.domain;

import br.com.nandesk.shared.persistence.AuditableEntity;
import br.com.nandesk.task.domain.TaskAssignment;
import br.com.nandesk.task.domain.UpdateTask;
import br.com.nandesk.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Envio: alterações propostas + fotos, ao fim da tarefa (RF-18), idempotente por chave do aparelho (RNF-03). */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private UpdateTask task;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private User collaborator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changeset", nullable = false)
    private ChangeSet changeset;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_changeset")
    private ChangeSet finalChangeset;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "idempotency_key", nullable = false, length = 36)
    private UUID idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", length = 30)
    private RejectionReason rejectionReason;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SubmissionPhoto> photos = new ArrayList<>();

    public Submission(
            UpdateTask task,
            TaskAssignment assignment,
            User collaborator,
            ChangeSet changeset,
            UUID idempotencyKey,
            String requestHash) {
        this.task = task;
        this.assignment = assignment;
        this.collaborator = collaborator;
        this.changeset = changeset;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = SubmissionStatus.PENDING;
    }
}
