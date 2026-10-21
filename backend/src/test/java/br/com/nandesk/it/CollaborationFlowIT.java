package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nandesk.task.application.TaskJobs;
import java.awt.Color;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * Do aceite ao crédito (RF-15 a RF-28, CA-2): reserva, check-in na cerca, envio idempotente, moderação com
 * conflitos, extrato e repasse. Inclui a corrida da RN-01 e os prazos da RN-02 com o relógio de teste.
 */
class CollaborationFlowIT extends IntegrationTest {

    @Autowired
    TaskJobs jobs;

    private Fixtures fx() {
        return new Fixtures(api, mail);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    @Tag("CA-2")
    @DisplayName("CA-2 RF-16 a RF-22 RF-26: aceita, faz check-in, envia com foto, é aprovado e recebe a comissão")
    void fullCycle() {
        Fixtures fx = fx();
        Fixtures.Person ana = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();

        JsonNode detail =
                ana.api().get("/api/v1/tasks/" + task.publicId()).expect(200).json();
        assertThat(detail.path("status").asString()).isEqualTo("OPEN");
        BigDecimal commission =
                new BigDecimal(detail.path("commission").path("amount").asString());

        String submission = fx.submittedTask(ana, task, "33.50");
        Api mod = fx.moderator();
        JsonNode queue = mod.get("/api/v1/moderation/submissions?market=ver-o-peso")
                .expect(200)
                .json();
        assertThat(queue.toString()).contains(submission);

        JsonNode review = mod.get("/api/v1/moderation/submissions/" + submission)
                .expect(200)
                .json();
        assertThat(review.path("ownSubmission").asBoolean()).isFalse();
        assertThat(review.path("checkIn").path("distanceMeters").asInt()).isLessThan(250);
        String photoUrl = review.path("photos").get(0).path("url").asString();
        api.get(photoUrl).expect(404);
        mod.get(photoUrl).expect(200);
        ana.api().get(photoUrl).expect(200);

        mod.post("/api/v1/moderation/submissions/" + submission + "/approve", Map.of("acceptConflicts", false))
                .expect(204);

        JsonNode ledger = ana.api().get("/api/v1/me/ledger").expect(200).json();
        assertThat(new BigDecimal(ledger.path("balance").path("amount").asString()))
                .isEqualByComparingTo(commission);
        JsonNode stall = api.get("/api/v1/stalls/" + task.stallId()).expect(200).json();
        assertThat(stall.path("offerings").toString()).contains("33.50");

        JsonNode history = ana.api().get("/api/v1/me/tasks").expect(200).json();
        assertThat(history.get(0).path("submissionStatus").asString()).isEqualTo("APPROVED");
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(ana.api()
                                .get("/api/v1/me/notifications")
                                .json()
                                .path("unread")
                                .asInt())
                        .isPositive());
        ana.api().post("/api/v1/me/notifications/read", Map.of("all", true)).expect(200);
        assertThat(ana.api()
                        .get("/api/v1/me/notifications")
                        .json()
                        .path("unread")
                        .asInt())
                .isZero();
    }

    @Test
    @Tag("RN-01")
    @DisplayName("RN-01: duas pessoas aceitam a mesma tarefa ao mesmo tempo → um sucesso e um TASK_ALREADY_TAKEN")
    void concurrentAccept() throws Exception {
        Fixtures fx = fx();
        Fixtures.Person a = fx.newCollaborator();
        Fixtures.Person b = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Api.Res>> results = new ArrayList<>();
        for (Fixtures.Person p : List.of(a, b)) {
            Callable<Api.Res> call = () -> {
                start.await();
                return p.api().post("/api/v1/tasks/" + task.publicId() + "/accept", null);
            };
            results.add(pool.submit(call));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (Future<Api.Res> r : results) {
            statuses.add(r.get().status());
            codes.add(r.get().code());
        }
        pool.shutdown();
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(codes).contains("TASK_ALREADY_TAKEN");
    }

    @Test
    @Tag("RN-05")
    @DisplayName("RN-05: check-in a 2 km é recusado com a distância; GPS impreciso passa com alerta")
    void geofence() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        p.api().post("/api/v1/tasks/" + task.publicId() + "/accept", null).expect(200);
        assertThat(p.api()
                        .post(
                                "/api/v1/tasks/" + task.publicId() + "/submission",
                                Map.of("taskType", "UPDATE_STALL", "offerings", List.of(), "photos", List.of()),
                                "Idempotency-Key",
                                UUID.randomUUID().toString())
                        .code())
                .isEqualTo("CHECKIN_REQUIRED");
        Api.Res far = p.api()
                .post(
                        "/api/v1/tasks/" + task.publicId() + "/check-in",
                        Map.of(
                                "latitude",
                                Fixtures.VOP_LAT + 0.018,
                                "longitude",
                                Fixtures.VOP_LNG,
                                "accuracyMeters",
                                10));
        assertThat(far.code()).isEqualTo("CHECKIN_OUT_OF_RANGE");
        assertThat(far.json().path("distanceMeters").asInt()).isBetween(1900, 2100);

        JsonNode lowAccuracy = p.api()
                .post(
                        "/api/v1/tasks/" + task.publicId() + "/check-in",
                        Map.of(
                                "latitude",
                                Fixtures.VOP_LAT + 0.0025,
                                "longitude",
                                Fixtures.VOP_LNG,
                                "accuracyMeters",
                                150))
                .json();
        assertThat(lowAccuracy.path("status").asString()).isIn("CHECKED_IN", "");
        p.api().post("/api/v1/tasks/" + task.publicId() + "/release", null).expect(204);
        assertThat(api.as(fx.login(p.email(), p.password()))
                        .get("/api/v1/tasks/" + task.publicId())
                        .json()
                        .path("status")
                        .asString())
                .isEqualTo("OPEN");
    }

    @Test
    @Tag("RN-02")
    @DisplayName("RN-02 RN-17: sem check-in em 2 h a reserva vence, a tarefa volta e a reputação cai")
    void reservationExpires() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        p.api().post("/api/v1/tasks/" + task.publicId() + "/accept", null).expect(200);
        int before = p.api().get("/api/v1/me/reputation").json().path("score").asInt();

        clock.advance(Duration.ofMinutes(106));
        jobs.expireReservations();
        clock.advance(Duration.ofMinutes(20));
        jobs.expireReservations();

        JsonNode history = p.api().get("/api/v1/me/tasks").expect(200).json();
        assertThat(history.get(0).path("outcome").asString()).isEqualTo("EXPIRED");
        assertThat(api.get("/api/v1/tasks/" + task.publicId()).status()).isEqualTo(401);
        assertThat(p.api()
                        .get("/api/v1/tasks/" + task.publicId())
                        .json()
                        .path("status")
                        .asString())
                .isEqualTo("OPEN");
        // RN-17: a expiração entra na conta; com menos resultados que o mínimo, a nota segue a inicial.
        JsonNode reputation = p.api().get("/api/v1/me/reputation").json();
        assertThat(reputation.path("expired").asInt()).isEqualTo(1);
        assertThat(reputation.path("score").asInt()).isEqualTo(before);
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(
                                p.api().get("/api/v1/me/notifications").json().toString())
                        .contains("RESERVATION"));
    }

    @Test
    @Tag("RN-27")
    @DisplayName(
            "RN-27 RF-18: foto e envio repetidos com a mesma chave não duplicam; outra chave com outro conteúdo é recusada")
    void idempotency() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        fx.acceptAndCheckIn(p.api(), task.publicId());

        String key = UUID.randomUUID().toString();
        byte[] photo = Fixtures.png(Color.ORANGE);
        String first = p.api()
                .upload("/api/v1/uploads", photo, "image/png", key)
                .expect(201)
                .json()
                .path("fileId")
                .asString();
        String again = p.api()
                .upload("/api/v1/uploads", photo, "image/png", key)
                .expect(201)
                .json()
                .path("fileId")
                .asString();
        assertThat(again).isEqualTo(first);
        assertThat(p.api()
                        .upload(
                                "/api/v1/uploads",
                                "nao-e-imagem".getBytes(),
                                "image/png",
                                UUID.randomUUID().toString())
                        .code())
                .isEqualTo("INVALID_PHOTO");
        p.api().upload("/api/v1/uploads", photo, "image/png", "nao-e-uuid").expect(400);

        Map<String, Object> body = fx.confirmAll(task, first, null);
        String submissionKey = UUID.randomUUID().toString();
        JsonNode one = p.api()
                .post("/api/v1/tasks/" + task.publicId() + "/submission", body, "Idempotency-Key", submissionKey)
                .expect(201)
                .json();
        JsonNode two = p.api()
                .post("/api/v1/tasks/" + task.publicId() + "/submission", body, "Idempotency-Key", submissionKey)
                .expect(201)
                .json();
        assertThat(two.path("submissionId").asString())
                .isEqualTo(one.path("submissionId").asString());

        Map<String, Object> changed = fx.confirmAll(task, first, "99.00");
        assertThat(p.api()
                        .post(
                                "/api/v1/tasks/" + task.publicId() + "/submission",
                                changed,
                                "Idempotency-Key",
                                submissionKey)
                        .code())
                .isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    @Tag("RN-14")
    @DisplayName("RN-14 RN-11: conflito de versão pede confirmação; quem envia não modera; recusa reabre a tarefa")
    void moderationRules() {
        Fixtures fx = fx();
        Fixtures.Person a = fx.newCollaborator();
        Fixtures.Person b = fx.newCollaborator();
        Fixtures.Task t1 = fx.openUpdateTask();
        String s1 = fx.submittedTask(a, t1, "11.00");

        Api mod = fx.moderator();
        mod.post(
                        "/api/v1/moderation/submissions/" + s1 + "/reject",
                        Map.of("reason", "ILLEGIBLE_PHOTO", "comment", "A foto saiu tremida.", "reopenTask", true))
                .expect(204);
        JsonNode rejected = a.api().get("/api/v1/me/tasks").json().get(0);
        assertThat(rejected.path("submissionStatus").asString()).isEqualTo("REJECTED");
        assertThat(rejected.path("rejectionReason").asString()).isEqualTo("ILLEGIBLE_PHOTO");
        assertThat(rejected.path("reviewComment").asString()).isEqualTo("A foto saiu tremida.");
        assertThat(mod.get("/api/v1/tasks/" + t1.publicId()).status()).isIn(200, 403);

        // O box muda depois do envio (versão nova): aprovar exige confirmar o conflito.
        String s2 = fx.submittedTask(b, t1, "12.00");
        Api admin = fx.admin();
        JsonNode stall = admin.get("/api/v1/admin/stalls?market=ver-o-peso&q=" + t1.stallCode())
                .expect(200)
                .json()
                .path("items")
                .get(0);
        admin.patch(
                        "/api/v1/admin/stalls/" + t1.stallId(),
                        Map.of(
                                "displayName",
                                "Box renomeado",
                                "showVendorName",
                                false,
                                "status",
                                "ACTIVE",
                                "version",
                                stall.path("version").asInt()))
                .expect(200);
        assertThat(mod.get("/api/v1/moderation/submissions/" + s2)
                        .json()
                        .path("conflicts")
                        .size())
                .isPositive();
        Api.Res withoutConfirm =
                mod.post("/api/v1/moderation/submissions/" + s2 + "/approve", Map.of("acceptConflicts", false));
        assertThat(withoutConfirm.code()).isEqualTo("SUBMISSION_CONFLICT");
        mod.post("/api/v1/moderation/submissions/" + s2 + "/approve", Map.of("acceptConflicts", true))
                .expect(204);
        assertThat(mod.post("/api/v1/moderation/submissions/" + s2 + "/approve", Map.of("acceptConflicts", true))
                        .status())
                .isBetween(400, 499);
    }

    @Test
    @Tag("RF-27")
    @DisplayName("RF-27 RN-09: repasse registrado pela administração baixa o saldo; abaixo do mínimo é recusado")
    void payout() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        p.api()
                .put(
                        "/api/v1/me/pix-key",
                        Map.of("type", "RANDOM", "key", UUID.randomUUID().toString()))
                .expect(200);
        Api admin = fx.admin();
        for (int i = 0; i < 3; i++) {
            Fixtures.Task task = fx.openUpdateTask();
            String s = fx.submittedTask(p, task, null);
            fx.moderator()
                    .post("/api/v1/moderation/submissions/" + s + "/approve", Map.of("acceptConflicts", true))
                    .expect(204);
        }
        BigDecimal balance = new BigDecimal(p.api()
                .get("/api/v1/me/ledger")
                .json()
                .path("balance")
                .path("amount")
                .asString());
        assertThat(balance).isGreaterThanOrEqualTo(new BigDecimal("10.00"));

        assertThat(admin.get("/api/v1/admin/balances").expect(200).json().toString())
                .contains(p.publicId());
        JsonNode pix = admin.get("/api/v1/admin/collaborators/" + p.publicId() + "/pix-key")
                .expect(200)
                .json();
        assertThat(pix.path("type").asString()).isEqualTo("RANDOM");

        assertThat(admin.post(
                                "/api/v1/admin/payouts",
                                Map.of(
                                        "collaboratorId", p.publicId(),
                                        "amount", Map.of("amount", "5.00", "currency", "BRL"),
                                        "method", "PIX"))
                        .code())
                .isEqualTo("PAYOUT_BELOW_MINIMUM");
        assertThat(admin.post(
                                "/api/v1/admin/payouts",
                                Map.of(
                                        "collaboratorId", p.publicId(),
                                        "amount",
                                                Map.of(
                                                        "amount",
                                                        balance.add(BigDecimal.TEN)
                                                                .toPlainString(),
                                                        "currency",
                                                        "BRL"),
                                        "method", "PIX"))
                        .code())
                .isEqualTo("INSUFFICIENT_BALANCE");
        admin.post(
                        "/api/v1/admin/payouts",
                        Map.of(
                                "collaboratorId",
                                p.publicId(),
                                "amount",
                                Map.of("amount", balance.toPlainString(), "currency", "BRL"),
                                "method",
                                "PIX",
                                "externalReference",
                                "E2E-PIX-1"))
                .expect(201);
        JsonNode after = p.api().get("/api/v1/me/ledger").json();
        assertThat(new BigDecimal(after.path("balance").path("amount").asString()))
                .isEqualByComparingTo("0.00");
        assertThat(after.path("totalPaidOut").path("amount").asString()).isEqualTo(balance.toPlainString());
        assertThat(admin.get("/api/v1/admin/payouts")
                        .expect(200)
                        .json()
                        .path("totalItems")
                        .asInt())
                .isPositive();
        admin.get("/api/v1/admin/collaborators/" + p.publicId() + "/ledger").expect(200);
    }
}
