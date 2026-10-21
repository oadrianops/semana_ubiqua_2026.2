package br.com.nandesk.submission.domain;

import br.com.nandesk.shared.web.MoneyDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Changeset do envio (Anexo A): alterações propostas para o box e as ofertas, com os {@code fileId} das fotos que
 * já subiram uma a uma (RN-27). Moderado antes de chegar aos dados públicos (ADR-0012).
 */
public record ChangeSet(
        @NotBlank String taskType,
        @Valid StallChange stall,
        @Size(max = 60) List<@Valid OfferingChange> offerings,
        @NotNull @Size(max = 5) List<@Valid PhotoRef> photos,
        @Size(max = 1000) String notes,
        ReportVerification verification) {

    public List<OfferingChange> offeringsOrEmpty() {
        return offerings == null ? List.of() : offerings;
    }

    public List<PhotoRef> photosOrEmpty() {
        return photos == null ? List.of() : photos;
    }

    /** {@code publicId}/{@code baseVersion} em UPDATE_STALL e VERIFY_REPORT; os demais campos em NEW_STALL. */
    public record StallChange(
            String publicId,
            Integer baseVersion,
            Map<String, String> changes,
            String marketSlug,
            String sectorSlug,
            @Size(max = 20) String code,
            @Size(max = 120) String displayName,
            @Size(max = 120) String vendorName,
            Consent consent) {}

    public record Consent(boolean publish, boolean showVendorName, String method) {}

    public enum OfferingAction {
        ADD,
        UPDATE,
        CONFIRM,
        REMOVE,
        ADD_SUGGESTED
    }

    public record OfferingChange(
            @NotNull OfferingAction action,
            String productSlug,
            Integer baseVersion,
            MoneyDto referencePrice,
            String priceUnit,
            String availability,
            Suggestion suggestion) {}

    public record Suggestion(
            @NotBlank @Size(max = 120) String namePt,
            @NotBlank String categorySlug,
            String photoFileId) {}

    public record PhotoRef(
            @NotBlank String fileId, @NotBlank String role, String productSlug) {}

    /** Resultado da verificação de uma denúncia (VERIFY_REPORT). */
    public record ReportVerification(
            boolean problemConfirmed, @Size(max = 500) String comment) {}
}
