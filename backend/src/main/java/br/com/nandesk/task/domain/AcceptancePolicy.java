package br.com.nandesk.task.domain;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.time.Duration;
import java.time.Instant;

/**
 * Regras do aceite e dos prazos da reserva: tarefas simultâneas (RN-03), limite diário de envios (RN-04),
 * reputação mínima (RN-17) e prazos de check-in e envio com aviso antes do vencimento (RN-02).
 */
public record AcceptancePolicy(
        int maxActive,
        int maxSubmissionsPerDay,
        int minReputation,
        Duration checkinDeadline,
        Duration submissionDeadline,
        Duration expiryWarning) {

    public void checkCanAccept(long activeReservations, long submissionsToday, int reputation) {
        if (reputation < minReputation) {
            throw new BusinessException(ErrorCode.REPUTATION_TOO_LOW);
        }
        if (activeReservations >= maxActive) {
            throw new BusinessException(ErrorCode.ACTIVE_TASK_LIMIT_REACHED);
        }
        checkCanSubmit(submissionsToday);
    }

    public void checkCanSubmit(long submissionsToday) {
        if (submissionsToday >= maxSubmissionsPerDay) {
            throw new BusinessException(ErrorCode.DAILY_SUBMISSION_LIMIT_REACHED);
        }
    }

    public Instant checkinDeadline(Instant acceptedAt) {
        return acceptedAt.plus(checkinDeadline);
    }

    public Instant submissionDeadline(Instant checkedInAt) {
        return checkedInAt.plus(submissionDeadline);
    }

    public boolean isExpired(Instant deadline, Instant now) {
        return now.isAfter(deadline);
    }

    public boolean shouldWarn(Instant deadline, Instant now) {
        return !now.isBefore(deadline.minus(expiryWarning)) && !isExpired(deadline, now);
    }
}
