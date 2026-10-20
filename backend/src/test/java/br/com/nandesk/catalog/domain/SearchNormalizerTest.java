package br.com.nandesk.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("RF-02")
@Tag("RF-36")
class SearchNormalizerTest {

    /** Casos compartilhados com o frontend (contracts/search-normalization-cases.json). */
    static Stream<Arguments> sharedCases() throws Exception {
        Path contract = Path.of("..", "contracts", "search-normalization-cases.json");
        JsonNode root = JsonMapper.builder().build().readTree(Files.readString(contract));
        return root.get("cases")
                .valueStream()
                .map(c -> Arguments.of(
                        c.get("input").asString(), c.get("expected").asString()));
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @MethodSource("sharedCases")
    @DisplayName("RF-02: normaliza ignorando acentos, maiúsculas, hífens e pontuação (contrato compartilhado)")
    void normalizesSharedCases(String input, String expected) {
        assertThat(SearchNormalizer.normalize(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("RF-02: \"acai\" e \"Açaí\" viram o mesmo termo")
    void accentInsensitive() {
        assertThat(SearchNormalizer.normalize("acai")).isEqualTo(SearchNormalizer.normalize("Açaí"));
    }

    @Test
    @DisplayName("RF-02: entrada nula vira texto vazio")
    void nullInput() {
        assertThat(SearchNormalizer.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("RF-02: divide o termo normalizado em palavras")
    void tokens() {
        assertThat(SearchNormalizer.tokens("Castanha-do-Pará")).containsExactly("castanha", "do", "para");
    }
}
