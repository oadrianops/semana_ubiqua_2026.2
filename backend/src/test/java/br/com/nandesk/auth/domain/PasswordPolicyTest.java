package br.com.nandesk.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-08")
@Tag("RNF-04")
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(Set.of("password", "12345678", "qwerty123"));

    @Test
    @DisplayName("12.1: mínimo de 8 e máximo de 128 caracteres")
    void length() {
        assertThat(policy.check("curta12")).contains(PasswordPolicy.Violation.TOO_SHORT);
        assertThat(policy.check("a".repeat(129))).contains(PasswordPolicy.Violation.TOO_LONG);
        assertThat(policy.check("castanha do para no box 12")).isEmpty();
    }

    @Test
    @DisplayName("12.1: bloqueia senhas comuns, sem diferenciar maiúsculas")
    void common() {
        assertThat(policy.check("PASSWORD")).contains(PasswordPolicy.Violation.TOO_COMMON);
        assertThat(policy.check("12345678")).contains(PasswordPolicy.Violation.TOO_COMMON);
    }

    @Test
    @DisplayName("12.1: sem regras forçadas de composição (NIST SP 800-63B)")
    void noCompositionRules() {
        assertThat(policy.check("frasecomprida")).isEmpty();
    }

    @Test
    @DisplayName("12.1: espera progressiva depois de 5 falhas (1, 2, 4… min)")
    void progressiveLockout() {
        assertThat(LoginLockout.lockMinutes(4, 5)).isZero();
        assertThat(LoginLockout.lockMinutes(5, 5)).isEqualTo(1);
        assertThat(LoginLockout.lockMinutes(6, 5)).isEqualTo(2);
        assertThat(LoginLockout.lockMinutes(7, 5)).isEqualTo(4);
        assertThat(LoginLockout.lockMinutes(30, 5)).isEqualTo(60);
    }
}
