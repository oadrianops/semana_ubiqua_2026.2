package br.com.nandesk.user.application;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.crypto.AesGcmCipher;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.Masking;
import br.com.nandesk.user.domain.CollaboratorProfile;
import br.com.nandesk.user.domain.CollaboratorProfile.PixKeyType;
import br.com.nandesk.user.domain.MarketSubscription;
import br.com.nandesk.user.domain.Role;
import br.com.nandesk.user.domain.User;
import br.com.nandesk.user.domain.UserStatus;
import br.com.nandesk.user.infra.CollaboratorProfileRepository;
import br.com.nandesk.user.infra.MarketSubscriptionRepository;
import br.com.nandesk.user.infra.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contas, perfil do colaborador, chave PIX cifrada e feiras de interesse (RF-08, RF-11). */
@Service
public class UserAccountService {

    private final UserRepository users;
    private final CollaboratorProfileRepository profiles;
    private final MarketSubscriptionRepository subscriptions;
    private final PasswordEncoder passwordEncoder;
    private final AesGcmCipher cipher;
    private final AppProperties properties;
    private final Clock clock;

    public UserAccountService(
            UserRepository users,
            CollaboratorProfileRepository profiles,
            MarketSubscriptionRepository subscriptions,
            PasswordEncoder passwordEncoder,
            AesGcmCipher cipher,
            AppProperties properties,
            Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.subscriptions = subscriptions;
        this.passwordEncoder = passwordEncoder;
        this.cipher = cipher;
        this.properties = properties;
        this.clock = clock;
    }

    public record ProfileView(
            UUID publicId,
            String name,
            String email,
            String phone,
            Role role,
            UserStatus status,
            String preferredLocale,
            boolean emailVerified,
            String pixKeyType,
            String pixKeyMasked,
            int reputationScore,
            boolean pushEnabled,
            Instant createdAt) {}

    /** Cria a conta já com o perfil de colaborador (todo usuário pode executar tarefas). */
    @Transactional
    public User create(String name, String email, String rawPassword, Role role, UserStatus status, String locale) {
        String normalized = User.normalizeEmail(email);
        if (users.existsByEmail(normalized)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        Instant now = clock.instant();
        User user = new User(
                name.trim(),
                normalized,
                passwordEncoder.encode(rawPassword),
                role,
                status,
                locale,
                properties.legal().termsVersion(),
                now);
        if (status == UserStatus.ACTIVE) {
            user.setEmailVerifiedAt(now);
        }
        User saved = users.save(user);
        profiles.save(new CollaboratorProfile(saved, properties.reputation().initialScore()));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(User.normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByPublicId(UUID publicId) {
        return users.findByPublicId(publicId);
    }

    @Transactional(readOnly = true)
    public User require(UUID publicId) {
        return users.findByPublicId(publicId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }

    @Transactional(readOnly = true)
    public User requireById(Long id) {
        return users.findById(id).orElseThrow(BusinessException::notFound);
    }

    @Transactional(readOnly = true)
    public ProfileView profile(UUID publicId) {
        User user = require(publicId);
        return view(user, profileOf(user));
    }

    @Transactional
    public ProfileView updateProfile(UUID publicId, String name, String phone, String locale, Boolean pushEnabled) {
        User user = require(publicId);
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        if (locale != null) {
            user.setPreferredLocale(locale);
        }
        CollaboratorProfile profile = profileOf(user);
        if (pushEnabled != null) {
            profile.setPushEnabled(pushEnabled);
        }
        return view(user, profile);
    }

    /** Chave PIX cifrada com AES-256-GCM e exibida mascarada (RF-11, 13.1). */
    @Transactional
    public ProfileView setPixKey(UUID publicId, PixKeyType type, String key) {
        User user = require(publicId);
        CollaboratorProfile profile = profileOf(user);
        String trimmed = key.trim();
        profile.setPixKeyType(type);
        profile.setPixKeyEncrypted(cipher.encrypt(trimmed));
        profile.setPixKeyMasked(Masking.pixKey(trimmed));
        return view(user, profile);
    }

    /** Chave PIX em claro — só para o admin registrar um repasse, com acesso auditado (RF-27). */
    @Transactional(readOnly = true)
    public Optional<String> revealPixKey(Long userId) {
        return profiles.findById(userId)
                .filter(p -> p.getPixKeyEncrypted() != null)
                .map(p -> cipher.decrypt(p.getPixKeyEncrypted()));
    }

    @Transactional(readOnly = true)
    public CollaboratorProfile profileOf(User user) {
        return profiles.findById(user.getId())
                .orElseGet(() -> profiles.save(
                        new CollaboratorProfile(user, properties.reputation().initialScore())));
    }

    @Transactional(readOnly = true)
    public Optional<CollaboratorProfile> findProfile(Long userId) {
        return profiles.findById(userId);
    }

    @Transactional
    public void setReputation(Long userId, int score) {
        profiles.findById(userId).ifPresent(p -> p.setReputationScore(score));
    }

    @Transactional(readOnly = true)
    public Set<Long> subscribedMarketIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        subscriptions.findAllByUserId(userId).forEach(s -> ids.add(s.getMarketId()));
        return ids;
    }

    @Transactional
    public void replaceSubscriptions(Long userId, Collection<Long> marketIds) {
        User user = requireById(userId);
        Set<Long> desired = new HashSet<>(marketIds);
        List<MarketSubscription> current = subscriptions.findAllByUserId(userId);
        current.stream().filter(s -> !desired.contains(s.getMarketId())).forEach(subscriptions::delete);
        Set<Long> existing = new HashSet<>();
        current.forEach(s -> existing.add(s.getMarketId()));
        desired.stream()
                .filter(id -> !existing.contains(id))
                .forEach(id -> subscriptions.save(new MarketSubscription(user, id)));
    }

    /** Colaboradores ativos inscritos na feira (RF-14). */
    @Transactional(readOnly = true)
    public List<Long> activeSubscriberIds(Long marketId) {
        return subscriptions.findUserIdsByMarket(marketId, UserStatus.ACTIVE);
    }

    public ProfileView view(User user, CollaboratorProfile profile) {
        return new ProfileView(
                user.getPublicId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getPreferredLocale(),
                user.getEmailVerifiedAt() != null,
                profile.getPixKeyType() == null ? null : profile.getPixKeyType().name(),
                profile.getPixKeyMasked(),
                profile.getReputationScore(),
                profile.isPushEnabled(),
                user.getCreatedAt());
    }
}
