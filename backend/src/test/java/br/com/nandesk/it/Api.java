package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP dos testes de integração: JSON, cookie de renovação (por instância), token Bearer, idioma e
 * multipart para as fotos. Sem dependência do TestRestTemplate.
 */
public class Api {

    public static final JsonMapper JSON = JsonMapper.builder().build();

    private final String base;
    private final HttpClient http;
    private String bearer;
    private String language = "pt-BR";

    public Api(String base) {
        this.base = base;
        this.http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    public record Res(int status, String body, Map<String, String> headers) {

        public JsonNode json() {
            return body == null || body.isBlank() ? JSON.nullNode() : JSON.readTree(body);
        }

        public String code() {
            return json().path("code").asString();
        }

        public Res expect(int expected) {
            assertThat(status).as("HTTP %s: %s", status, body).isEqualTo(expected);
            return this;
        }

        public String header(String name) {
            return headers.get(name.toLowerCase());
        }
    }

    public Api as(String accessToken) {
        Api copy = new Api(base);
        copy.bearer = accessToken;
        copy.language = language;
        return copy;
    }

    public Api lang(String lng) {
        this.language = lng;
        return this;
    }

    public Res get(String path, String... headers) {
        return send(builder(path, headers).GET());
    }

    public Res post(String path, Object body, String... headers) {
        return send(builder(path, headers)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : JSON.writeValueAsString(body))));
    }

    public Res put(String path, Object body) {
        return send(builder(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))));
    }

    public Res patch(String path, Object body) {
        return send(builder(path)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))));
    }

    public Res delete(String path) {
        return send(builder(path).DELETE());
    }

    public Res upload(String path, byte[] content, String contentType, String idempotencyKey) {
        String boundary = "----nandesk" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"foto.png\"\r\n"
                            + "Content-Type: " + contentType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return send(builder(path, "Idempotency-Key", idempotencyKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())));
    }

    private HttpRequest.Builder builder(String path, String... headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Accept-Language", language);
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        for (int i = 0; i + 1 < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
        return b;
    }

    private Res send(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((k, v) -> headers.put(k.toLowerCase(), String.join(",", v)));
            return new Res(response.statusCode(), response.body(), headers);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
