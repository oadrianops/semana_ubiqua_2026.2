package br.com.nandesk.task.domain;

import static br.com.nandesk.task.domain.AssignmentOutcome.APPROVED;
import static br.com.nandesk.task.domain.AssignmentOutcome.EXPIRED;
import static br.com.nandesk.task.domain.AssignmentOutcome.REJECTED;
import static br.com.nandesk.task.domain.AssignmentOutcome.RELEASED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-23")
class ReputationCalculatorTest {

    private final ReputationCalculator calculator = new ReputationCalculator(20, 80, 5);

    @Test
    @DisplayName("RN-17: com menos de 5 resultados vale o valor inicial (80)")
    void initialScore() {
        assertThat(calculator.score(List.of(new ReputationCalculator.Result(REJECTED, true))))
                .isEqualTo(80);
    }

    @Test
    @DisplayName("RN-17: round(100 × A / (A + R + 0,5 × E))")
    void formula() {
        List<ReputationCalculator.Result> results = new ArrayList<>();
        results.addAll(Collections.nCopies(6, new ReputationCalculator.Result(APPROVED, true)));
        results.addAll(Collections.nCopies(2, new ReputationCalculator.Result(REJECTED, true)));
        results.addAll(Collections.nCopies(2, new ReputationCalculator.Result(EXPIRED, true)));
        // 100 × 6 / (6 + 2 + 1) = 66,67 → 67
        assertThat(calculator.score(results)).isEqualTo(67);
    }

    @Test
    @DisplayName("RN-17: desistência antes do check-in não conta; depois do check-in conta como expirada")
    void releasesBeforeCheckInIgnored() {
        List<ReputationCalculator.Result> results = new ArrayList<>();
        results.addAll(Collections.nCopies(5, new ReputationCalculator.Result(APPROVED, true)));
        results.addAll(Collections.nCopies(10, new ReputationCalculator.Result(RELEASED, false)));
        assertThat(calculator.score(results)).isEqualTo(100);
        results.add(new ReputationCalculator.Result(RELEASED, true));
        // 100 × 5 / (5 + 0,5) = 90,9 → 91
        assertThat(calculator.score(results)).isEqualTo(91);
    }

    @Test
    @DisplayName("RN-17: considera só os últimos 20 resultados")
    void window() {
        List<ReputationCalculator.Result> results = new ArrayList<>();
        // Mais recentes primeiro: 20 aprovados e, mais antigos, 10 rejeitados que ficam fora da janela.
        results.addAll(Collections.nCopies(20, new ReputationCalculator.Result(APPROVED, true)));
        results.addAll(Collections.nCopies(10, new ReputationCalculator.Result(REJECTED, true)));
        assertThat(calculator.score(results)).isEqualTo(100);
    }

    @Test
    @DisplayName("RN-17: só rejeições levam a 0")
    void onlyRejections() {
        assertThat(calculator.score(Collections.nCopies(5, new ReputationCalculator.Result(REJECTED, true))))
                .isZero();
    }
}
