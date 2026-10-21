package br.com.nandesk.notification.application;

import br.com.nandesk.notification.domain.Notification;
import br.com.nandesk.notification.domain.NotificationType;
import br.com.nandesk.notification.domain.PushEndpointPolicy;
import br.com.nandesk.notification.domain.PushSubscription;
import br.com.nandesk.notification.infra.NotificationRepository;
import br.com.nandesk.notification.infra.PushSubscriptionRepository;
import br.com.nandesk.notification.infra.WebPushSender;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.config.AsyncConfig;
import br.com.nandesk.shared.crypto.Hashing;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.time.BelemTime;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.PageResponse;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Notificações (RF-14, 9.8): a in-app é gravada na mesma transação do evento e é a fonte da verdade; depois do
 * commit seguem o SSE (app aberto) e o Web Push (melhor esforço), com antispam por feira e horário de silêncio.
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final PushSubscriptionRepository subscriptions;
    private final UserAccountService users;
    private final SseHub sse;
    private final WebPushSender push;
    private final MessageSource messages;
    private final ApplicationEventPublisher events;
    private final JsonMapper json;
    private final AppProperties properties;
    private final Clock clock;
    private final Cache<String, Instant> pushCooldown;
    private final PushEndpointPolicy endpoints;

    public NotificationService(
            NotificationRepository notifications,
            PushSubscriptionRepository subscriptions,
            UserAccountService users,
            SseHub sse,
            WebPushSender push,
            MessageSource messages,
            ApplicationEventPublisher events,
            JsonMapper json,
            AppProperties properties,
            Clock clock) {
        this.notifications = notifications;
        this.subscriptions = subscriptions;
        this.users = users;
        this.sse = sse;
        this.push = push;
        this.messages = messages;
        this.events = events;
        this.json = json;
        this.properties = properties;
        this.clock = clock;
        this.endpoints = new PushEndpointPolicy(properties.push().allowedEndpointPatterns());
        this.pushCooldown = Caffeine.newBuilder()
                .expireAfterWrite(properties.notifications().pushCooldownPerMarket())
                .maximumSize(100_000)
                .build();
    }

    /** Pedido de entrega depois do commit. {@code cooldownKey} não nulo aplica o antispam de push por feira. */
    public record Dispatch(Long userId, UUID notificationId, String cooldownKey) {}

    public record NotificationView(
            UUID publicId,
            NotificationType type,
            String messageKey,
            Map<String, Object> params,
            String title,
            String body,
            String link,
            Instant readAt,
            Instant createdAt) {}

    public record NotificationPage(long unread, PageResponse<NotificationView> page) {}

    @Transactional
    public Notification create(
            Long userId, NotificationType type, Map<String, Object> params, String link, String cooldownKey) {
        Notification n =
                notifications.save(new Notification(userId, type, "notification." + type.name(), params, link));
        events.publishEvent(new Dispatch(userId, n.getPublicId(), cooldownKey));
        return n;
    }

    @Transactional
    public void createForAll(
            Collection<Long> userIds,
            NotificationType type,
            Map<String, Object> params,
            String link,
            String cooldownPrefix) {
        for (Long userId : userIds) {
            create(userId, type, params, link, cooldownPrefix == null ? null : cooldownPrefix + "|" + userId);
        }
    }

    /** Entrega assíncrona depois do commit: SSE e push. */
    @Async(AsyncConfig.SIDE_EFFECTS)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(Dispatch dispatch) {
        Notification n = notifications.findByPublicId(dispatch.notificationId()).orElse(null);
        if (n == null) {
            return;
        }
        User user = users.requireById(dispatch.userId());
        Locale locale = AppLocale.fromTag(user.getPreferredLocale()).locale();
        NotificationView view = view(n, locale);
        sse.send(
                dispatch.userId(),
                Map.of("unread", notifications.countByUserIdAndReadAtIsNull(dispatch.userId()), "latest", view));
        if (!shouldPush(user, dispatch.cooldownKey())) {
            return;
        }
        String payload = json.writeValueAsString(Map.of(
                "title",
                view.title(),
                "body",
                view.body(),
                "url",
                n.getLink() == null ? "/notifications" : n.getLink()));
        for (PushSubscription s : subscriptions.findAllByUserId(user.getId())) {
            switch (push.send(s.getEndpoint(), s.getP256dh(), s.getAuth(), payload)) {
                case SENT -> {
                    s.setLastSuccessAt(clock.instant());
                    s.setFailureCount(0);
                }
                case GONE -> subscriptions.delete(s);
                case FAILED -> s.setFailureCount(s.getFailureCount() + 1);
                case DISABLED -> {
                    return;
                }
            }
        }
    }

    private boolean shouldPush(User user, String cooldownKey) {
        if (!push.enabled()) {
            return false;
        }
        boolean pushEnabled =
                users.findProfile(user.getId()).map(p -> p.isPushEnabled()).orElse(true);
        if (!pushEnabled || isQuietHours()) {
            return false;
        }
        if (cooldownKey != null) {
            if (pushCooldown.getIfPresent(cooldownKey) != null) {
                return false;
            }
            pushCooldown.put(cooldownKey, clock.instant());
        }
        return true;
    }

    /** Horário de silêncio para push (padrão 22h–6h, America/Belem — RF-14). */
    boolean isQuietHours() {
        LocalTime now = LocalTime.now(clock.withZone(BelemTime.ZONE));
        LocalTime start = properties.notifications().quietHoursStart();
        LocalTime end = properties.notifications().quietHoursEnd();
        return start.isAfter(end)
                ? (!now.isBefore(start) || now.isBefore(end))
                : (!now.isBefore(start) && now.isBefore(end));
    }

    @Transactional(readOnly = true)
    public NotificationPage list(UUID userPublicId, int page, int size) {
        User user = users.require(userPublicId);
        Locale locale = LocaleContextHolder.getLocale();
        return new NotificationPage(
                notifications.countByUserIdAndReadAtIsNull(user.getId()),
                PageResponse.of(
                        notifications.findAllByUserIdOrderByCreatedAtDesc(
                                user.getId(), PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                        n -> view(n, locale)));
    }

    @Transactional
    public long markRead(UUID userPublicId, List<UUID> ids, boolean all) {
        User user = users.require(userPublicId);
        if (all) {
            notifications.markAllRead(user.getId(), clock.instant());
        } else if (ids != null && !ids.isEmpty()) {
            notifications.markRead(user.getId(), ids, clock.instant());
        }
        return notifications.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public UUID subscribePush(UUID userPublicId, String endpoint, String p256dh, String auth, String userAgent) {
        if (!endpoints.allows(endpoint)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        User user = users.require(userPublicId);
        String hash = Hashing.sha256Hex(endpoint);
        PushSubscription sub = subscriptions
                .findByEndpointHash(hash)
                .orElseGet(() -> new PushSubscription(user.getId(), endpoint, hash, p256dh, auth, userAgent));
        sub.setUserId(user.getId());
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        sub.setFailureCount(0);
        return subscriptions.save(sub).getPublicId();
    }

    @Transactional
    public void unsubscribePush(UUID userPublicId, UUID subscriptionId) {
        User user = users.require(userPublicId);
        subscriptions
                .findByPublicIdAndUserId(subscriptionId, user.getId())
                .ifPresentOrElse(subscriptions::delete, () -> {
                    throw BusinessException.notFound();
                });
    }

    @Transactional
    public int purgeReadOlderThan(Duration age) {
        return notifications.deleteReadBefore(clock.instant().minus(age));
    }

    @Transactional(readOnly = true)
    public List<Notification> allOf(Long userId) {
        return notifications.findAllByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void deleteAllOf(Long userId) {
        notifications.deleteAll(notifications.findAllByUserIdOrderByCreatedAtAsc(userId));
        subscriptions.deleteAllByUserId(userId);
    }

    private NotificationView view(Notification n, Locale locale) {
        Object[] args = args(n, locale);
        String title = messages.getMessage(n.getMessageKey() + ".title", args, n.getMessageKey(), locale);
        String body = messages.getMessage(n.getMessageKey() + ".body", args, "", locale);
        return new NotificationView(
                n.getPublicId(),
                n.getType(),
                n.getMessageKey(),
                n.getParams(),
                title,
                body,
                n.getLink(),
                n.getReadAt(),
                n.getCreatedAt());
    }

    /** Parâmetros posicionais na ordem esperada pelas mensagens de cada tipo. */
    private static Object[] args(Notification n, Locale locale) {
        Map<String, Object> p = n.getParams() == null ? Map.of() : n.getParams();
        Map<String, Object> ordered = new LinkedHashMap<>();
        switch (n.getType()) {
            case NEW_TASKS -> ordered.put("market", p.get("market"));
            case SUBMISSION_APPROVED, PAYOUT_REGISTERED -> ordered.put("amount", money(p.get("amount"), locale));
            case SUBMISSION_REJECTED -> ordered.put("reason", p.get("reason"));
            case RESERVATION_EXPIRING, RESERVATION_EXPIRED -> ordered.put("market", p.get("market"));
        }
        return ordered.values().toArray();
    }

    /** "R$ 4,00", "R$4.00" ou "4,00 R$", conforme o idioma (RN-24); o valor fica gravado como texto decimal. */
    private static String money(Object amount, Locale locale) {
        if (amount == null) {
            return "";
        }
        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(java.util.Currency.getInstance("BRL"));
        try {
            return format.format(new java.math.BigDecimal(amount.toString()));
        } catch (NumberFormatException e) {
            return amount.toString();
        }
    }
}
