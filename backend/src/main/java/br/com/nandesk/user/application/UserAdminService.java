package br.com.nandesk.user.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.Masking;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.user.application.UserEvents.UserBlocked;
import br.com.nandesk.user.domain.CollaboratorProfile;
import br.com.nandesk.user.domain.Role;
import br.com.nandesk.user.domain.User;
import br.com.nandesk.user.domain.UserStatus;
import br.com.nandesk.user.infra.CollaboratorProfileRepository;
import br.com.nandesk.user.infra.MarketSubscriptionRepository;
import br.com.nandesk.user.infra.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Usuários e papéis (RF-31) e o ciclo de exclusão de conta com anonimização (RN-20). */
@Service
public class UserAdminService {

    private final UserRepository users;
    private final CollaboratorProfileRepository profiles;
    private final MarketSubscriptionRepository subscriptions;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserAdminService(
            UserRepository users,
            CollaboratorProfileRepository profiles,
            MarketSubscriptionRepository subscriptions,
            AuditService audit,
            ApplicationEventPublisher events,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.subscriptions = subscriptions;
        this.audit = audit;
        this.events = events;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public record AdminUserView(
            UUID publicId,
            Long id,
            String name,
            String emailMasked,
            Role role,
            UserStatus status,
            int reputation,
            Instant createdAt,
            Instant lastLoginAt) {}

    @Transactional(readOnly = true)
    public PageResponse<AdminUserView> search(String q, Role role, int page, int size) {
        String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase() + "%";
        return PageResponse.of(
                users.search(
                        like,
                        role,
                        PageRequest.of(
                                Math.max(0, page),
                                PageResponse.clampSize(size),
                                Sort.by("createdAt").descending())),
                this::view);
    }

    @Transactional(readOnly = true)
    public AdminUserView get(UUID publicId) {
        return view(users.findByPublicId(publicId).orElseThrow(BusinessException::notFound));
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> byIds(List<Long> ids) {
        return users.findAllById(ids).stream().map(this::view).toList();
    }

    @Transactional
    public AdminUserView update(UUID publicId, Role role, UserStatus status, Long adminId) {
        User user = users.findByPublicId(publicId).orElseThrow(BusinessException::notFound);
        if (user.getId().equals(adminId)
                && (role != null && role != user.getRole() || status == UserStatus.SUSPENDED)) {
            throw new BusinessException(ErrorCode.SELF_MODERATION_FORBIDDEN);
        }
        Map<String, Object> before =
                Map.of("role", user.getRole().name(), "status", user.getStatus().name());
        if (role != null) {
            user.setRole(role);
        }
        if (status != null && status != user.getStatus()) {
            if (status != UserStatus.ACTIVE && status != UserStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            user.setStatus(status);
            if (status == UserStatus.SUSPENDED) {
                events.publishEvent(new UserBlocked(user.getId(), "SUSPENDED"));
            }
        }
        audit.record(
                adminId,
                "USER_UPDATED",
                "user",
                user.getPublicId(),
                before,
                Map.of("role", user.getRole().name(), "status", user.getStatus().name()));
        return view(user);
    }

    /** Pedido de exclusão: bloqueia o login na hora e cancela as reservas (RN-20). */
    @Transactional
    public void requestDeletion(Long userId) {
        User user = users.findById(userId).orElseThrow(BusinessException::notFound);
        user.setStatus(UserStatus.DELETION_PENDING);
        user.setDeletionRequestedAt(clock.instant());
        events.publishEvent(new UserBlocked(user.getId(), "DELETION_PENDING"));
        audit.record(user.getId(), "ACCOUNT_DELETION_REQUESTED", "user", user.getPublicId());
    }

    @Transactional(readOnly = true)
    public List<User> pendingDeletion() {
        return users.findAllByStatus(UserStatus.DELETION_PENDING);
    }

    /**
     * Anonimiza os dados pessoais (RN-20): nome, e-mail, telefone, senha e chave PIX. Lançamentos e auditoria
     * permanecem com a referência anonimizada.
     */
    @Transactional
    public void anonymize(Long userId) {
        User user = users.findById(userId).orElseThrow(BusinessException::notFound);
        user.setName("Conta excluída");
        user.setEmail("anon-" + user.getPublicId() + "@nandesk.invalid");
        user.setPhone(null);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.ANONYMIZED);
        profiles.findById(userId).ifPresent(CollaboratorProfileAnonymizer::clear);
        subscriptions.deleteAllByUserId(userId);
        audit.record(null, "ACCOUNT_ANONYMIZED", "user", user.getPublicId());
    }

    private AdminUserView view(User u) {
        int reputation = profiles.findById(u.getId())
                .map(CollaboratorProfile::getReputationScore)
                .orElse(0);
        return new AdminUserView(
                u.getPublicId(),
                u.getId(),
                u.getName(),
                Masking.email(u.getEmail()),
                u.getRole(),
                u.getStatus(),
                reputation,
                u.getCreatedAt(),
                u.getLastLoginAt());
    }

    /** Remove a chave PIX do perfil anonimizado. */
    static final class CollaboratorProfileAnonymizer {
        private CollaboratorProfileAnonymizer() {}

        static void clear(CollaboratorProfile p) {
            p.setPixKeyType(null);
            p.setPixKeyEncrypted(null);
            p.setPixKeyMasked(null);
            p.setPushEnabled(false);
        }
    }
}
