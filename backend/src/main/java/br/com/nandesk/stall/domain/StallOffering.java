package br.com.nandesk.stall.domain;

import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.shared.persistence.AuditableEntity;
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

/** Produto vendido em um box, com preço de referência opcional (RN-15) e disponibilidade. */
@Entity
@Table(name = "stall_offerings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StallOffering extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stall_id", nullable = false)
    private Stall stall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "reference_price", precision = 10, scale = 2)
    private BigDecimal referencePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", length = 20)
    private PriceUnit priceUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false, length = 20)
    private Availability availability;

    @Column(name = "last_verified_at", nullable = false)
    private Instant lastVerifiedAt;

    public StallOffering(
            Stall stall,
            Product product,
            BigDecimal referencePrice,
            PriceUnit priceUnit,
            Availability availability,
            Instant verifiedAt) {
        this.stall = stall;
        this.product = product;
        this.referencePrice = referencePrice;
        this.priceUnit = priceUnit;
        this.availability = availability;
        this.lastVerifiedAt = verifiedAt;
    }
}
