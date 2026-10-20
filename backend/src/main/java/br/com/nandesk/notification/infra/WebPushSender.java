package br.com.nandesk.notification.infra;

import br.com.nandesk.shared.config.AppProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envia uma mensagem Web Push ao serviço de push do navegador (9.8). "Melhor esforço": falha não afeta a
 * notificação in-app. Respostas 404/410 indicam inscrição vencida.
 */
@Component
public class WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    public enum Result {
        SENT,
        GONE,
        FAILED,
        DISABLED
    }

    private final AppProperties.Push config;
    private final Clock clock;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final WebPushCrypto.Vapid vapid;

    public WebPushSender(AppProperties properties, Clock clock) {
        this.config = properties.push();
        this.clock = clock;
        WebPushCrypto.Vapid keys = null;
        if (config.configured()) {
            try {
                keys = WebPushCrypto.Vapid.of(config.vapidPublicKey(), config.vapidPrivateKey());
            } catch (Exception e) {
                log.warn("Chaves VAPID inválidas: push desligado ({})", e.getMessage());
            }
        }
        this.vapid = keys;
    }

    public boolean enabled() {
        return vapid != null;
    }

    public Result send(String endpoint, String p256dh, String auth, String payloadJson) {
        if (vapid == null) {
            return Result.DISABLED;
        }
        try {
            URI uri = URI.create(endpoint);
            String audience = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            long exp = clock.instant().plus(Duration.ofHours(12)).getEpochSecond();
            String jwt = vapid.jwt(audience, config.vapidSubject(), exp);
            byte[] body = WebPushCrypto.encrypt(
                    payloadJson.getBytes(StandardCharsets.UTF_8),
                    Base64.getUrlDecoder().decode(p256dh),
                    Base64.getUrlDecoder().decode(auth));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "vapid t=" + jwt + ", k=" + vapid.publicKeyB64())
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", "86400")
                    .header("Urgency", "normal")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status == 404 || status == 410) {
                return Result.GONE;
            }
            return status >= 200 && status < 300 ? Result.SENT : Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        } catch (Exception e) {
            log.debug("Falha no push: {}", e.getMessage());
            return Result.FAILED;
        }
    }
}
