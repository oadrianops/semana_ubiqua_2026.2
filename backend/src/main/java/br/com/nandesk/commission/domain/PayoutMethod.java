package br.com.nandesk.commission.domain;

/** Forma do repasse feito fora da plataforma (P2, RF-27). */
public enum PayoutMethod {
    PIX,
    TRANSFER,
    CASH,
    OTHER
}
