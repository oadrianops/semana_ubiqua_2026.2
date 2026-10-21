package br.com.nandesk.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PushEndpointPolicyTest {

    private final PushEndpointPolicy policy = new PushEndpointPolicy(List.of());

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://fcm.googleapis.com/fcm/send/abc",
                "https://updates.push.services.mozilla.com/wpush/v2/abc",
                "https://wns2-par02p.notify.windows.com/w/?token=abc",
                "https://web.push.apple.com/QJ_abc"
            })
    @DisplayName("RF-14: aceita os serviços de push dos navegadores")
    void acceptsBrowserPushServices(String endpoint) {
        assertThat(policy.allows(endpoint)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://fcm.googleapis.com/fcm/send/abc",
                "https://api:8091/actuator/prometheus",
                "https://169.254.169.254/latest/meta-data",
                "https://fcm.googleapis.com.evil.example/x",
                "https://user@fcm.googleapis.com/x",
                "file:///etc/passwd",
                "nao-e-url"
            })
    @DisplayName("12.7: recusa endereços internos, sem HTTPS ou de outros domínios (SSRF)")
    void rejectsEverythingElse(String endpoint) {
        assertThat(policy.allows(endpoint)).isFalse();
    }

    @Test
    @DisplayName("12.7: a lista pode ser trocada por configuração (ex.: servidor falso nos testes)")
    void configurable() {
        PushEndpointPolicy local = new PushEndpointPolicy(List.of("^http://localhost:\\d+/"));
        assertThat(local.allows("http://localhost:8099/push/1")).isTrue();
        assertThat(local.allows("https://fcm.googleapis.com/fcm/send/abc")).isFalse();
    }
}
