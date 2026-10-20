package br.com.nandesk.commission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-26")
@Tag("RF-27")
class LedgerBalanceTest {

    private static final BigDecimal MIN = new BigDecimal("10.00");

    @Test
    @DisplayName("RN-09: saldo é a soma dos lançamentos com sinal")
    void balanceIsSignedSum() {
        BigDecimal balance = LedgerBalance.balance(List.of(
                new BigDecimal("4.00"), new BigDecimal("8.00"), new BigDecimal("-10.00"), new BigDecimal("3.00")));
        assertThat(balance).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("RN-09: sinal por tipo — crédito +, repasse e estorno −, ajuste com o sinal informado")
    void signs() {
        assertThat(LedgerBalance.signed(EntryType.CREDIT, new BigDecimal("4.00")))
                .isEqualByComparingTo("4.00");
        assertThat(LedgerBalance.signed(EntryType.PAYOUT, new BigDecimal("4.00")))
                .isEqualByComparingTo("-4.00");
        assertThat(LedgerBalance.signed(EntryType.REVERSAL, new BigDecimal("4.00")))
                .isEqualByComparingTo("-4.00");
        assertThat(LedgerBalance.signed(EntryType.ADJUSTMENT, new BigDecimal("-2.50")))
                .isEqualByComparingTo("-2.50");
        assertThat(LedgerBalance.signed(EntryType.ADJUSTMENT, new BigDecimal("2.50")))
                .isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("RN-09: repasse não pode exceder o saldo")
    void payoutCannotExceedBalance() {
        assertThatThrownBy(() -> LedgerBalance.checkPayout(new BigDecimal("12.00"), new BigDecimal("12.01"), MIN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("RN-09: repasse precisa respeitar o mínimo de R$ 10,00")
    void payoutMinimum() {
        assertThatThrownBy(() -> LedgerBalance.checkPayout(new BigDecimal("50.00"), new BigDecimal("9.99"), MIN))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.PAYOUT_BELOW_MINIMUM);
        assertThatCode(() -> LedgerBalance.checkPayout(new BigDecimal("50.00"), new BigDecimal("10.00"), MIN))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN-24: dinheiro com 2 casas e arredondamento HALF_EVEN")
    void moneyRounding() {
        assertThat(LedgerBalance.money(new BigDecimal("2.345"))).isEqualByComparingTo("2.34");
        assertThat(LedgerBalance.money(new BigDecimal("2.355"))).isEqualByComparingTo("2.36");
        assertThat(LedgerBalance.money(new BigDecimal("2.345")).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("RN-08: estorno e ajuste exigem justificativa")
    void justificationRequired() {
        assertThatThrownBy(() -> LedgerBalance.requireJustification(" "))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatCode(() -> LedgerBalance.requireJustification("Crédito em duplicidade"))
                .doesNotThrowAnyException();
    }
}
