package br.com.nandesk.commission.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;

/** Valor da comissão (RN-07): o da regra vigente no momento da criação da tarefa; sem regra, zero (voluntário). */
public final class CommissionCalculator {

    private CommissionCalculator() {}

    public record RuleVersion(String taskType, BigDecimal amount, Instant validFrom, Instant validTo) {

        boolean appliesAt(Instant instant) {
            return !instant.isBefore(validFrom) && (validTo == null || instant.isBefore(validTo));
        }
    }

    public static BigDecimal amountAt(Collection<RuleVersion> rules, String taskType, Instant instant) {
        return rules.stream()
                .filter(r -> r.taskType().equals(taskType) && r.appliesAt(instant))
                .max(Comparator.comparing(RuleVersion::validFrom))
                .map(r -> LedgerBalance.money(r.amount()))
                .orElse(LedgerBalance.money(BigDecimal.ZERO));
    }
}
