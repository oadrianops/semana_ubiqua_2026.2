package br.com.nandesk.commission.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Valor da comissão por tipo de tarefa com início de vigência; histórico preservado (RF-25). */
@Entity
@Table(name = "commission_rules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionRule extends BaseEntity {

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CommissionRule(String taskType, BigDecimal amount, Instant validFrom, Long createdBy) {
        this.taskType = taskType;
        this.amount = amount;
        this.validFrom = validFrom;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }
}
