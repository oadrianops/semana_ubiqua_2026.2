package br.com.nandesk.commission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-28")
class BudgetPolicyTest {

    @Test
    @DisplayName("RN-10: publica quando orçamento − comprometido − realizado ≥ comissão")
    void publishesWithinBudget() {
        assertThat(BudgetPolicy.canPublish(
                        new BigDecimal("100.00"),
                        new BigDecimal("60.00"),
                        new BigDecimal("36.00"),
                        new BigDecimal("4.00")))
                .isTrue();
    }

    @Test
    @DisplayName("RN-10: sem saldo de orçamento, a tarefa fica ON_HOLD")
    void holdsWhenExhausted() {
        assertThat(BudgetPolicy.canPublish(
                        new BigDecimal("100.00"),
                        new BigDecimal("60.00"),
                        new BigDecimal("36.01"),
                        new BigDecimal("4.00")))
                .isFalse();
    }

    @Test
    @DisplayName("RN-10: sem orçamento cadastrado no mês, não há teto")
    void noBudgetNoCeiling() {
        assertThat(BudgetPolicy.canPublish(
                        null, new BigDecimal("999.00"), new BigDecimal("999.00"), new BigDecimal("4.00")))
                .isTrue();
    }

    @Test
    @DisplayName("RN-10: comissão zero (modo voluntário) sempre pode ser publicada")
    void volunteerAlwaysPublishes() {
        assertThat(BudgetPolicy.canPublish(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isTrue();
    }

    @Test
    @DisplayName("RN-10: disponível = orçamento − comprometido − realizado")
    void available() {
        assertThat(BudgetPolicy.available(new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("30.00")))
                .isEqualByComparingTo("50.00");
    }
}
