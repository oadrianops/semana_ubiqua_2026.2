package br.com.nandesk.user.domain;

import br.com.nandesk.shared.persistence.ClockHolder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Perfil 1:1 do colaborador: chave PIX cifrada (13.1), reputação (RN-17) e preferência de push. */
@Entity
@Table(name = "collaborator_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollaboratorProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "pix_key_type", length = 20)
    private PixKeyType pixKeyType;

    @Column(name = "pix_key_encrypted", length = 512)
    private byte[] pixKeyEncrypted;

    @Column(name = "pix_key_masked", length = 60)
    private String pixKeyMasked;

    @Column(name = "reputation_score", nullable = false)
    private int reputationScore;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CollaboratorProfile(User user, int initialReputation) {
        this.user = user;
        this.reputationScore = initialReputation;
    }

    @PrePersist
    @PreUpdate
    void onWrite() {
        updatedAt = ClockHolder.now();
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || (o instanceof CollaboratorProfile other && userId != null && Objects.equals(userId, other.userId));
    }

    @Override
    public int hashCode() {
        return CollaboratorProfile.class.hashCode();
    }

    public enum PixKeyType {
        RANDOM,
        EMAIL,
        PHONE,
        CPF
    }
}
