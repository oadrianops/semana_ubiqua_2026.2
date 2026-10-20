package br.com.nandesk.report.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-07")
class ReportThresholdTest {

    private final ReportThreshold threshold = new ReportThreshold(3);

    @Test
    @DisplayName("RN-16: 3 denúncias abertas na janela geram tarefa de verificação")
    void createsTaskAtThreshold() {
        assertThat(threshold.shouldCreateTask(3, false)).isTrue();
        assertThat(threshold.shouldCreateTask(2, false)).isFalse();
    }

    @Test
    @DisplayName("RN-16 e RN-25: com tarefa ativa no box, não cria outra")
    void respectsActiveTask() {
        assertThat(threshold.shouldCreateTask(5, true)).isFalse();
    }
}
