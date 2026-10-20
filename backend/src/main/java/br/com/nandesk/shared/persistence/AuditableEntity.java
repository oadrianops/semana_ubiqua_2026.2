package br.com.nandesk.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidade exposta na API: {@code public_id} (UUID), {@code created_at}/{@code updated_at} em UTC e
 * {@code version} para bloqueio otimista (10.1). Os instantes vêm do bean {@code Clock} ({@link ClockHolder}).
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36, unique = true)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public UUID getPublicId() {
        return publicId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    @PrePersist
    @PreUpdate
    void onWrite() {
        Instant now = ClockHolder.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
