package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Painel de moderação e administração (RF-12, RF-13, RF-23 a RF-33). */
class AdministrationIT extends IntegrationTest {

    private Fixtures fx() {
        return new Fixtures(api, mail);
    }

    @Test
    @Tag("RN-13")
    @DisplayName("RF-28 RN-13: cadastro de box novo exige consentimento; aprovado, o box aparece; revogado, sai do ar")
    void newStallWithConsent() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Api mod = fx.moderator();
        JsonNode task = mod.post(
                        "/api/v1/moderation/tasks",
                        Map.of(
                                "type",
                                "NEW_STALL",
                                "marketSlug",
                                "ver-o-peso",
                                "sectorSlug",
                                "ervas-e-banhos",
                                "instructions",
                                "Box novo"))
                .expect(201)
                .json();
        String taskId = task.path("publicId").asString();
        fx.acceptAndCheckIn(p.api(), taskId);
        String photo = fx.uploadPhoto(p.api(), Color.GREEN);
        String code = "N" + (System.nanoTime() % 100000);
        Map<String, Object> noConsent = Map.of(
                "taskType", "NEW_STALL",
                "stall",
                        Map.of(
                                "marketSlug",
                                "ver-o-peso",
                                "code",
                                code,
                                "displayName",
                                "Ervas da Dona Nena",
                                "consent",
                                Map.of("publish", false, "showVendorName", false, "method", "VERBAL")),
                "offerings",
                        List.of(Map.of(
                                "action",
                                "ADD",
                                "productSlug",
                                "priprioca",
                                "referencePrice",
                                Map.of("amount", "6.00", "currency", "BRL"),
                                "priceUnit",
                                "BUNCH",
                                "availability",
                                "AVAILABLE")),
                "photos", List.of(Map.of("fileId", photo, "role", "STALL")));
        assertThat(p.api()
                        .post(
                                "/api/v1/tasks/" + taskId + "/submission",
                                noConsent,
                                "Idempotency-Key",
                                UUID.randomUUID().toString())
                        .code())
                .isEqualTo("CONSENT_REQUIRED");

        Map<String, Object> withConsent = Map.of(
                "taskType", "NEW_STALL",
                "stall",
                        Map.of(
                                "marketSlug",
                                "ver-o-peso",
                                "sectorSlug",
                                "ervas-e-banhos",
                                "code",
                                code,
                                "displayName",
                                "Ervas da Dona Nena",
                                "vendorName",
                                "Nena",
                                "consent",
                                Map.of("publish", true, "showVendorName", true, "method", "SIGNED_FORM")),
                "offerings",
                        List.of(
                                Map.of(
                                        "action",
                                        "ADD",
                                        "productSlug",
                                        "priprioca",
                                        "referencePrice",
                                        Map.of("amount", "6.00", "currency", "BRL"),
                                        "priceUnit",
                                        "BUNCH",
                                        "availability",
                                        "AVAILABLE"),
                                Map.of(
                                        "action",
                                        "ADD_SUGGESTED",
                                        "suggestion",
                                        Map.of("namePt", "Erva-cidreira " + code, "categorySlug", "ervas-e-banhos"),
                                        "priceUnit",
                                        "BUNCH",
                                        "availability",
                                        "AVAILABLE")),
                "photos", List.of(Map.of("fileId", photo, "role", "STALL")));
        String submission = p.api()
                .post(
                        "/api/v1/tasks/" + taskId + "/submission",
                        withConsent,
                        "Idempotency-Key",
                        UUID.randomUUID().toString())
                .expect(201)
                .json()
                .path("submissionId")
                .asString();
        JsonNode review = mod.get("/api/v1/moderation/submissions/" + submission)
                .expect(200)
                .json();
        assertThat(review.path("stallDiff").toString()).contains(code);
        mod.post("/api/v1/moderation/submissions/" + submission + "/approve", Map.of("acceptConflicts", false))
                .expect(204);

        JsonNode found = api.get("/api/v1/search?q=priprioca").expect(200).json();
        assertThat(found.toString()).contains(code);

        JsonNode suggestions =
                mod.get("/api/v1/moderation/product-suggestions").expect(200).json();
        String suggested = null;
        for (JsonNode s : suggestions.path("items")) {
            if (s.toString().contains("Erva-cidreira " + code)) {
                suggested = s.path("slug").asString();
            }
        }
        assertThat(suggested).isNotNull();
        mod.post("/api/v1/moderation/product-suggestions/" + suggested + "/approve", null)
                .expect(200);

        Api admin = fx.admin();
        JsonNode stall = admin.get("/api/v1/admin/stalls?market=ver-o-peso&q=" + code)
                .expect(200)
                .json()
                .path("items")
                .get(0);
        String stallId = stall.path("publicId").asString();
        assertThat(stall.path("consentStatus").asString()).isEqualTo("GRANTED");
        admin.post("/api/v1/admin/stalls/" + stallId + "/consent/revoke", null).expect(200);
        api.get("/api/v1/stalls/" + stallId).expect(404);
        admin.post("/api/v1/admin/stalls/" + stallId + "/consent/grant", Map.of("method", "VERBAL"))
                .expect(200);
        api.get("/api/v1/stalls/" + stallId).expect(200);
    }

    @Test
    @Tag("RF-07")
    @DisplayName("RF-07 RN-16: três avisos sobre o box abrem uma verificação; a moderação pode descartar")
    void reportsOpenVerification() {
        Fixtures fx = fx();
        Api mod = fx.moderator();
        JsonNode stalls = api.get("/api/v1/markets/feira-da-25/stalls?size=50")
                .expect(200)
                .json()
                .path("items");
        String stallId = stalls.get(stalls.size() - 1).path("publicId").asString();
        Map<String, Object> report =
                Map.of("stallId", stallId, "reason", "PRICE_MISMATCH", "comment", "Preço mudou", "website", "");
        api.post("/api/v1/reports", Map.of("stallId", stallId, "reason", "OTHER", "website", "robo"))
                .expect(202);
        for (int i = 0; i < 3; i++) {
            api.post("/api/v1/reports", report).expect(202);
        }
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(mod.get("/api/v1/admin/tasks?market=feira-da-25&size=50")
                                .json()
                                .toString())
                        .contains("VERIFY_REPORT"));
        JsonNode open = mod.get("/api/v1/moderation/reports?status=TASK_CREATED")
                .expect(200)
                .json();
        assertThat(open.path("totalItems").asInt()).isPositive();

        String other = stalls.get(0).path("publicId").asString();
        api.post("/api/v1/reports", Map.of("stallId", other, "reason", "OTHER", "comment", "teste", "website", ""))
                .expect(202);
        JsonNode pending =
                mod.get("/api/v1/moderation/reports?status=OPEN").expect(200).json();
        String reportId = pending.path("items").get(0).path("publicId").asString();
        mod.post("/api/v1/moderation/reports/" + reportId + "/dismiss", null).expect(204);
    }

    @Test
    @Tag("RF-29")
    @DisplayName("RF-29 RF-30 RF-31: feiras, setores, boxes, categorias e produtos com controle de versão")
    void catalogAndMarkets() {
        Api admin = fx().admin();
        JsonNode market =
                admin.get("/api/v1/admin/markets/feira-da-25").expect(200).json();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", market.path("name").asString());
        body.put("address", market.path("address").asString());
        body.put("latitude", -1.4406);
        body.put("longitude", -48.4704);
        body.put("radiusMeters", 300);
        body.put("openingHours", Api.JSON.convertValue(market.path("openingHours"), List.class));
        body.put("active", true);
        body.put("translations", Api.JSON.convertValue(market.path("translations"), Map.class));
        body.put("expectedVersion", market.path("version").asInt());
        JsonNode saved =
                admin.put("/api/v1/admin/markets/feira-da-25", body).expect(200).json();
        assertThat(saved.path("radiusMeters").asInt()).isEqualTo(300);
        assertThat(admin.put("/api/v1/admin/markets/feira-da-25", body).code()).isEqualTo("STALE_VERSION");
        admin.put(
                        "/api/v1/admin/markets/feira-da-25/sectors/peixes",
                        Map.of(
                                "sortOrder",
                                9,
                                "active",
                                true,
                                "names",
                                Map.of("pt-BR", "Peixes", "en", "Fish", "es", "Pescado")))
                .expect(200);
        admin.get("/api/v1/admin/markets").expect(200);

        JsonNode categories = admin.get("/api/v1/admin/categories").expect(200).json();
        JsonNode cat = categories.get(0);
        admin.put(
                        "/api/v1/admin/categories/" + cat.path("slug").asString(),
                        Map.of(
                                "icon",
                                cat.path("icon").asString(),
                                "sortOrder",
                                1,
                                "active",
                                true,
                                "names",
                                Api.JSON.convertValue(cat.path("names"), Map.class)))
                .expect(200);

        JsonNode product = admin.get("/api/v1/admin/products/jambu").expect(200).json();
        assertThat(product.path("completeness").path("en").asBoolean()).isTrue();
        Map<String, Object> productBody = new java.util.HashMap<>();
        productBody.put("categorySlug", product.path("categorySlug").asString());
        productBody.put("defaultUnit", product.path("defaultUnit").asString());
        productBody.put("seasonMonths", List.of(6, 7, 8));
        productBody.put("status", "ACTIVE");
        productBody.put("translations", Api.JSON.convertValue(product.path("translations"), Map.class));
        productBody.put(
                "aliases", Map.of("pt-BR", List.of("agrião-do-pará"), "en", List.of("paracress"), "es", List.of()));
        productBody.put("expectedVersion", product.path("version").asInt());
        admin.put("/api/v1/admin/products/jambu", productBody).expect(200);
        assertThat(api.get("/api/v1/search?q=agriao%20do%20para").json().toString())
                .contains("jambu");
        assertThat(admin.get("/api/v1/admin/products?status=ACTIVE&q=jam")
                        .expect(200)
                        .json()
                        .path("totalItems")
                        .asInt())
                .isPositive();
    }

    @Test
    @Tag("RF-25")
    @DisplayName(
            "RF-25 RN-10: regra de comissão nova, orçamento esgotado deixa a tarefa em espera e ela pode sair como voluntária")
    void commissionsAndBudget() {
        Fixtures fx = fx();
        Api admin = fx.admin();
        admin.post(
                        "/api/v1/admin/commission-rules",
                        Map.of("taskType", "VERIFY_REPORT", "amount", Map.of("amount", "3.50", "currency", "BRL")))
                .expect(201);
        assertThat(admin.get("/api/v1/admin/commission-rules")
                        .expect(200)
                        .json()
                        .toString())
                .contains("3.50");

        String period = admin.get("/api/v1/admin/budgets")
                .expect(200)
                .json()
                .path("overview")
                .path("period")
                .asString();
        admin.put(
                        "/api/v1/admin/budgets",
                        Map.of(
                                "period",
                                period,
                                "sponsorName",
                                "Demonstração NANDESK",
                                "amount",
                                Map.of("amount", "0.00", "currency", "BRL")))
                .expect(200);
        try {
            Api mod = fx.moderator();
            JsonNode stalls = api.get("/api/v1/markets/feira-da-25/stalls?size=50")
                    .expect(200)
                    .json()
                    .path("items");
            String onHold = null;
            for (JsonNode stall : stalls) {
                Api.Res created = mod.post(
                        "/api/v1/moderation/tasks",
                        Map.of(
                                "type",
                                "UPDATE_STALL",
                                "marketSlug",
                                "feira-da-25",
                                "stallPublicId",
                                stall.path("publicId").asString()));
                if (created.status() == 201) {
                    onHold = created.json().path("publicId").asString();
                    assertThat(created.json().path("status").asString()).isEqualTo("ON_HOLD");
                    break;
                }
            }
            assertThat(onHold).as("box livre na Feira da 25").isNotNull();
            assertThat(mod.post("/api/v1/moderation/tasks/" + onHold + "/publish", Map.of("volunteer", false))
                            .code())
                    .isEqualTo("BUDGET_EXHAUSTED");
            mod.post("/api/v1/moderation/tasks/" + onHold + "/publish", Map.of("volunteer", true))
                    .expect(204);
            assertThat(mod.get("/api/v1/tasks/" + onHold).status()).isIn(200, 403);
        } finally {
            fx.ensureBudget();
        }

        JsonNode metrics =
                admin.get("/api/v1/admin/metrics/overview").expect(200).json();
        assertThat(metrics.path("publishedStalls").asInt()).isPositive();
        assertThat(metrics.path("budget").path("hasBudget").asBoolean()).isTrue();
        api.post(
                        "/api/v1/telemetry/web-vitals",
                        Map.of(
                                "metrics",
                                List.of(Map.of("name", "LCP", "value", 1800.0, "route", "/", "connection", "4g"))))
                .expect(202);
    }

    @Test
    @Tag("RF-12")
    @DisplayName(
            "RF-12 RNF-10: administração troca papel, suspende (tarefas voltam para a lista) e tudo fica na auditoria")
    void usersAndAudit() {
        Fixtures fx = fx();
        Fixtures.Person p = fx.newCollaborator();
        Fixtures.Task task = fx.openUpdateTask();
        p.api().post("/api/v1/tasks/" + task.publicId() + "/accept", null).expect(200);

        Api admin = fx.admin();
        JsonNode users = admin.get("/api/v1/admin/users?q=" + p.email().substring(0, 10))
                .expect(200)
                .json();
        assertThat(users.path("items").get(0).path("emailMasked").asString()).doesNotContain(p.email());
        admin.patch("/api/v1/admin/users/" + p.publicId(), Map.of("status", "SUSPENDED"))
                .expect(200);
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(fx.moderator()
                                .get("/api/v1/admin/tasks?status=OPEN&size=50")
                                .json()
                                .toString())
                        .contains(task.publicId()));
        assertThat(api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", p.password()))
                        .code())
                .isEqualTo("ACCOUNT_SUSPENDED");

        admin.patch("/api/v1/admin/users/" + p.publicId(), Map.of("status", "ACTIVE", "role", "MODERATOR"))
                .expect(200);
        JsonNode audit =
                admin.get("/api/v1/admin/audit-log?entityType=user").expect(200).json();
        assertThat(audit.path("totalItems").asInt()).isPositive();
    }
}
