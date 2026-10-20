package br.com.nandesk.task.domain;

import static br.com.nandesk.task.domain.TaskStatus.ACCEPTED;
import static br.com.nandesk.task.domain.TaskStatus.CANCELLED;
import static br.com.nandesk.task.domain.TaskStatus.CHECKED_IN;
import static br.com.nandesk.task.domain.TaskStatus.COMPLETED;
import static br.com.nandesk.task.domain.TaskStatus.ON_HOLD;
import static br.com.nandesk.task.domain.TaskStatus.OPEN;
import static br.com.nandesk.task.domain.TaskStatus.SUBMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("RF-16")
@Tag("RF-19")
class TaskStateMachineTest {

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(ON_HOLD, OPEN),
                Arguments.of(ON_HOLD, CANCELLED),
                Arguments.of(OPEN, ACCEPTED),
                Arguments.of(OPEN, CANCELLED),
                Arguments.of(ACCEPTED, OPEN),
                Arguments.of(ACCEPTED, CHECKED_IN),
                Arguments.of(CHECKED_IN, OPEN),
                Arguments.of(CHECKED_IN, SUBMITTED),
                Arguments.of(SUBMITTED, COMPLETED),
                Arguments.of(SUBMITTED, OPEN),
                Arguments.of(SUBMITTED, CANCELLED));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("validTransitions")
    @DisplayName("9.7: transições válidas da tarefa são permitidas")
    void allowsValidTransitions(TaskStatus from, TaskStatus to) {
        assertThat(TaskStateMachine.canTransition(from, to)).isTrue();
        TaskStateMachine.require(from, to);
    }

    @Test
    @DisplayName("9.7: qualquer outra transição lança TASK_NOT_AVAILABLE")
    void rejectsEveryOtherTransition() {
        Set<String> valid = Set.of(
                "ON_HOLD>OPEN",
                "ON_HOLD>CANCELLED",
                "OPEN>ACCEPTED",
                "OPEN>CANCELLED",
                "ACCEPTED>OPEN",
                "ACCEPTED>CHECKED_IN",
                "CHECKED_IN>OPEN",
                "CHECKED_IN>SUBMITTED",
                "SUBMITTED>COMPLETED",
                "SUBMITTED>OPEN",
                "SUBMITTED>CANCELLED");
        for (TaskStatus from : EnumSet.allOf(TaskStatus.class)) {
            for (TaskStatus to : EnumSet.allOf(TaskStatus.class)) {
                if (valid.contains(from + ">" + to)) {
                    continue;
                }
                assertThat(TaskStateMachine.canTransition(from, to))
                        .as(from + " -> " + to)
                        .isFalse();
                assertThatThrownBy(() -> TaskStateMachine.require(from, to))
                        .isInstanceOf(BusinessException.class)
                        .extracting(e -> ((BusinessException) e).code())
                        .isEqualTo(ErrorCode.TASK_NOT_AVAILABLE);
            }
        }
    }

    @Test
    @DisplayName("RN-25: estados que contam como tarefa ativa do box")
    void activeStatuses() {
        assertThat(TaskStatus.ACTIVE).containsExactlyInAnyOrder(ON_HOLD, OPEN, ACCEPTED, CHECKED_IN, SUBMITTED);
        assertThat(COMPLETED.isActive()).isFalse();
        assertThat(CANCELLED.isActive()).isFalse();
    }
}
