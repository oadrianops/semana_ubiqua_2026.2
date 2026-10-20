package br.com.nandesk.shared.security;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import java.util.Objects;

/** Segregação de funções (RN-11): ninguém modera o próprio envio nem registra repasse para si mesmo. */
public final class SegregationOfDuties {

    private SegregationOfDuties() {}

    public static void checkNotSelf(Long actorId, Long subjectId) {
        if (Objects.equals(actorId, subjectId)) {
            throw new BusinessException(ErrorCode.SELF_MODERATION_FORBIDDEN);
        }
    }
}
