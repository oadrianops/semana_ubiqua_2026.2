package br.com.nandesk.task.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import br.com.nandesk.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cada aceite é uma tentativa, com check-in e resultado (9.7). A coluna gerada {@code active_task_id} impede,
 * no banco, duas tentativas em andamento para a mesma tarefa (RN-01).
 */
@Entity
@Table(name = "task_assignments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private UpdateTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private User collaborator;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "checkin_deadline_at", nullable = false)
    private Instant checkinDeadlineAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "submission_deadline_at")
    private Instant submissionDeadlineAt;

    /** Apagadas após a retenção; ficam só a distância e a marcação (RN-21). */
    @Column(name = "checkin_latitude", precision = 9, scale = 6)
    private BigDecimal checkinLatitude;

    @Column(name = "checkin_longitude", precision = 9, scale = 6)
    private BigDecimal checkinLongitude;

    @Column(name = "checkin_accuracy_m")
    private Integer checkinAccuracyMeters;

    @Column(name = "checkin_distance_m")
    private Integer checkinDistanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_flag", length = 20)
    private CheckinFlag checkinFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20)
    private AssignmentOutcome outcome;

    @Column(name = "outcome_at")
    private Instant outcomeAt;

    @Column(name = "expiry_warned_at")
    private Instant expiryWarnedAt;

    /** Quando o envio foi recebido; base do limite diário de envios (RN-04). */
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TaskAssignment(UpdateTask task, User collaborator, Instant acceptedAt, Instant checkinDeadlineAt) {
        this.task = task;
        this.collaborator = collaborator;
        this.acceptedAt = acceptedAt;
        this.checkinDeadlineAt = checkinDeadlineAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }

    public boolean isOpen() {
        return outcome == null;
    }

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }

    /** Prazo vigente: de envio após o check-in; de check-in antes (RN-02). */
    public Instant currentDeadline() {
        return checkedInAt != null ? submissionDeadlineAt : checkinDeadlineAt;
    }

    public void close(AssignmentOutcome result, Instant now) {
        this.outcome = result;
        this.outcomeAt = now;
    }
}
