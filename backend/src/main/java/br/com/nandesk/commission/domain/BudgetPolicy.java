package br.com.nandesk.commission.domain;

import java.math.BigDecimal;

/**
 * Orçamento mensal (RN-10): com orçamento cadastrado, a tarefa só é publicada se
 * {@code orçamento − comprometido − realizado ≥ comissão}; sem orçamento no mês não há teto; comissão zero sempre
 * pode ser publicada (modo voluntário).
 */
public final class BudgetPolicy {

    private BudgetPolicy() {}

    public static BigDecimal available(BigDecimal budget, BigDecimal committed, BigDecimal realized) {
        return LedgerBalance.money(budget.subtract(committed).subtract(realized));
    }

    public static boolean canPublish(
            BigDecimal budget, BigDecimal committed, BigDecimal realized, BigDecimal commission) {
        if (commission.signum() == 0 || budget == null) {
            return true;
        }
        return available(budget, committed, realized).compareTo(commission) >= 0;
    }
}
