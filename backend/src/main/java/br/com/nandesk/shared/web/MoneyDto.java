package br.com.nandesk.shared.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Dinheiro na API: {@code {"amount": "12.50", "currency": "BRL"}} com amount como string (RN-24). */
public record MoneyDto(String amount, String currency) {

    public static final String BRL = "BRL";

    public static MoneyDto brl(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return new MoneyDto(value.setScale(2, RoundingMode.HALF_EVEN).toPlainString(), BRL);
    }

    public BigDecimal toBigDecimal() {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_EVEN);
    }
}
