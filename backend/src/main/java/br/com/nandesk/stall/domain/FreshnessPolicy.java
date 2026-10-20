package br.com.nandesk.stall.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Frescor dos dados (RN-12): acima de {@code staleAfter} sem verificação o box ganha tarefa automática; acima de
 * {@code outdatedAfter} a página mostra o aviso de possível desatualização.
 */
public record FreshnessPolicy(Duration staleAfter, Duration outdatedAfter) {

    public boolean isOutdated(Instant lastVerifiedAt, Instant now) {
        return lastVerifiedAt == null || Duration.between(lastVerifiedAt, now).compareTo(outdatedAfter) > 0;
    }

    public boolean needsUpdateTask(Instant lastVerifiedAt, Instant now) {
        return lastVerifiedAt == null || Duration.between(lastVerifiedAt, now).compareTo(staleAfter) > 0;
    }

    public long daysSince(Instant lastVerifiedAt, Instant now) {
        return lastVerifiedAt == null
                ? -1
                : Duration.between(lastVerifiedAt, now).toDays();
    }
}
