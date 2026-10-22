package br.com.nandesk.stall.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("RF-03")
class InstagramHandleTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "fruteiradojorjao",
                "@fruteiradojorjao",
                "  @FruteiraDoJorjao ",
                "instagram.com/fruteiradojorjao",
                "https://www.instagram.com/fruteiradojorjao/",
                "https://instagram.com/fruteiradojorjao?igsh=MTZ3",
                "http://instagr.am/fruteiradojorjao#posts"
            })
    @DisplayName("RF-03: aceita o perfil escrito de vários jeitos e guarda só o nome, em minúsculas")
    void normalizesCommonForms(String raw) {
        assertThat(InstagramHandle.normalize(raw)).contains("fruteiradojorjao");
    }

    @Test
    @DisplayName("RF-03: sem perfil, nada é guardado")
    void blankIsEmpty() {
        assertThat(InstagramHandle.normalize(null)).isEmpty();
        assertThat(InstagramHandle.normalize("  ")).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"perfil com espaço", ".comeca", "termina.", "dois..pontos", "acentuação", "@"})
    @DisplayName("RF-03: recusa o que o Instagram não aceita como nome de perfil")
    void rejectsInvalid(String raw) {
        assertThatThrownBy(() -> InstagramHandle.normalize(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-03: no máximo 30 caracteres")
    void maxLength() {
        assertThat(InstagramHandle.normalize("a".repeat(30))).isPresent();
        assertThatThrownBy(() -> InstagramHandle.normalize("a".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(InstagramHandle.profileUrl("box_67")).isEqualTo("https://www.instagram.com/box_67/");
    }
}
