package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Cadastro, sessão, senha, perfil e LGPD (RF-08 a RF-12, RN-20, RN-22). */
class AccountIT extends IntegrationTest {

    private Fixtures fx() {
        return new Fixtures(api, mail);
    }

    private Map<String, Object> registration(String email, String password) {
        return Map.of(
                "name",
                "Rita do Açaí",
                "email",
                email,
                "password",
                password,
                "locale",
                "pt-BR",
                "acceptTerms",
                true,
                "adultDeclaration",
                true);
    }

    private static String email() {
        return "rita-" + UUID.randomUUID().toString().substring(0, 8) + "@teste.nandesk.com.br";
    }

    @Test
    @Tag("RF-08")
    @DisplayName(
            "RF-08 RF-10: cadastro exige termos, recusa senha comum e e-mail repetido; aceitar tarefa pede e-mail confirmado")
    void registrationAndVerification() {
        String email = email();
        api.post("/api/v1/auth/register", Map.of("name", "X", "email", email, "password", "12345678"))
                .expect(400);
        api.post("/api/v1/auth/register", registration(email, "12345678")).expect(400);

        String password = "Castanha-" + UUID.randomUUID();
        JsonNode created = api.post("/api/v1/auth/register", registration(email, password))
                .expect(201)
                .json();
        assertThat(created.path("verificationRequired").asBoolean()).isTrue();
        assertThat(api.post("/api/v1/auth/register", registration(email, password))
                        .code())
                .isEqualTo("EMAIL_ALREADY_REGISTERED");

        Api session = api.as(fx().login(email, password));
        JsonNode blocked = session.get("/api/v1/tasks/available").expect(200).json();
        assertThat(blocked.path("canAccept").asBoolean()).isFalse();
        assertThat(blocked.path("blockedReason").asString()).isEqualTo("ACCOUNT_NOT_VERIFIED");

        api.post("/api/v1/auth/verify-email/resend", Map.of("email", email)).expect(202);
        String token = mail.tokenSentTo(email, "confirm");
        api.post("/api/v1/auth/verify-email", Map.of("token", token)).expect(204);
        api.post("/api/v1/auth/verify-email", Map.of("token", token)).expect(400);
        assertThat(session.get("/api/v1/tasks/available")
                        .json()
                        .path("canAccept")
                        .asBoolean())
                .isTrue();
    }

    @Test
    @Tag("RF-09")
    @DisplayName("RF-09: login, renovação pelo cookie, logout encerra a sessão")
    void sessionLifecycle() {
        Fixtures.Person p = fx().newCollaborator();
        Api browser = new Api(apiBase());
        JsonNode session = browser.post("/api/v1/auth/login", Map.of("email", p.email(), "password", p.password()))
                .expect(200)
                .json();
        assertThat(session.path("user").path("role").asString()).isEqualTo("COLLABORATOR");

        JsonNode renewed =
                browser.post("/api/v1/auth/refresh", null).expect(200).json();
        assertThat(renewed.path("accessToken").asString()).isNotBlank();
        browser.post("/api/v1/auth/logout", null).expect(204);
        browser.post("/api/v1/auth/refresh", null).expect(401);

        assertThat(api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", "errada-123"))
                        .code())
                .isEqualTo("INVALID_CREDENTIALS");
        api.get("/api/v1/me").expect(401);
        api.as("token-invalido").get("/api/v1/me").expect(401);
    }

    @Test
    @Tag("RN-24")
    @DisplayName("RF-09: após 5 senhas erradas a conta fica bloqueada por um tempo")
    void lockout() {
        Fixtures.Person p = fx().newCollaborator();
        for (int i = 0; i < 5; i++) {
            api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", "errada-" + i));
        }
        Api.Res locked = api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", p.password()));
        assertThat(locked.code()).isEqualTo("ACCOUNT_LOCKED_TEMPORARILY");
    }

    @Test
    @Tag("RF-10")
    @DisplayName("RF-10: redefinir senha pelo link do e-mail; o link vale uma vez")
    void passwordReset() {
        Fixtures.Person p = fx().newCollaborator();
        api.post("/api/v1/auth/password/forgot", Map.of("email", p.email())).expect(202);
        api.post("/api/v1/auth/password/forgot", Map.of("email", "ninguem@teste.nandesk.com.br"))
                .expect(202);
        String token = mail.tokenSentTo(p.email(), "senha");
        String fresh = "Tucupi-" + UUID.randomUUID();
        api.post("/api/v1/auth/password/reset", Map.of("token", token, "password", fresh))
                .expect(204);
        api.post("/api/v1/auth/password/reset", Map.of("token", token, "password", fresh))
                .expect(400);
        fx().login(p.email(), fresh);
        api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", p.password()))
                .expect(401);
    }

    @Test
    @Tag("RF-11")
    @DisplayName("RF-11 RN-22: perfil, idioma, chave PIX mascarada e feiras de interesse")
    void profile() {
        Fixtures.Person p = fx().newCollaborator();
        JsonNode updated = p.api()
                .patch("/api/v1/me", Map.of("name", "Rita Atualizada", "phone", "(91) 99999-0000", "locale", "en"))
                .expect(200)
                .json();
        assertThat(updated.path("preferredLocale").asString()).isEqualTo("en");
        p.api().patch("/api/v1/me", Map.of("locale", "fr")).expect(400);

        JsonNode pix = p.api()
                .put("/api/v1/me/pix-key", Map.of("type", "EMAIL", "key", "rita.pix@teste.nandesk.com.br"))
                .expect(200)
                .json();
        assertThat(pix.path("pixKeyMasked").asString()).doesNotContain("rita.pix@teste");
        assertThat(pix.path("pixKeyType").asString()).isEqualTo("EMAIL");

        JsonNode subs = p.api()
                .put("/api/v1/me/market-subscriptions", Map.of("markets", List.of("ver-o-peso", "feira-da-25")))
                .expect(200)
                .json();
        assertThat(subs.path("markets").size()).isEqualTo(2);
        p.api()
                .put("/api/v1/me/market-subscriptions", Map.of("markets", List.of("nao-existe")))
                .expect(404);
        assertThat(p.api()
                        .get("/api/v1/me/reputation")
                        .expect(200)
                        .json()
                        .path("score")
                        .asInt())
                .isEqualTo(80);
    }

    @Test
    @Tag("RF-12")
    @DisplayName("RF-12 RN-20: exportar os dados e pedir a exclusão da conta (anonimização)")
    void privacy() {
        Fixtures.Person p = fx().newCollaborator();
        JsonNode export = p.api().get("/api/v1/me/export").expect(200).json();
        assertThat(export.toString()).contains(p.email());

        p.api().post("/api/v1/me/deletion-request", null).expect(202);
        api.post("/api/v1/auth/login", Map.of("email", p.email(), "password", p.password()))
                .expect(401);
    }

    private String apiBase() {
        return "http://localhost:" + port;
    }
}
