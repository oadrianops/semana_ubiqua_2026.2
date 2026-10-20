package br.com.nandesk.submission.domain;

import br.com.nandesk.submission.domain.ChangeSet.OfferingAction;
import br.com.nandesk.submission.domain.ChangeSet.OfferingChange;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conflito na aprovação (RN-26): se o box ou uma oferta mudou desde o envio (versão diferente da registrada no
 * changeset), o moderador vê o conflito campo a campo e decide.
 */
public final class ChangeSetConflicts {

    private static final Set<OfferingAction> VERSIONED =
            EnumSet.of(OfferingAction.UPDATE, OfferingAction.CONFIRM, OfferingAction.REMOVE);

    private ChangeSetConflicts() {}

    /**
     * @param currentStallVersion versão atual do box (ignorada em NEW_STALL)
     * @param currentOfferingVersions versões atuais das ofertas do box, por slug do produto
     * @return lista de itens em conflito: {@code "stall"} e {@code "offering:<slug>"}
     */
    public static List<String> detect(
            ChangeSet changeSet, int currentStallVersion, Map<String, Integer> currentOfferingVersions) {
        List<String> conflicts = new ArrayList<>();
        if (changeSet.stall() != null
                && changeSet.stall().baseVersion() != null
                && changeSet.stall().baseVersion() != currentStallVersion) {
            conflicts.add("stall");
        }
        for (OfferingChange o : changeSet.offeringsOrEmpty()) {
            if (!VERSIONED.contains(o.action())) {
                continue;
            }
            Integer current = currentOfferingVersions.get(o.productSlug());
            if (current == null || !current.equals(o.baseVersion())) {
                conflicts.add("offering:" + o.productSlug());
            }
        }
        return conflicts;
    }
}
