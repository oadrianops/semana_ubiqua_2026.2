package br.com.nandesk.it;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import tools.jackson.databind.JsonNode;

/** Cenários reutilizáveis dos *IT: contas do seed, colaboradores novos, tarefas e fotos de teste. */
public final class Fixtures {

    public static final String SEED_PASSWORD = "senha-de-teste-123";
    public static final double VOP_LAT = -1.4525;
    public static final double VOP_LNG = -48.5035;
    private static final AtomicInteger SEQ = new AtomicInteger();

    private final Api api;
    private final CapturedMail mail;

    public Fixtures(Api api, CapturedMail mail) {
        this.api = api;
        this.mail = mail;
    }

    public String login(String email, String password) {
        return api.post("/api/v1/auth/login", Map.of("email", email, "password", password))
                .expect(200)
                .json()
                .path("accessToken")
                .asString();
    }

    public Api admin() {
        return api.as(login("admin@nandesk.local", SEED_PASSWORD));
    }

    public Api moderator() {
        return api.as(login("moderador@nandesk.local", SEED_PASSWORD));
    }

    public record Person(String email, String password, Api api, String publicId) {}

    /** Cadastro + confirmação pelo link do e-mail + login (RF-08, RF-10). */
    public Person newCollaborator() {
        String email = "colab" + SEQ.incrementAndGet() + "-"
                + UUID.randomUUID().toString().substring(0, 8) + "@teste.nandesk.com.br";
        String password = "Feira-" + UUID.randomUUID();
        api.post(
                        "/api/v1/auth/register",
                        Map.of(
                                "name",
                                "Pessoa de Teste",
                                "email",
                                email,
                                "password",
                                password,
                                "locale",
                                "pt-BR",
                                "acceptTerms",
                                true,
                                "adultDeclaration",
                                true))
                .expect(201);
        String token = mail.tokenSentTo(email, "confirm");
        api.post("/api/v1/auth/verify-email", Map.of("token", token)).expect(204);
        Api session = api.as(login(email, password));
        String id =
                session.get("/api/v1/me").expect(200).json().path("publicId").asString();
        session.put("/api/v1/me/market-subscriptions", Map.of("markets", List.of("ver-o-peso")))
                .expect(200);
        return new Person(email, password, session, id);
    }

    public record Task(String publicId, String stallId, String stallCode, int stallVersion) {}

    /**
     * Tarefa UPDATE_STALL aberta no Ver-o-Peso, criada pela moderação num box sem tarefa ativa (RN-01). Se todos os
     * boxes tiverem tarefa, cancela uma aberta para liberar.
     */
    public Task openUpdateTask() {
        Api mod = moderator();
        JsonNode stalls = api.get("/api/v1/markets/ver-o-peso/stalls?size=50")
                .expect(200)
                .json()
                .path("items");
        for (int attempt = 0; attempt < 2; attempt++) {
            for (JsonNode stall : stalls) {
                Api.Res created = mod.post(
                        "/api/v1/moderation/tasks",
                        Map.of(
                                "type",
                                "UPDATE_STALL",
                                "marketSlug",
                                "ver-o-peso",
                                "stallPublicId",
                                stall.path("publicId").asString(),
                                "priority",
                                3));
                if (created.status() == 201) {
                    JsonNode t = created.json();
                    if ("ON_HOLD".equals(t.path("status").asString())) {
                        ensureBudget();
                        mod.post(
                                        "/api/v1/moderation/tasks/"
                                                + t.path("publicId").asString() + "/publish",
                                        Map.of("volunteer", false))
                                .expect(204);
                    }
                    return new Task(
                            t.path("publicId").asString(),
                            t.path("stall").path("publicId").asString(),
                            t.path("stall").path("code").asString(),
                            t.path("stall").path("version").asInt());
                }
            }
            JsonNode open = mod.get("/api/v1/admin/tasks?status=OPEN&size=50")
                    .expect(200)
                    .json()
                    .path("items");
            for (JsonNode t : open) {
                if (!t.path("stall").isNull()) {
                    mod.post("/api/v1/moderation/tasks/" + t.path("publicId").asString() + "/cancel", Map.of())
                            .expect(204);
                }
            }
        }
        throw new IllegalStateException("Nenhum box livre para criar tarefa");
    }

    /** Repõe a verba de teste do mês (RN-10), para as tarefas novas nascerem abertas. */
    public void ensureBudget() {
        Api admin = admin();
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
                                Map.of("amount", "5000.00", "currency", "BRL")))
                .expect(200);
    }

    public void acceptAndCheckIn(Api collaborator, String taskId) {
        collaborator.post("/api/v1/tasks/" + taskId + "/accept", null).expect(200);
        collaborator
                .post(
                        "/api/v1/tasks/" + taskId + "/check-in",
                        Map.of("latitude", VOP_LAT + 0.0002, "longitude", VOP_LNG, "accuracyMeters", 12))
                .expect(200);
    }

    public String uploadPhoto(Api collaborator, Color color) {
        return collaborator
                .upload(
                        "/api/v1/uploads",
                        png(color),
                        "image/png",
                        UUID.randomUUID().toString())
                .expect(201)
                .json()
                .path("fileId")
                .asString();
    }

    /** Changeset de atualização que confirma todas as ofertas do box e muda o preço da primeira (Anexo A). */
    public Map<String, Object> confirmAll(Task task, String fileId, String newPrice) {
        JsonNode stall = api.get("/api/v1/stalls/" + task.stallId()).expect(200).json();
        List<Map<String, Object>> offerings = new ArrayList<>();
        boolean first = true;
        for (JsonNode o : stall.path("offerings")) {
            if (first && newPrice != null) {
                offerings.add(Map.of(
                        "action",
                        "UPDATE",
                        "productSlug",
                        o.path("productSlug").asString(),
                        "baseVersion",
                        o.path("version").asInt(),
                        "referencePrice",
                        Map.of("amount", newPrice, "currency", "BRL"),
                        "priceUnit",
                        o.path("priceUnit").isNull()
                                ? "KG"
                                : o.path("priceUnit").asString(),
                        "availability",
                        "AVAILABLE"));
            } else {
                offerings.add(Map.of(
                        "action", "CONFIRM",
                        "productSlug", o.path("productSlug").asString(),
                        "baseVersion", o.path("version").asInt()));
            }
            first = false;
        }
        return Map.of(
                "taskType",
                "UPDATE_STALL",
                "stall",
                Map.of(
                        "publicId",
                        task.stallId(),
                        "baseVersion",
                        stall.path("version").asInt()),
                "offerings",
                offerings,
                "photos",
                List.of(Map.of("fileId", fileId, "role", "STALL")),
                "notes",
                "Conferido no box");
    }

    /** Fluxo completo até o envio pendente na moderação; devolve o id do envio. */
    public String submittedTask(Person who, Task task, String newPrice) {
        acceptAndCheckIn(who.api(), task.publicId());
        String file = uploadPhoto(who.api(), new Color(SEQ.incrementAndGet() * 40 % 255, 120, 80));
        return who.api()
                .post(
                        "/api/v1/tasks/" + task.publicId() + "/submission",
                        confirmAll(task, file, newPrice),
                        "Idempotency-Key",
                        UUID.randomUUID().toString())
                .expect(201)
                .json()
                .path("submissionId")
                .asString();
    }

    public static byte[] png(Color color) {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 640, 480);
        g.setColor(Color.WHITE);
        g.drawString("NANDESK " + UUID.randomUUID(), 40, 240);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
