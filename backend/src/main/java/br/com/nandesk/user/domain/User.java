package br.com.nandesk.user.domain;

import br.com.nandesk.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Conta de colaborador, moderador ou administrador (seção 4). O feirante não é usuário na v1 (P1). */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "preferred_locale", nullable = false, length = 10)
    private String preferredLocale;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    @Column(name = "adult_declared_at", nullable = false)
    private Instant adultDeclaredAt;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public User(
            String name,
            String email,
            String passwordHash,
            Role role,
            UserStatus status,
            String preferredLocale,
            String termsVersion,
            Instant acceptedAt) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.preferredLocale = preferredLocale;
        this.termsVersion = termsVersion;
        this.termsAcceptedAt = acceptedAt;
        this.adultDeclaredAt = acceptedAt;
    }

    /** E-mail único, comparado sem diferenciar maiúsculas (RF-08). */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean canLogin() {
        return status == UserStatus.ACTIVE || status == UserStatus.PENDING_VERIFICATION;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
