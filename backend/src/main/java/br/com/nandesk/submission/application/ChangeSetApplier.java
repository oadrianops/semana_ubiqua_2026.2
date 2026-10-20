package br.com.nandesk.submission.application;

import br.com.nandesk.catalog.application.CatalogAdminService;
import br.com.nandesk.catalog.application.CatalogQueryService;
import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.file.domain.FileVisibility;
import br.com.nandesk.file.domain.StoredFile;
import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.market.domain.Sector;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.stall.application.StallAdminService;
import br.com.nandesk.stall.application.StallAdminService.OfferingCommand;
import br.com.nandesk.stall.domain.Availability;
import br.com.nandesk.stall.domain.ConsentMethod;
import br.com.nandesk.stall.domain.Stall;
import br.com.nandesk.submission.domain.ChangeSet;
import br.com.nandesk.submission.domain.ChangeSet.OfferingChange;
import br.com.nandesk.submission.domain.PhotoRole;
import br.com.nandesk.submission.domain.Submission;
import br.com.nandesk.submission.domain.SubmissionPhoto;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.task.domain.UpdateTask;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica um changeset aprovado aos dados públicos numa única transação (RF-22): box, ofertas, foto do box e data
 * de verificação. Sugestões de produto entram como PENDING (RN-18) e só aparecem depois de aprovadas.
 */
@Component
public class ChangeSetApplier {

    private final StallAdminService stalls;
    private final MarketQueryService markets;
    private final CatalogQueryService catalog;
    private final CatalogAdminService catalogAdmin;
    private final AppProperties properties;
    private final Clock clock;

    public ChangeSetApplier(
            StallAdminService stalls,
            MarketQueryService markets,
            CatalogQueryService catalog,
            CatalogAdminService catalogAdmin,
            AppProperties properties,
            Clock clock) {
        this.stalls = stalls;
        this.markets = markets;
        this.catalog = catalog;
        this.catalogAdmin = catalogAdmin;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Stall apply(Submission submission, ChangeSet cs) {
        UpdateTask task = submission.getTask();
        Instant now = clock.instant();
        Stall stall = task.getType() == TaskType.NEW_STALL ? createStall(submission, cs) : task.getStall();
        if (task.getType() != TaskType.NEW_STALL
                && cs.stall() != null
                && cs.stall().changes() != null) {
            applyStallChanges(stall, cs.stall().changes());
        }
        Map<String, StoredFile> suggestionPhotos = new java.util.HashMap<>();
        submission.getPhotos().stream()
                .filter(p -> p.getRole() == PhotoRole.SUGGESTION)
                .forEach(p -> suggestionPhotos.put(p.getFile().getPublicId().toString(), p.getFile()));
        for (OfferingChange change : cs.offeringsOrEmpty()) {
            applyOffering(stall, change, submission, suggestionPhotos, now);
        }
        for (SubmissionPhoto photo : submission.getPhotos()) {
            StoredFile file = photo.getFile();
            if (photo.getRole() == PhotoRole.STALL) {
                file.setVisibility(FileVisibility.PUBLIC);
                file.setDeleteAfter(null);
                stalls.setPhoto(stall, file);
            } else if (photo.getRole() == PhotoRole.SUGGESTION) {
                file.setDeleteAfter(null);
            } else {
                file.setDeleteAfter(now.plus(properties.retention().evidencePhotos()));
            }
        }
        stalls.markVerified(stall, now);
        return stall;
    }

    private Stall createStall(Submission submission, ChangeSet cs) {
        ChangeSet.StallChange s = cs.stall();
        Market market = submission.getTask().getMarket();
        if (s.marketSlug() != null && !s.marketSlug().equals(market.getSlug())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Sector sector = s.sectorSlug() == null
                ? submission.getTask().getSuggestedSector()
                : markets.sector(market, s.sectorSlug()).orElse(null);
        ConsentMethod method = s.consent().method() == null
                ? ConsentMethod.VERBAL
                : ConsentMethod.valueOf(s.consent().method());
        return stalls.createStall(
                market,
                sector,
                s.code().trim(),
                s.displayName(),
                s.vendorName(),
                s.consent().showVendorName(),
                method,
                submission.getCollaborator());
    }

    private void applyStallChanges(Stall stall, Map<String, String> changes) {
        if (changes.containsKey("displayName")) {
            stall.setDisplayName(changes.get("displayName"));
        }
        if (changes.containsKey("sectorSlug")) {
            markets.sector(stall.getMarket(), changes.get("sectorSlug")).ifPresent(stall::setSector);
        }
    }

    private void applyOffering(
            Stall stall,
            OfferingChange change,
            Submission submission,
            Map<String, StoredFile> suggestionPhotos,
            Instant now) {
        switch (change.action()) {
            case ADD, UPDATE -> stalls.putOffering(stall, command(product(change.productSlug()), change), now);
            case CONFIRM -> stalls.confirmOffering(stall, product(change.productSlug()), now);
            case REMOVE -> stalls.removeOffering(stall, product(change.productSlug()));
            case ADD_SUGGESTED -> {
                ChangeSet.Suggestion suggestion = change.suggestion();
                StoredFile photo =
                        suggestion.photoFileId() == null ? null : suggestionPhotos.get(suggestion.photoFileId());
                Product pending = catalogAdmin.suggest(
                        suggestion.namePt(), suggestion.categorySlug(), submission.getCollaborator(), photo);
                stalls.putOffering(stall, command(pending, change), now);
            }
        }
    }

    private Product product(String slug) {
        return catalog.findBySlug(slug).orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR));
    }

    private static OfferingCommand command(Product product, OfferingChange change) {
        BigDecimal price =
                change.referencePrice() == null ? null : change.referencePrice().toBigDecimal();
        PriceUnit unit = change.priceUnit() == null ? product.getDefaultUnit() : PriceUnit.valueOf(change.priceUnit());
        Availability availability =
                change.availability() == null ? Availability.AVAILABLE : Availability.valueOf(change.availability());
        return new OfferingCommand(product, price, unit, availability);
    }
}
