package br.com.nandesk.shared.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-22")
class SegregationOfDutiesTest {

    @Test
    @DisplayName("RN-11: ninguém modera o próprio envio")
    void selfModerationForbidden() {
        assertThatThrownBy(() -> SegregationOfDuties.checkNotSelf(7L, 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.SELF_MODERATION_FORBIDDEN);
        assertThatCode(() -> SegregationOfDuties.checkNotSelf(7L, 8L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RN-11: ninguém registra repasse para si mesmo")
    void selfPayoutForbidden() {
        assertThatThrownBy(() -> SegregationOfDuties.checkNotSelf(3L, 3L))
                .extracting(e -> ((BusinessException) e).code())
                .isEqualTo(ErrorCode.SELF_MODERATION_FORBIDDEN);
    }
}
