package br.com.nandesk.shared;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.i18n.Translatable;
import br.com.nandesk.shared.time.BelemTime;
import br.com.nandesk.shared.web.Masking;
import br.com.nandesk.shared.web.MoneyDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-06")
class SharedRulesTest {

    record T(String getLocale, String text) implements Translatable {}

    @Test
    @DisplayName("RN-19: conteúdo no idioma pedido, com fallback para PT-BR")
    void translationFallback() {
        List<T> translations = List.of(new T("pt-BR", "Castanha-do-pará"), new T("en", "Brazil nut"));
        assertThat(Translatable.pick(translations, AppLocale.EN)).map(T::text).contains("Brazil nut");
        assertThat(Translatable.pick(translations, AppLocale.ES)).map(T::text).contains("Castanha-do-pará");
    }

    @Test
    @DisplayName("RN-19: Accept-Language desconhecido cai em PT-BR")
    void localeFallback() {
        assertThat(AppLocale.fromTag("fr-FR")).isEqualTo(AppLocale.PT_BR);
        assertThat(AppLocale.fromTag("en-US")).isEqualTo(AppLocale.EN);
        assertThat(AppLocale.fromTag("es-AR")).isEqualTo(AppLocale.ES);
        assertThat(AppLocale.fromTag(null)).isEqualTo(AppLocale.PT_BR);
    }

    @Test
    @DisplayName("RN-23: o dia civil é o de Belém (UTC−3)")
    void belemDay() {
        // 02:00 UTC do dia 21 ainda é dia 20 em Belém.
        Clock clock = Clock.fixed(Instant.parse("2026-10-21T02:00:00Z"), ZoneOffset.UTC);
        assertThat(BelemTime.today(clock)).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(BelemTime.startOfDay(LocalDate.of(2026, 10, 20))).isEqualTo(Instant.parse("2026-10-20T03:00:00Z"));
    }

    @Test
    @DisplayName("RN-24: dinheiro na API como string decimal com 2 casas")
    void moneyAsString() {
        assertThat(MoneyDto.brl(new BigDecimal("12.5"))).isEqualTo(new MoneyDto("12.50", "BRL"));
        assertThat(MoneyDto.brl(null)).isNull();
    }

    @Test
    @DisplayName("13.4: e-mail mascarado nos logs")
    void maskEmail() {
        assertThat(Masking.email("ana.silva@dominio.com")).isEqualTo("a***@d***.com");
        assertThat(Masking.pixKey("123e4567-e89b-12d3-a456-426614174000")).isEqualTo("••••4000");
    }
}
