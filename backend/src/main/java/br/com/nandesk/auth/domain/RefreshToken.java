package br.com.nandesk.auth.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import br.com.nandesk.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Token de renovação (12.1): só o hash SHA-256 é guardado; rotação a cada uso; reapresentar um token já revogado
 * revoga a família inteira (detecção de roubo).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "family_id", nullable = false, length = 36)
    private UUID familyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RefreshToken(
            User user,
            UUID familyId,
            String tokenHash,
            Instant expiresAt,
            Instant absoluteExpiresAt,
            String userAgent) {
        this.user = user;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
        this.userAgent = userAgent;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now) && absoluteExpiresAt.isAfter(now);
    }
}
