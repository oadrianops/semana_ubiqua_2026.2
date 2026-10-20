package br.com.nandesk.task.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-17")
class CheckInPolicyTest {

    // Centro aproximado do Ver-o-Peso, raio de 250 m (seed).
    private static final double LAT = -1.4525;
    private static final double LNG = -48.5035;
    private final CheckInPolicy policy = new CheckInPolicy(100);

    @Test
    @DisplayName("RN-05: aceita check-in dentro do raio mais a tolerância de precisão")
    void insideRadius() {
        CheckInPolicy.Result result = policy.evaluate(LAT, LNG, 250, -1.45261, -48.50331, 35);
        assertThat(result.accepted()).isTrue();
        assertThat(result.flag()).isNull();
        assertThat(result.distanceMeters()).isLessThan(250);
    }

    @Test
    @DisplayName("RN-05: recusa check-in a 2 km da feira com precisão de 20 m")
    void twoKilometersAway() {
        // ~2 km ao sul do centro da feira.
        CheckInPolicy.Result result = policy.evaluate(LAT, LNG, 250, LAT - 0.018, LNG, 20);
        assertThat(result.accepted()).isFalse();
        assertThat(result.distanceMeters()).isGreaterThan(1900);
    }

    @Test
    @DisplayName("RN-05: a 300 m com precisão de 180 m aceita e marca LOW_ACCURACY")
    void lowAccuracyAccepted() {
        // ~300 m do centro: 250 + min(180, 100) = 350 m de limite.
        CheckInPolicy.Result result = policy.evaluate(LAT, LNG, 250, LAT - 0.0027, LNG, 180);
        assertThat(result.distanceMeters()).isBetween(290, 310);
        assertThat(result.accepted()).isTrue();
        assertThat(result.flag()).isEqualTo(CheckinFlag.LOW_ACCURACY);
    }

    @Test
    @DisplayName("RN-05: a tolerância de precisão é limitada a 100 m")
    void toleranceIsCapped() {
        // ~400 m do centro, precisão de 500 m: limite = 250 + 100 = 350 m → recusado.
        CheckInPolicy.Result result = policy.evaluate(LAT, LNG, 250, LAT - 0.0036, LNG, 500);
        assertThat(result.accepted()).isFalse();
    }

    @Test
    @DisplayName("RN-05: precisão exatamente 100 m não marca baixa precisão")
    void accuracyAtLimit() {
        CheckInPolicy.Result result = policy.evaluate(LAT, LNG, 250, LAT, LNG, 100);
        assertThat(result.accepted()).isTrue();
        assertThat(result.flag()).isNull();
    }
}
