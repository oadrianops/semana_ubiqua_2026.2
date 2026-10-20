package br.com.nandesk.commission.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lançamento do extrato: somente inserido, nunca alterado ou apagado (RN-08). Tarefa e repasse são referenciados
 * pelo id para o módulo de comissões não depender do de tarefas (ADR-0003).
 */
@Entity
@Immutable
@Table(name = "ledger_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "collaborator_id", nullable = false, updatable = false)
    private Long collaboratorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, updatable = false, length = 20)
    private EntryType entryType;

    /** Valor com sinal (RN-09). */
    @Column(name = "amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "task_id", updatable = false)
    private Long taskId;

    @Column(name = "payout_id", updatable = false)
    private Long payoutId;

    @Column(name = "reverses_entry_id", updatable = false)
    private Long reversesEntryId;

    @Column(name = "description", nullable = false, updatable = false, length = 255)
    private String description;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntry(
            Long collaboratorId,
            EntryType entryType,
            BigDecimal signedAmount,
            Long taskId,
            Long payoutId,
            Long reversesEntryId,
            String description,
            Long createdBy,
            Instant createdAt) {
        this.collaboratorId = collaboratorId;
        this.entryType = entryType;
        this.amount = signedAmount;
        this.taskId = taskId;
        this.payoutId = payoutId;
        this.reversesEntryId = reversesEntryId;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
