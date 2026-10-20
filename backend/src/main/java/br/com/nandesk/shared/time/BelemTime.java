package br.com.nandesk.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Dia civil e horários no fuso de Belém (RN-23); datas sempre gravadas como {@link Instant} em UTC. */
public final class BelemTime {

    public static final ZoneId ZONE = ZoneId.of("America/Belem");

    private BelemTime() {}

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public static Instant startOfDay(LocalDate day) {
        return day.atStartOfDay(ZONE).toInstant();
    }

    public static Instant startOfNextDay(LocalDate day) {
        return day.plusDays(1).atStartOfDay(ZONE).toInstant();
    }
}
