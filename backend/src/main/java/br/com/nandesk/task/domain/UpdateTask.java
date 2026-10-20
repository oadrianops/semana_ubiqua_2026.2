package br.com.nandesk.task.domain;

import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.persistence.AuditableEntity;
import br.com.nandesk.stall.domain.Stall;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tarefa ("corrida") publicada pela plataforma (RF-13). {@code version} faz o bloqueio otimista do aceite
 * concorrente (RN-01); a coluna gerada {@code active_stall_id} garante, no banco, uma tarefa ativa por box (RN-25).
 */
@Entity
@Table(name = "update_tasks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateTask extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20)
    private TaskOrigin origin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id")
    private Stall stall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_sector_id")
    private Sector suggestedSector;

    @Column(name = "instructions", length = 500)
    private String instructions;

    @Column(name = "priority", nullable = false)
    private int priority = 2;

    /** Cópia da regra vigente na criação (RN-07). */
    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public UpdateTask(
            TaskType type,
            TaskOrigin origin,
            Market market,
            Stall stall,
            Sector suggestedSector,
            String instructions,
            int priority,
            BigDecimal commissionAmount,
            Long createdBy) {
        this.type = type;
        this.origin = origin;
        this.market = market;
        this.stall = stall;
        this.suggestedSector = suggestedSector;
        this.instructions = instructions;
        this.priority = priority;
        this.commissionAmount = commissionAmount;
        this.createdBy = createdBy;
        this.status = TaskStatus.ON_HOLD;
    }

    /** Aplica uma transição validada pela máquina de estados (9.7). */
    public void moveTo(TaskStatus next, Instant now) {
        TaskStateMachine.require(status, next);
        if (next == TaskStatus.OPEN && publishedAt == null) {
            publishedAt = now;
        }
        if (next == TaskStatus.COMPLETED || next == TaskStatus.CANCELLED) {
            closedAt = now;
        }
        status = next;
    }
}
