package br.com.nandesk.submission.domain;

import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.submission.domain.ChangeSet.OfferingAction;
import br.com.nandesk.submission.domain.ChangeSet.OfferingChange;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Validações do changeset (Anexo A): {@code baseVersion} em UPDATE/CONFIRM/REMOVE; consentimento obrigatório em
 * NEW_STALL; de {@code min} a {@code max} fotos (RN-06); ao menos uma foto do box em NEW_STALL; cada produto no
 * máximo uma vez.
 */
public final class ChangeSetValidator {

    private static final Set<OfferingAction> NEED_VERSION =
            EnumSet.of(OfferingAction.UPDATE, OfferingAction.CONFIRM, OfferingAction.REMOVE);

    private ChangeSetValidator() {}

    public static void validate(ChangeSet cs, String expectedTaskType, int minPhotos, int maxPhotos) {
        if (!expectedTaskType.equals(cs.taskType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        int photos = cs.photosOrEmpty().size();
        if (photos < minPhotos || photos > maxPhotos) {
            throw new BusinessException(ErrorCode.INVALID_PHOTO);
        }
        if ("NEW_STALL".equals(cs.taskType())) {
            if (cs.stall() == null
                    || cs.stall().consent() == null
                    || !cs.stall().consent().publish()) {
                throw new BusinessException(ErrorCode.CONSENT_REQUIRED);
            }
            if (cs.stall().code() == null || cs.stall().code().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            boolean hasStallPhoto = cs.photosOrEmpty().stream()
                    .anyMatch(p -> PhotoRole.STALL.name().equals(p.role()));
            if (!hasStallPhoto) {
                throw new BusinessException(ErrorCode.INVALID_PHOTO);
            }
        }
        Set<String> seen = new HashSet<>();
        for (OfferingChange o : cs.offeringsOrEmpty()) {
            if (NEED_VERSION.contains(o.action()) && o.baseVersion() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            if (o.action() == OfferingAction.ADD_SUGGESTED) {
                if (o.suggestion() == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                continue;
            }
            if (o.productSlug() == null || !seen.add(o.productSlug())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }
}
