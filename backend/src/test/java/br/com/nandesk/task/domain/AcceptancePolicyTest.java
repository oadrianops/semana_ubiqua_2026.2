package br.com.nandesk.task.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-16")
class AcceptancePolicyTest {

    private final AcceptancePolicy policy =
            new AcceptancePolicy(2, 15, 40, Duration.ofHours(2), Duration.ofHours(1), Duration.ofMinutes(15));

    @Test
    @DisplayName("RN-03: com 2 tarefas aceitas não aceita a terceira")
    void maxActive() {
        assertThatThrownBy(() -> policy.checkCanAccept(2, 0, 80))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.ACTIVE_TASK_LIMIT_REACHED);
        assertThatCode(() -> policy.checkCanAccept(1, 0, 80)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN-04: com 15 envios no dia não aceita nova tarefa")
    void dailyLimit() {
        assertThatThrownBy(() -> policy.checkCanAccept(0, 15, 80))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.DAILY_SUBMISSION_LIMIT_REACHED);
        assertThatThrownBy(() -> policy.checkCanSubmit(15))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.DAILY_SUBMISSION_LIMIT_REACHED);
        assertThatCode(() -> policy.checkCanSubmit(14)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN-17: reputação abaixo de 40 impede aceitar")
    void reputationTooLow() {
        assertThatThrownBy(() -> policy.checkCanAccept(0, 0, 39))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.REPUTATION_TOO_LOW);
        assertThatCode(() -> policy.checkCanAccept(0, 0, 40)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN-02: prazo de check-in de 2 h após o aceite e de envio de 1 h após o check-in")
    void deadlines() {
        Instant accepted = Instant.parse("2026-10-20T13:00:00Z");
        assertThat(policy.checkinDeadline(accepted)).isEqualTo(Instant.parse("2026-10-20T15:00:00Z"));
        assertThat(policy.submissionDeadline(accepted)).isEqualTo(Instant.parse("2026-10-20T14:00:00Z"));
    }

    @Test
    @DisplayName("RN-02: aviso 15 min antes do vencimento")
    void expiryWarning() {
        Instant deadline = Instant.parse("2026-10-20T15:00:00Z");
        assertThat(policy.shouldWarn(deadline, Instant.parse("2026-10-20T14:44:00Z")))
                .isFalse();
        assertThat(policy.shouldWarn(deadline, Instant.parse("2026-10-20T14:46:00Z")))
                .isTrue();
        assertThat(policy.isExpired(deadline, Instant.parse("2026-10-20T15:00:01Z")))
                .isTrue();
        assertThat(policy.isExpired(deadline, Instant.parse("2026-10-20T14:59:59Z")))
                .isFalse();
    }
}
