package br.com.nandesk.stall.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-03")
class FreshnessPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private final FreshnessPolicy policy = new FreshnessPolicy(Duration.ofDays(30), Duration.ofDays(60));

    @Test
    @DisplayName("RN-12: box verificado há 61 dias mostra o aviso de possível desatualização")
    void outdatedAfterSixtyDays() {
        assertThat(policy.isOutdated(NOW.minus(Duration.ofDays(61)), NOW)).isTrue();
    }

    @Test
    @DisplayName("RN-12: box verificado há 60 dias ainda não mostra o aviso")
    void notOutdatedAtSixtyDays() {
        assertThat(policy.isOutdated(NOW.minus(Duration.ofDays(60)), NOW)).isFalse();
    }

    @Test
    @DisplayName("RN-12: box sem verificação conta como desatualizado")
    void neverVerified() {
        assertThat(policy.isOutdated(null, NOW)).isTrue();
        assertThat(policy.needsUpdateTask(null, NOW)).isTrue();
    }

    @Test
    @DisplayName("RN-12: mais de 30 dias sem verificação pede tarefa automática de atualização")
    void staleAfterThirtyDays() {
        assertThat(policy.needsUpdateTask(NOW.minus(Duration.ofDays(31)), NOW)).isTrue();
        assertThat(policy.needsUpdateTask(NOW.minus(Duration.ofDays(29)), NOW)).isFalse();
    }

    @Test
    @DisplayName("RN-12: dias desde a verificação para o texto \"verificado há N dias\"")
    void daysSince() {
        assertThat(policy.daysSince(NOW.minus(Duration.ofHours(49)), NOW)).isEqualTo(2);
    }
}
