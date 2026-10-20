package br.com.nandesk.notification.api;

import br.com.nandesk.notification.application.NotificationService;
import br.com.nandesk.notification.application.NotificationService.NotificationPage;
import br.com.nandesk.notification.application.SseHub;
import br.com.nandesk.notification.application.StreamTickets;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Notificações in-app, stream SSE e inscrições de push (RF-14, 9.8). */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Notificações")
public class NotificationController {

    private final NotificationService notifications;
    private final StreamTickets tickets;
    private final SseHub sse;
    private final UserAccountService users;
    private final AppProperties properties;

    public NotificationController(
            NotificationService notifications,
            StreamTickets tickets,
            SseHub sse,
            UserAccountService users,
            AppProperties properties) {
        this.notifications = notifications;
        this.tickets = tickets;
        this.sse = sse;
        this.users = users;
        this.properties = properties;
    }

    public record ReadRequest(List<UUID> ids, boolean all) {}

    public record PushKeys(
            @NotBlank @Size(max = 255) String p256dh,
            @NotBlank @Size(max = 255) String auth) {}

    public record PushSubscriptionRequest(
            @NotBlank @Size(max = 2048) String endpoint,
            @Valid PushKeys keys) {}

    @Operation(summary = "Minhas notificações (RF-14)")
    @GetMapping("/notifications")
    ResponseEntity<NotificationPage> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(notifications.list(CurrentUser.require().publicId(), page, size));
    }

    @Operation(summary = "Marcar como lidas")
    @PostMapping("/notifications/read")
    ResponseEntity<Map<String, Long>> read(@RequestBody ReadRequest body) {
        long unread = notifications.markRead(CurrentUser.require().publicId(), body.ids(), body.all());
        return ResponseEntity.ok(Map.of("unread", unread));
    }

    @Operation(summary = "Ticket de uso único para o stream SSE (válido por 60 s)")
    @PostMapping("/notifications/stream-ticket")
    ResponseEntity<Map<String, String>> ticket() {
        Long userId = users.require(CurrentUser.require().publicId()).getId();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("ticket", tickets.issue(userId)));
    }

    @Operation(summary = "Stream SSE das notificações")
    @GetMapping(path = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@RequestParam String ticket) {
        Long userId = tickets.redeem(ticket).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        return sse.open(userId);
    }

    @Operation(summary = "Chave pública VAPID para inscrever o push")
    @GetMapping("/push-subscriptions/public-key")
    ResponseEntity<Map<String, Object>> publicKey() {
        String key = properties.push().vapidPublicKey();
        return ResponseEntity.ok(
                Map.of("enabled", properties.push().configured(), "publicKey", key == null ? "" : key));
    }

    @Operation(summary = "Registrar inscrição de push (9.8)")
    @PostMapping("/push-subscriptions")
    ResponseEntity<Map<String, UUID>> subscribe(
            @Valid @RequestBody PushSubscriptionRequest body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        UUID id = notifications.subscribePush(
                CurrentUser.require().publicId(),
                body.endpoint(),
                body.keys().p256dh(),
                body.keys().auth(),
                userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length())));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @Operation(summary = "Remover inscrição de push")
    @DeleteMapping("/push-subscriptions/{id}")
    ResponseEntity<Void> unsubscribe(@PathVariable UUID id) {
        notifications.unsubscribePush(CurrentUser.require().publicId(), id);
        return ResponseEntity.noContent().build();
    }
}
