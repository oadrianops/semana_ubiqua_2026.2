package br.com.nandesk.submission.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.nandesk.submission.domain.ChangeSet.OfferingAction;
import br.com.nandesk.submission.domain.ChangeSet.OfferingChange;
import br.com.nandesk.submission.domain.ChangeSet.StallChange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("RF-22")
class ChangeSetConflictTest {

    @Test
    @DisplayName("RN-26: versão do box diferente da registrada no changeset é conflito")
    void stallVersionChanged() {
        ChangeSet cs = updateStall(7, List.of());
        List<String> conflicts = ChangeSetConflicts.detect(cs, 8, Map.of());
        assertThat(conflicts).containsExactly("stall");
    }

    @Test
    @DisplayName("RN-26: oferta alterada depois do envio é conflito campo a campo")
    void offeringVersionChanged() {
        ChangeSet cs = updateStall(
                7,
                List.of(
                        new OfferingChange(OfferingAction.UPDATE, "castanha-do-para", 3, null, "KG", "AVAILABLE", null),
                        new OfferingChange(OfferingAction.CONFIRM, "acai-polpa", 5, null, null, null, null)));
        List<String> conflicts = ChangeSetConflicts.detect(cs, 7, Map.of("castanha-do-para", 4, "acai-polpa", 5));
        assertThat(conflicts).containsExactly("offering:castanha-do-para");
    }

    @Test
    @DisplayName("RN-26: sem mudanças desde o envio, não há conflito")
    void noConflict() {
        ChangeSet cs = updateStall(
                7,
                List.of(
                        new OfferingChange(OfferingAction.REMOVE, "tucupi", 2, null, null, null, null),
                        new OfferingChange(OfferingAction.ADD, "cumaru", null, null, "PACKAGE", "AVAILABLE", null)));
        assertThat(ChangeSetConflicts.detect(cs, 7, Map.of("tucupi", 2))).isEmpty();
    }

    @Test
    @DisplayName("RN-26: oferta removida por outra aprovação também é conflito")
    void offeringGone() {
        ChangeSet cs = updateStall(
                7, List.of(new OfferingChange(OfferingAction.UPDATE, "tucupi", 2, null, "BOTTLE", "AVAILABLE", null)));
        assertThat(ChangeSetConflicts.detect(cs, 7, Map.of())).containsExactly("offering:tucupi");
    }

    private static ChangeSet updateStall(int baseVersion, List<OfferingChange> offerings) {
        return new ChangeSet(
                "UPDATE_STALL",
                new StallChange(
                        "8b1e2c4d-5f60-4a71-8b92-a3b4c5d6e7f8", baseVersion, null, null, null, null, null, null, null),
                offerings,
                List.of(),
                null,
                null);
    }
}
