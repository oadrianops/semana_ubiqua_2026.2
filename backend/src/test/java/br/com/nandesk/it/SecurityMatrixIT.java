package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Matriz de segurança (seções 4.2 e 15.2): cada rota protegida × papel. 401 sem sessão, 403 sem o papel, e o
 * papel certo passa da autorização (qualquer status diferente de 401/403).
 */
class SecurityMatrixIT extends IntegrationTest {

    enum Who {
        ANONYMOUS,
        COLLABORATOR,
        MODERATOR,
        ADMIN
    }

    static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/me", Who.COLLABORATOR),
                Arguments.of("GET", "/api/v1/me/ledger", Who.COLLABORATOR),
                Arguments.of("GET", "/api/v1/me/notifications", Who.COLLABORATOR),
                Arguments.of("GET", "/api/v1/tasks/available", Who.COLLABORATOR),
                Arguments.of("GET", "/api/v1/moderation/submissions", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/moderation/reports", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/moderation/product-suggestions", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/admin/tasks", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/admin/stalls", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/admin/markets", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/admin/metrics/overview", Who.MODERATOR),
                Arguments.of("GET", "/api/v1/admin/users", Who.ADMIN),
                Arguments.of("GET", "/api/v1/admin/audit-log", Who.ADMIN),
                Arguments.of("GET", "/api/v1/admin/balances", Who.ADMIN),
                Arguments.of("GET", "/api/v1/admin/payouts", Who.ADMIN),
                Arguments.of("GET", "/api/v1/admin/budgets", Who.ADMIN),
                Arguments.of("GET", "/api/v1/admin/commission-rules", Who.ADMIN));
    }

    private Api client(Who who, Fixtures fx) {
        return switch (who) {
            case ANONYMOUS -> api;
            case COLLABORATOR -> fx.newCollaborator().api();
            case MODERATOR -> fx.moderator();
            case ADMIN -> fx.admin();
        };
    }

    @Tag("RNF-09")
    @ParameterizedTest(name = "{0} {1} exige {2}")
    @MethodSource("routes")
    @DisplayName("RNF-09: autorização por papel em cada rota")
    void matrix(String method, String path, Who minimum) {
        Fixtures fx = new Fixtures(api, mail);
        for (Who who : Who.values()) {
            int status = client(who, fx).get(path).status();
            if (who == Who.ANONYMOUS) {
                assertThat(status).as("%s sem sessão", path).isEqualTo(401);
            } else if (who.ordinal() < minimum.ordinal()) {
                assertThat(status).as("%s como %s", path, who).isEqualTo(403);
            } else {
                assertThat(status).as("%s como %s", path, who).isNotIn(401, 403);
            }
        }
    }

    @Test
    @Tag("RN-13")
    @DisplayName("RN-13 RN-22: dono vê a própria tarefa; outra pessoa não vê a foto restrita nem a reserva alheia")
    void ownership() {
        Fixtures fx = new Fixtures(api, mail);
        Fixtures.Person owner = fx.newCollaborator();
        Fixtures.Person other = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        fx.acceptAndCheckIn(owner.api(), task.publicId());
        String file = fx.uploadPhoto(owner.api(), java.awt.Color.BLUE);
        other.api().get("/api/v1/files/" + file).expect(404);
        owner.api().get("/api/v1/files/" + file).expect(200);
        assertThat(other.api()
                        .post("/api/v1/tasks/" + task.publicId() + "/release", null)
                        .status())
                .isBetween(400, 499);
        assertThat(other.api()
                        .post(
                                "/api/v1/tasks/" + task.publicId() + "/check-in",
                                Map.of(
                                        "latitude",
                                        Fixtures.VOP_LAT,
                                        "longitude",
                                        Fixtures.VOP_LNG,
                                        "accuracyMeters",
                                        10))
                        .status())
                .isBetween(400, 499);
    }

    @Test
    @Tag("RF-14")
    @DisplayName("RF-14: inscrição de push, chave pública e ticket do SSE")
    void notificationsChannels() {
        Fixtures fx = new Fixtures(api, mail);
        Fixtures.Person p = fx.newCollaborator();
        p.api().get("/api/v1/me/push-subscriptions/public-key").expect(200);
        String ticket = p.api()
                .post("/api/v1/me/notifications/stream-ticket", null)
                .expect(200)
                .json()
                .path("ticket")
                .asString();
        assertThat(ticket).isNotBlank();
        api.get("/api/v1/me/notifications/stream?ticket=invalido").expect(401);
        Api.Res sub = p.api()
                .post(
                        "/api/v1/me/push-subscriptions",
                        Map.of(
                                "endpoint",
                                "https://fcm.googleapis.com/fcm/send/teste-" + System.nanoTime(),
                                "keys",
                                Map.of(
                                        "p256dh",
                                        "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlUls0VJXg7A8u-Ts1XbjhazAkj7I99e8QcYP7DkM",
                                        "auth",
                                        "tBHItJI5svbpez7KI4CCXg")));
        assertThat(sub.status()).isIn(200, 201, 503);
        if (sub.status() != 503) {
            p.api()
                    .delete("/api/v1/me/push-subscriptions/"
                            + sub.json().path("id").asString())
                    .expect(204);
        }
    }
}
