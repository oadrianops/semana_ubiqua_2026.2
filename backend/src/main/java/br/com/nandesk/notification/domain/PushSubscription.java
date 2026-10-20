package br.com.nandesk.notification.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Inscrição de Web Push do navegador (9.8). Respostas 404/410 do serviço de push removem a inscrição. */
@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "endpoint", nullable = false, columnDefinition = "text")
    private String endpoint;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "endpoint_hash", nullable = false, length = 64)
    private String endpointHash;

    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;

    @Column(name = "auth", nullable = false, length = 255)
    private String auth;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PushSubscription(
            Long userId, String endpoint, String endpointHash, String p256dh, String auth, String userAgent) {
        this.userId = userId;
        this.endpoint = endpoint;
        this.endpointHash = endpointHash;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }
}
