package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nandesk.lgpd.application.RetentionJobs;
import br.com.nandesk.notification.application.SseHub;
import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** Notificações em tempo real e Web Push (RF-14, ADR-0010), retenção e anonimização (RN-20 a RN-22, RN-27). */
class NotificationsAndPrivacyIT extends IntegrationTest {

    @Autowired
    RetentionJobs retention;

    @Autowired
    SseHub sse;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private Fixtures fx() {
        return new Fixtures(api, mail);
    }

    /** Chaves do navegador (P-256 não comprimida + 16 bytes de auth), como o PushManager entrega. */
    private static Map<String, String> browserKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        ECPublicKey pub = (ECPublicKey) gen.generateKeyPair().getPublic();
        byte[] x = unsigned(pub.getW().getAffineX().toByteArray());
        byte[] y = unsigned(pub.getW().getAffineY().toByteArray());
        byte[] point = new byte[65];
        point[0] = 4;
        System.arraycopy(x, 0, point, 33 - x.length, x.length);
        System.arraycopy(y, 0, point, 65 - y.length, y.length);
        byte[] auth = new byte[16];
        new SecureRandom().nextBytes(auth);
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return Map.of("p256dh", b64.encodeToString(point), "auth", b64.encodeToString(auth));
    }

    private static byte[] unsigned(byte[] value) {
        if (value.length > 32) {
            byte[] trimmed = new byte[32];
            System.arraycopy(value, value.length - 32, trimmed, 0, 32);
            return trimmed;
        }
        return value;
    }

    @Test
    @Tag("RF-14")
    @DisplayName(
            "RF-14: tarefa nova na feira de interesse chega por SSE e por Web Push cifrado; inscrição vencida é removida")
    void pushAndSse() throws Exception {
        List<String> received = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/push/", exchange -> {
            received.add(exchange.getRequestURI().getPath() + " "
                    + exchange.getRequestHeaders().getFirst("Content-Encoding") + " "
                    + exchange.getRequestHeaders().getFirst("Authorization").startsWith("vapid t="));
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().endsWith("gone") ? 410 : 201, -1);
            exchange.close();
        });
        server.start();
        try {
            Fixtures fx = fx();
            Fixtures.Person p = fx.newCollaborator();
            String base = "http://localhost:" + server.getAddress().getPort();
            p.api()
                    .post("/api/v1/me/push-subscriptions", Map.of("endpoint", base + "/push/ok", "keys", browserKeys()))
                    .expect(201);
            p.api()
                    .post(
                            "/api/v1/me/push-subscriptions",
                            Map.of("endpoint", base + "/push/gone", "keys", browserKeys()))
                    .expect(201);
            assertThat(p.api()
                            .post(
                                    "/api/v1/me/push-subscriptions",
                                    Map.of("endpoint", "https://api:8091/actuator", "keys", browserKeys()))
                            .code())
                    .isEqualTo("VALIDATION_ERROR");

            String ticket = p.api()
                    .post("/api/v1/me/notifications/stream-ticket", null)
                    .expect(200)
                    .json()
                    .path("ticket")
                    .asString();
            HttpResponse<Stream<String>> stream = HttpClient.newHttpClient()
                    .send(
                            HttpRequest.newBuilder(URI.create("http://localhost:" + port
                                            + "/api/v1/me/notifications/stream?ticket=" + ticket))
                                    .header("Accept", "text/event-stream")
                                    .build(),
                            HttpResponse.BodyHandlers.ofLines());
            assertThat(stream.statusCode()).isEqualTo(200);
            assertThat(sse.openConnections()).isPositive();
            sse.heartbeat();
            // O ticket é de uso único.
            api.get("/api/v1/me/notifications/stream?ticket=" + ticket).expect(401);

            fx.openUpdateTask();
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> assertThat(received).anyMatch(r -> r.startsWith("/push/ok aes128gcm true")));
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .untilAsserted(() -> assertThat(received).anyMatch(r -> r.startsWith("/push/gone")));
            assertThat(stream.body().limit(20).anyMatch(line -> line.contains("notification")))
                    .isTrue();
            assertThat(p.api()
                            .get("/api/v1/me/notifications")
                            .json()
                            .path("unread")
                            .asInt())
                    .isPositive();
        } finally {
            server.stop(0);
        }
    }

    @Test
    @Tag("RN-20")
    @DisplayName("RN-20: pedido de exclusão sem saldo anonimiza a conta no job; a exportação traz histórico e extrato")
    void anonymization() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        String submission = fx.submittedTask(p, task, null);
        fx.moderator()
                .post(
                        "/api/v1/moderation/submissions/" + submission + "/reject",
                        Map.of("reason", "OUTDATED", "comment", "teste", "reopenTask", true))
                .expect(204);
        JsonNode export = p.api().get("/api/v1/me/export").expect(200).json();
        assertThat(export.toString()).contains(p.email()).contains("REJECTED");

        p.api().post("/api/v1/me/deletion-request", null).expect(202);
        retention.anonymization();
        JsonNode users =
                fx.admin().get("/api/v1/admin/users?q=" + p.email()).expect(200).json();
        assertThat(users.path("items").size()).isZero();
        assertThat(api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", p.password()))
                        .status())
                .isEqualTo(401);
    }

    @Test
    @Tag("RN-22")
    @DisplayName("RN-21 RN-22 RN-27: retenção apaga coordenadas, fotos e uploads temporários vencidos")
    void retentionJobs() throws IOException {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        fx.acceptAndCheckIn(p.api(), task.publicId());
        String temp = fx.uploadPhoto(p.api(), Color.MAGENTA);
        p.api().get("/api/v1/files/" + temp).expect(200);
        p.api().get("/api/v1/files/" + temp + "?variant=thumb").expect(200);

        clock.advance(Duration.ofDays(200));
        retention.orphanUploads();
        retention.retention();
        retention.anonymization();
        p.api().get("/api/v1/files/" + temp).expect(404);
    }
}
