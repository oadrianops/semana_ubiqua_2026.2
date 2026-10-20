package br.com.nandesk.report.domain;

/** Denúncias acumuladas (RN-16): N denúncias na janela geram VERIFY_REPORT, se o box não tiver tarefa ativa. */
public record ReportThreshold(int threshold) {

    public boolean shouldCreateTask(long openReportsInWindow, boolean stallHasActiveTask) {
        return !stallHasActiveTask && openReportsInWindow >= threshold;
    }
}
