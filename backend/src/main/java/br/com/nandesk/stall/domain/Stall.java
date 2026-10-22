package br.com.nandesk.stall.domain;

import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.persistence.AuditableEntity;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ponto de venda identificado por um código dentro da feira (glossário). */
@Entity
@Table(name = "stalls")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stall extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id", nullable = false)
    private Market market;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "vendor_name", length = 120)
    private String vendorName;

    @Column(name = "show_vendor_name", nullable = false)
    private boolean showVendorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_file_id")
    private StoredFile photo;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StallStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 20)
    private ConsentStatus consentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_method", nullable = false, length = 20)
    private ConsentMethod consentMethod;

    @Column(name = "consent_recorded_at", nullable = false)
    private Instant consentRecordedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consent_recorded_by", nullable = false)
    private User consentRecordedBy;

    @Column(name = "consent_revoked_at")
    private Instant consentRevokedAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    /** Box fictício da demonstração (ADR-0017, ADR-0018): a interface o marca como exemplo. */
    @Column(name = "demo", nullable = false)
    private boolean demo;

    /** Perfil do Instagram já normalizado ({@link InstagramHandle}), quando o feirante quis mostrar. */
    @Column(name = "instagram_handle", length = 30)
    private String instagramHandle;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StallOffering> offerings = new ArrayList<>();

    public Stall(
            Market market,
            Sector sector,
            String code,
            String displayName,
            ConsentMethod consentMethod,
            User recordedBy,
            Instant recordedAt) {
        this.market = market;
        this.sector = sector;
        this.code = code;
        this.displayName = displayName;
        this.status = StallStatus.ACTIVE;
        this.consentStatus = ConsentStatus.GRANTED;
        this.consentMethod = consentMethod;
        this.consentRecordedBy = recordedBy;
        this.consentRecordedAt = recordedAt;
    }

    /** Só é público com status ativo e consentimento concedido (RN-13). */
    public boolean isPublic() {
        return status == StallStatus.ACTIVE && consentStatus == ConsentStatus.GRANTED;
    }

    /** Nome do feirante só com consentimento específico (RN-14). */
    public String publicVendorName() {
        return showVendorName ? vendorName : null;
    }

    /** Revogação do consentimento: oculta o box na hora (RN-13). */
    public void revokeConsent(Instant now) {
        this.consentStatus = ConsentStatus.REVOKED;
        this.consentRevokedAt = now;
    }
}
