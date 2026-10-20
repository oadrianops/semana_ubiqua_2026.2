package br.com.nandesk.task.domain;

import java.util.List;

/**
 * Reputação (RN-17): {@code round(100 × A / (A + R + 0,5 × E))} sobre os últimos N resultados (A = aprovados,
 * R = rejeitados, E = expirados ou desistências depois do check-in). Com menos de {@code minimumResults}, vale o
 * valor inicial.
 */
public record ReputationCalculator(int window, int initialScore, int minimumResults) {

    /** Resultado de uma tentativa; {@code checkedIn} distingue desistência antes/depois do check-in. */
    public record Result(AssignmentOutcome outcome, boolean checkedIn) {

        boolean counts() {
            return outcome != AssignmentOutcome.RELEASED || checkedIn;
        }
    }

    /** @param results resultados do mais recente para o mais antigo */
    public int score(List<Result> results) {
        List<Result> counted =
                results.stream().filter(Result::counts).limit(window).toList();
        if (counted.size() < minimumResults) {
            return initialScore;
        }
        long approved = counted.stream()
                .filter(r -> r.outcome() == AssignmentOutcome.APPROVED)
                .count();
        long rejected = counted.stream()
                .filter(r -> r.outcome() == AssignmentOutcome.REJECTED)
                .count();
        long expired = counted.stream()
                .filter(r -> r.outcome() == AssignmentOutcome.EXPIRED || r.outcome() == AssignmentOutcome.RELEASED)
                .count();
        double denominator = approved + rejected + 0.5 * expired;
        if (denominator == 0) {
            return initialScore;
        }
        return (int) Math.round(100.0 * approved / denominator);
    }
}
