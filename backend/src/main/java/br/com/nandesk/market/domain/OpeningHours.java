package br.com.nandesk.market.domain;

import java.util.List;

/** Horário de funcionamento: {@code {"days":["MON","TUE"],"opens":"05:00","closes":"18:00"}} (10.3). */
public record OpeningHours(List<String> days, String opens, String closes) {}
