package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrato da API (seção 15.2): gera {@code docs/api/openapi.json}. No CI ({@code CI=true}) falha se o arquivo
 * versionado estiver desatualizado; localmente, regrava o arquivo.
 */
class OpenApiSnapshotIT extends IntegrationTest {

    private static final Path SNAPSHOT = Path.of("..", "docs", "api", "openapi.json");

    @Test
    @DisplayName("Contrato OpenAPI versionado igual ao gerado pela API")
    void snapshot() throws IOException {
        JsonNode spec = api.get("/v3/api-docs").expect(200).json();
        assertThat(spec.path("paths").size()).isGreaterThan(50);
        JsonMapper pretty = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        // A porta muda a cada execução: o servidor não entra no contrato.
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> tree = new java.util.TreeMap<>(pretty.convertValue(spec, java.util.Map.class));
        tree.remove("servers");
        String generated =
                pretty.writeValueAsString(tree) + System.lineSeparator().replace("\r", "");
        if ("true".equals(System.getenv("CI"))) {
            assertThat(Files.exists(SNAPSHOT))
                    .as("docs/api/openapi.json versionado")
                    .isTrue();
            assertThat(Files.readString(SNAPSHOT, StandardCharsets.UTF_8))
                    .as("Contrato desatualizado: rode os *IT localmente e versione docs/api/openapi.json")
                    .isEqualTo(generated);
        } else {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, generated, StandardCharsets.UTF_8);
        }
    }
}
