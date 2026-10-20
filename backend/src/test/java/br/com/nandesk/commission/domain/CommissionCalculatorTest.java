package br.com.nandesk.commission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nandesk.commission.domain.CommissionCalculator.RuleVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-25")
class CommissionCalculatorTest {

    private static final Instant OCT_01 = Instant.parse("2026-10-01T03:00:00Z");
    private static final Instant OCT_15 = Instant.parse("2026-10-15T03:00:00Z");

    private final List<RuleVersion> rules = List.of(
            new RuleVersion("UPDATE_STALL", new BigDecimal("4.00"), OCT_01, OCT_15),
            new RuleVersion("UPDATE_STALL", new BigDecimal("5.00"), OCT_15, null),
            new RuleVersion("NEW_STALL", new BigDecimal("8.00"), OCT_01, null));

    @Test
    @DisplayName("RN-07: o valor vem da regra vigente no momento da criação da tarefa")
    void ruleInForce() {
        assertThat(CommissionCalculator.amountAt(rules, "UPDATE_STALL", Instant.parse("2026-10-10T12:00:00Z")))
                .isEqualByComparingTo("4.00");
        assertThat(CommissionCalculator.amountAt(rules, "UPDATE_STALL", Instant.parse("2026-10-20T12:00:00Z")))
                .isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("RN-07: sem regra cadastrada, comissão zero (modo voluntário)")
    void noRuleMeansVolunteer() {
        assertThat(CommissionCalculator.amountAt(rules, "VERIFY_REPORT", OCT_15))
                .isEqualByComparingTo("0.00");
        assertThat(CommissionCalculator.amountAt(rules, "NEW_STALL", Instant.parse("2026-09-01T00:00:00Z")))
                .isEqualByComparingTo("0.00");
    }
}
