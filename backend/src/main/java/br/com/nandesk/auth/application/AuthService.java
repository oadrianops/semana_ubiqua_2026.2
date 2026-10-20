package br.com.nandesk.auth.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.auth.domain.LoginLockout;
import br.com.nandesk.auth.domain.OneTimeToken;
import br.com.nandesk.auth.domain.OneTimeToken.Purpose;
import br.com.nandesk.auth.domain.PasswordPolicy;
import br.com.nandesk.auth.domain.RefreshToken;
import br.com.nandesk.auth.infra.OneTimeTokenRepository;
import br.com.nandesk.auth.infra.RefreshTokenRepository;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.crypto.Hashing;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.Masking;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.Role;
import br.com.nandesk.user.domain.User;
import br.com.nandesk.user.domain.UserStatus;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro, login, renovação rotativa, logout, verificação de e-mail e redefinição de senha (RF-08 a RF-10, 12.1).
 * Erros de login são sempre genéricos; depois de 5 falhas a conta espera 1, 2, 4… minutos.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountService users;
    private final RefreshTokenRepository refreshTokens;
    private final OneTimeTokenRepository oneTimeTokens;
    private final TokenService tokens;
    private final MailService mail;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final AppProperties properties;
    private final Clock clock;
    private final PasswordPolicy passwordPolicy;
    private final String dummyHash;

    public AuthService(
            UserAccountService users,
            RefreshTokenRepository refreshTokens,
            OneTimeTokenRepository oneTimeTokens,
            TokenService tokens,
            MailService mail,
            PasswordEncoder passwordEncoder,
            AuditService audit,
            AppProperties properties,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.oneTimeTokens = oneTimeTokens;
        this.tokens = tokens;
        this.mail = mail;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
        this.passwordPolicy = new PasswordPolicy(loadCommonPasswords());
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record Session(TokenService.AccessToken access, String refreshToken, Instant refreshExpiresAt, User user) {}

    public PasswordPolicy passwordPolicy() {
        return passwordPolicy;
    }

    /** Cadastro de colaborador (RF-08): com e-mail ativo a conta nasce PENDING_VERIFICATION. */
    @Transactional
    public User register(String name, String email, String password, String locale) {
        validatePassword(password);
        boolean verify = mail.enabled();
        User user = users.create(
                name,
                email,
                password,
                Role.COLLABORATOR,
                verify ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE,
                AppLocale.fromTag(locale).tag());
        if (verify) {
            issueAndSend(user, Purpose.EMAIL_VERIFICATION);
        }
        return user;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public Session login(String email, String password, String userAgent) {
        Instant now = clock.instant();
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (user.isLocked(now)) {
            long seconds = Duration.between(now, user.getLockedUntil()).toSeconds() + 1;
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED_TEMPORARILY, Map.of("retryAfterSeconds", seconds));
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            long minutes = LoginLockout.lockMinutes(failures, properties.auth().lockoutThreshold());
            if (minutes > 0) {
                user.setLockedUntil(now.plus(Duration.ofMinutes(minutes)));
                log.info(
                        "Conta {} em espera por {} min após {} falhas",
                        Masking.email(user.getEmail()),
                        minutes,
                        failures);
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (!user.canLogin()) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        return newSession(user, UUID.randomUUID(), now.plus(properties.jwt().refreshAbsoluteTtl()), userAgent);
    }

    /**
     * Renovação com rotação a cada uso (12.1). Reapresentar um token já revogado revoga a família inteira e vai para
     * a auditoria (sinal de roubo).
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public Session refresh(String rawToken, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        Instant now = clock.instant();
        RefreshToken token = refreshTokens
                .findByTokenHash(Hashing.sha256Hex(rawToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        if (token.getRevokedAt() != null) {
            refreshTokens.revokeFamily(token.getFamilyId(), now);
            audit.record(
                    token.getUser().getId(),
                    "REFRESH_TOKEN_REUSE",
                    "user",
                    token.getUser().getPublicId());
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (!token.isActive(now) || !token.getUser().canLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        Session session = newSession(token.getUser(), token.getFamilyId(), token.getAbsoluteExpiresAt(), userAgent);
        token.setRevokedAt(now);
        refreshTokens
                .findByTokenHash(Hashing.sha256Hex(session.refreshToken()))
                .ifPresent(n -> token.setReplacedById(n.getId()));
        return session;
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokens
                .findByTokenHash(Hashing.sha256Hex(rawToken))
                .ifPresent(t -> refreshTokens.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        OneTimeToken token = consume(rawToken, Purpose.EMAIL_VERIFICATION);
        User user = token.getUser();
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(clock.instant());
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
    }

    @Transactional
    public void resendVerification(String email) {
        users.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(u -> issueAndSend(u, Purpose.EMAIL_VERIFICATION));
    }

    /** Sempre responde 202, exista ou não a conta (RF-10). */
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmail(email).filter(User::canLogin).ifPresent(u -> issueAndSend(u, Purpose.PASSWORD_RESET));
    }

    /** Redefinição: link de uso único de 30 min; encerra todas as sessões (RF-10, 12.1). */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);
        OneTimeToken token = consume(rawToken, Purpose.PASSWORD_RESET);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        refreshTokens.revokeAllOf(user.getId(), clock.instant());
        audit.record(user.getId(), "PASSWORD_RESET", "user", user.getPublicId());
    }

    @Transactional
    public void changePassword(UUID userId, String current, String newPassword) {
        User user = users.require(userId);
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        validatePassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokens.revokeAllOf(user.getId(), clock.instant());
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        refreshTokens.revokeAllOf(userId, clock.instant());
    }

    @Transactional
    public int purgeExpiredTokens() {
        Instant now = clock.instant();
        return refreshTokens.deleteExpired(now) + oneTimeTokens.deleteExpired(now);
    }

    public void validatePassword(String password) {
        if (!passwordPolicy.check(password).isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Session newSession(User user, UUID familyId, Instant absoluteExpiry, String userAgent) {
        Instant now = clock.instant();
        String raw = Hashing.randomToken();
        Instant expires = now.plus(properties.jwt().refreshTokenTtl());
        if (expires.isAfter(absoluteExpiry)) {
            expires = absoluteExpiry;
        }
        String agent = userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length()));
        refreshTokens.saveAndFlush(
                new RefreshToken(user, familyId, Hashing.sha256Hex(raw), expires, absoluteExpiry, agent));
        return new Session(tokens.issue(user), raw, expires, user);
    }

    private void issueAndSend(User user, Purpose purpose) {
        String raw = Hashing.randomToken();
        Duration ttl = purpose == Purpose.EMAIL_VERIFICATION
                ? properties.auth().emailVerificationTtl()
                : properties.auth().passwordResetTtl();
        oneTimeTokens.save(new OneTimeToken(
                user, purpose, Hashing.sha256Hex(raw), clock.instant().plus(ttl)));
        Locale locale = AppLocale.fromTag(user.getPreferredLocale()).locale();
        if (purpose == Purpose.EMAIL_VERIFICATION) {
            mail.sendVerification(user.getEmail(), user.getName(), raw, locale);
        } else {
            mail.sendPasswordReset(user.getEmail(), user.getName(), raw, locale);
        }
    }

    private OneTimeToken consume(String rawToken, Purpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        OneTimeToken token = oneTimeTokens
                .findByTokenHashAndPurpose(Hashing.sha256Hex(rawToken), purpose)
                .filter(t -> t.isUsable(clock.instant()))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
        token.setUsedAt(clock.instant());
        return token;
    }

    private static Set<String> loadCommonPasswords() {
        Set<String> set = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("security/common-passwords.txt").getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .forEach(l -> set.add(l.toLowerCase(Locale.ROOT)));
        } catch (IOException e) {
            log.warn("Lista de senhas comuns indisponível: {}", e.getMessage());
        }
        return set;
    }
}
