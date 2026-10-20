package br.com.nandesk.commission.domain;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Saldo e repasse (RN-08, RN-09, RN-24): lançamentos com sinal, saldo = soma; repasse ≤ saldo e ≥ mínimo;
 * dinheiro sempre com 2 casas e HALF_EVEN.
 */
public final class LedgerBalance {

    private LedgerBalance() {}

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal balance(Collection<BigDecimal> signedAmounts) {
        return money(signedAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Converte o valor informado no valor com sinal gravado no extrato. */
    public static BigDecimal signed(EntryType type, BigDecimal amount) {
        BigDecimal value = money(amount);
        return switch (type) {
            case CREDIT -> value.abs();
            case PAYOUT, REVERSAL -> value.abs().negate();
            case ADJUSTMENT -> value;
        };
    }

    public static void checkPayout(BigDecimal balance, BigDecimal amount, BigDecimal minimum) {
        if (amount.compareTo(minimum) < 0) {
            throw new BusinessException(ErrorCode.PAYOUT_BELOW_MINIMUM);
        }
        if (amount.compareTo(balance) > 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    public static void requireJustification(String justification) {
        if (justification == null || justification.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
