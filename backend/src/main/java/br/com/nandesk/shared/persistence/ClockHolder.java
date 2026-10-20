package br.com.nandesk.shared.persistence;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Ponte entre o bean {@link Clock} e os callbacks JPA ({@code @PrePersist}), que não recebem injeção.
 * Nos testes, o relógio fixo do contexto também vale para as datas de auditoria.
 */
@Component
public class ClockHolder {

    private static volatile Clock clock = Clock.systemUTC();

    public ClockHolder(Clock appClock) {
        setClock(appClock);
    }

    public static Instant now() {
        return Instant.now(clock);
    }

    static void setClock(Clock newClock) {
        clock = newClock;
    }
}
