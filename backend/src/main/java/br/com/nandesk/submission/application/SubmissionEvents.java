package br.com.nandesk.submission.application;

import java.math.BigDecimal;
import java.util.UUID;

/** Eventos dos envios, consumidos depois do commit (notificação do colaborador — RF-14). */
public final class SubmissionEvents {

    private SubmissionEvents() {}

    public record SubmissionReviewed(
            Long collaboratorId,
            UUID submissionId,
            UUID taskId,
            boolean approved,
            BigDecimal commission,
            String reason) {}
}
