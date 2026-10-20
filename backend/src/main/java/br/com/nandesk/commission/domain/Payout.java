package br.com.nandesk.commission.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Repasse feito fora da plataforma e registrado nela (P2, RF-27). A plataforma não executa pagamentos. */
@Entity
@Table(name = "payouts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payout extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "collaborator_id", nullable = false)
    private Long collaboratorId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PayoutMethod method;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "registered_by", nullable = false)
    private Long registeredBy;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Payout(
            Long collaboratorId,
            BigDecimal amount,
            PayoutMethod method,
            String externalReference,
            Instant paidAt,
            Long registeredBy,
            String notes) {
        this.collaboratorId = collaboratorId;
        this.amount = amount;
        this.method = method;
        this.externalReference = externalReference;
        this.paidAt = paidAt;
        this.registeredBy = registeredBy;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }
}
