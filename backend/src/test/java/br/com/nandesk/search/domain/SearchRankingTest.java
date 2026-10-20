package br.com.nandesk.search.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nandesk.search.domain.SearchRanking.Candidate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-02")
class SearchRankingTest {

    @Test
    @DisplayName("RF-02: termo igual à consulta vale 100")
    void exactMatch() {
        assertThat(SearchRanking.score("jambu", "jambu", false)).isEqualTo(100);
    }

    @Test
    @DisplayName("RF-02: termo que começa com a consulta vale 80")
    void prefix() {
        assertThat(SearchRanking.score("castanha do para", "castanha", false)).isEqualTo(80);
    }

    @Test
    @DisplayName("RF-02: alguma palavra começando com a consulta vale 60")
    void wordPrefix() {
        assertThat(SearchRanking.score("oleo de copaiba", "copa", false)).isEqualTo(60);
    }

    @Test
    @DisplayName("RF-02: termo que só contém a consulta vale 40")
    void contains() {
        assertThat(SearchRanking.score("pato no tucupi", "ucup", false)).isEqualTo(40);
    }

    @Test
    @DisplayName("RF-02: consulta com várias palavras exige todas (cada uma pode estar em qualquer posição)")
    void multiWord() {
        assertThat(SearchRanking.score("castanha do para", "para castanha", false))
                .isEqualTo(40);
        assertThat(SearchRanking.score("castanha do para", "para jambu", false)).isZero();
    }

    @Test
    @DisplayName("RF-02: +10 quando o termo é do idioma pedido")
    void localeBonus() {
        assertThat(SearchRanking.score("brazil nut", "brazil nut", true)).isEqualTo(110);
    }

    @Test
    @DisplayName("RF-02: sem relação vale 0")
    void noMatch() {
        assertThat(SearchRanking.score("tucupi", "acai", false)).isZero();
    }

    @Test
    @DisplayName("RF-02: desempate por mais boxes disponíveis e depois por nome")
    void tieBreak() {
        Candidate a = new Candidate(1L, "Bacuri", 80, 1);
        Candidate b = new Candidate(2L, "Açaí", 80, 3);
        Candidate c = new Candidate(3L, "Abacaxi", 80, 3);
        Candidate d = new Candidate(4L, "Jambu", 100, 0);
        assertThat(SearchRanking.sort(List.of(a, b, c, d)))
                .extracting(Candidate::productId)
                .containsExactly(4L, 3L, 2L, 1L);
    }
}
