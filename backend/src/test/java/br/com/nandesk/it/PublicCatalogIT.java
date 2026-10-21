package br.com.nandesk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;

/** Vitrine pública e busca (RF-01 a RF-06, RF-36, CA-1, CA-3) contra o banco com o seed, antes dos outros *IT. */
@Order(1)
class PublicCatalogIT extends IntegrationTest {

    record Expected(String slug, String namePt, List<String> terms, Set<String> stalls) {}

    static Stream<Arguments> seedProducts() throws IOException {
        JsonNode seed;
        try (InputStream in = new ClassPathResource("seed/dev-seed.json").getInputStream()) {
            seed = Api.JSON.readTree(in);
        }
        List<Arguments> out = new ArrayList<>();
        for (JsonNode p : seed.path("products")) {
            String slug = p.path("slug").asString();
            Set<String> stalls = new HashSet<>();
            for (JsonNode s : seed.path("stalls")) {
                for (JsonNode o : s.path("offerings")) {
                    if (o.path("product").asString().equals(slug)) {
                        stalls.add(s.path("market").asString() + "/"
                                + s.path("code").asString());
                    }
                }
            }
            List<String> terms = new ArrayList<>();
            terms.add(p.path("names").path("pt-BR").asString());
            for (String lng : List.of("en", "es")) {
                for (JsonNode alias : p.path("aliases").path(lng)) {
                    terms.add(alias.asString());
                }
            }
            out.add(Arguments.of(slug, terms, stalls));
        }
        assertThat(out).hasSize(26);
        return out.stream();
    }

    @Tag("CA-1")
    @ParameterizedTest(name = "CA-1: {0}")
    @MethodSource("seedProducts")
    @DisplayName("CA-1: cada produto do seed aparece na feira e no box certos, em PT e pelos sinônimos EN/ES")
    void everySeedProductIsFound(String slug, List<String> terms, Set<String> expectedStalls) {
        for (String term : terms) {
            JsonNode result = api.get("/api/v1/search?q=" + URLEncoder.encode(term, StandardCharsets.UTF_8))
                    .expect(200)
                    .json();
            JsonNode item = null;
            for (JsonNode i : result.path("items")) {
                if (i.path("product").path("slug").asString().equals(slug)) {
                    item = i;
                }
            }
            assertThat(item)
                    .as("busca por \"%s\" deveria trazer %s", term, slug)
                    .isNotNull();
            Set<String> found = new HashSet<>();
            for (JsonNode m : item.path("markets")) {
                for (JsonNode s : m.path("stalls")) {
                    found.add(m.path("market").path("slug").asString() + "/"
                            + s.path("code").asString());
                }
            }
            assertThat(found).as("boxes para \"%s\"", term).containsExactlyInAnyOrderElementsOf(expectedStalls);
        }
    }

    @Test
    @Tag("RF-02")
    @DisplayName("RF-02: busca sem acento, com erro de digitação sugere o termo e devolve o idioma pedido")
    void searchToleratesAccentsAndTypos() {
        JsonNode acai = api.get("/api/v1/search?q=acai").expect(200).json();
        assertThat(acai.path("items").get(0).path("product").path("slug").asString())
                .isEqualTo("acai-polpa");

        JsonNode typo = api.get("/api/v1/search?q=jambo").expect(200).json();
        assertThat(typo.path("didYouMean").toString()).contains("jambu");

        JsonNode en =
                api.lang("en").get("/api/v1/search?q=castanha").expect(200).json();
        assertThat(en.path("locale").asString()).isEqualTo("en");
        assertThat(en.path("items").get(0).path("product").path("name").asString())
                .isEqualTo("Brazil nut");

        JsonNode suggestions = api.lang("pt-BR")
                .get("/api/v1/search/suggestions?q=ca")
                .expect(200)
                .json();
        assertThat(suggestions.size()).isPositive();

        assertThat(api.get("/api/v1/search?q=").expect(200).json().path("items").size())
                .isZero();
    }

    @Test
    @Tag("RF-01")
    @DisplayName("RF-01 RF-03 RF-04 RF-05: feiras, setores, boxes, produto e categorias")
    void storefront() {
        JsonNode markets = api.get("/api/v1/markets").expect(200).json();
        assertThat(markets.size()).isEqualTo(2);
        JsonNode vop = api.get("/api/v1/markets/ver-o-peso").expect(200).json();
        assertThat(vop.path("sectors").size()).isPositive();
        JsonNode stalls =
                api.get("/api/v1/markets/ver-o-peso/stalls?size=50").expect(200).json();
        assertThat(stalls.path("totalItems").asInt()).isEqualTo(8);
        String stallId = stalls.path("items").get(0).path("publicId").asString();
        JsonNode stall = api.get("/api/v1/stalls/" + stallId).expect(200).json();
        assertThat(stall.path("offerings").size()).isPositive();
        assertThat(stall.path("lastVerifiedAt").isNull()).isFalse();

        JsonNode categories = api.get("/api/v1/categories").expect(200).json();
        assertThat(categories.size()).isEqualTo(9);
        String category = categories.get(0).path("slug").asString();
        api.get("/api/v1/categories/" + category + "/products").expect(200);

        JsonNode product = api.get("/api/v1/products/jambu").expect(200).json();
        assertThat(product.path("markets").get(0).path("market").path("slug").asString())
                .isEqualTo("feira-da-25");
        api.get("/api/v1/products/nao-existe").expect(404);
        api.get("/api/v1/stalls/00000000-0000-4000-8000-000000000000").expect(404);
    }

    @Test
    @Tag("RF-36")
    @DisplayName("RF-36: snapshot com ETag por versão e idioma, 304 quando nada mudou")
    void snapshotEtag() {
        Api.Res first = api.get("/api/v1/markets/ver-o-peso/snapshot").expect(200);
        String etag = first.header("ETag");
        assertThat(etag).contains("pt-BR");
        assertThat(first.json().path("stalls").size()).isEqualTo(8);
        api.get("/api/v1/markets/ver-o-peso/snapshot", "If-None-Match", etag).expect(304);
        Api.Res en = api.lang("en").get("/api/v1/markets/ver-o-peso/snapshot", "If-None-Match", etag);
        assertThat(en.status()).isEqualTo(200);
        assertThat(en.header("ETag")).contains("en");
    }

    @Test
    @Tag("RF-06")
    @DisplayName("RF-06 RN-19: textos no idioma pedido, com o PT-BR como reserva, e erros em Problem Details")
    void languagesAndProblemDetails() {
        JsonNode es = api.lang("es").get("/api/v1/products/jambu").expect(200).json();
        assertThat(es.path("product").path("name").asString()).isNotBlank();
        JsonNode problem =
                api.lang("en").get("/api/v1/products/nao-existe").expect(404).json();
        assertThat(problem.path("code").asString()).isEqualTo("NOT_FOUND");
        assertThat(problem.path("title").asString()).isNotBlank();
    }
}
