package br.com.nandesk.stall.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-03")
@Tag("RF-29")
class StallConsentTest {

    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");

    @Test
    @DisplayName("RN-13: box ativo com consentimento concedido é público")
    void publicWithConsent() {
        Stall stall = new Stall(null, null, "12", "Empório", ConsentMethod.VERBAL, null, NOW);
        assertThat(stall.isPublic()).isTrue();
    }

    @Test
    @DisplayName("RN-13: a revogação oculta o box imediatamente")
    void revocationHides() {
        Stall stall = new Stall(null, null, "12", "Empório", ConsentMethod.SIGNED_FORM, null, NOW);
        stall.revokeConsent(NOW);
        assertThat(stall.isPublic()).isFalse();
        assertThat(stall.getConsentRevokedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("RN-13: box inativo não é público")
    void inactiveHidden() {
        Stall stall = new Stall(null, null, "12", "Empório", ConsentMethod.VERBAL, null, NOW);
        stall.setStatus(StallStatus.INACTIVE);
        assertThat(stall.isPublic()).isFalse();
    }

    @Test
    @DisplayName("RN-14: o nome do feirante só aparece com consentimento específico")
    void vendorNameNeedsConsent() {
        Stall stall = new Stall(null, null, "12", "Empório", ConsentMethod.VERBAL, null, NOW);
        stall.setVendorName("Maria");
        assertThat(stall.publicVendorName()).isNull();
        stall.setShowVendorName(true);
        assertThat(stall.publicVendorName()).isEqualTo("Maria");
    }
}
