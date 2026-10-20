package br.com.nandesk.notification.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import br.com.nandesk.shared.persistence.ClockHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Notificação in-app — fonte da verdade (9.8): chave de tradução + parâmetros; o texto é montado no idioma
 * atual do usuário.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "message_key", nullable = false, length = 80)
    private String messageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params")
    private Map<String, Object> params;

    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notification(
            Long userId, NotificationType type, String messageKey, Map<String, Object> params, String link) {
        this.userId = userId;
        this.type = type;
        this.messageKey = messageKey;
        this.params = params;
        this.link = link;
    }

    @PrePersist
    void onCreate() {
        createdAt = ClockHolder.now();
    }
}
