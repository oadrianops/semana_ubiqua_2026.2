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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Orçamento mensal do programa por patrocinador (RF-28, RN-10). */
@Entity
@Table(name = "program_budgets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramBudget extends BaseEntity {

    /** Período {@code AAAA-MM} no calendário de Belém. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "period", nullable = false, length = 7)
    private String period;

    @Column(name = "sponsor_name", nullable = false, length = 120)
    private String sponsorName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProgramBudget(String period, String sponsorName, BigDecimal amount, Long createdBy) {
        this.period = period;
        this.sponsorName = sponsorName;
        this.amount = amount;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }
}
