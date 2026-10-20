package br.com.nandesk.commission.domain;

/** Tipos de lançamento do extrato imutável (RN-08). */
public enum EntryType {
    CREDIT,
    PAYOUT,
    REVERSAL,
    ADJUSTMENT
}
