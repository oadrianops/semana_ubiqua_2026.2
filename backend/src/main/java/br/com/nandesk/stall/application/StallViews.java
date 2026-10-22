package br.com.nandesk.stall.application;

import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.market.application.MarketViews.MarketRef;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.stall.domain.Availability;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Respostas de leitura dos boxes (RF-03). */
public final class StallViews {

    private StallViews() {}

    public record SectorRef(String slug, String name) {}

    /** Foto da galeria do box com as duas variantes (miniatura de 400 px e completa). */
    public record PhotoView(String url, String thumbUrl, int width, int height) {}

    public record StallSummary(
            UUID publicId,
            String code,
            String name,
            SectorRef sector,
            String photoThumbUrl,
            Instant lastVerifiedAt,
            boolean outdated,
            int offeringCount,
            boolean demo) {}

    public record OfferingView(
            UUID publicId,
            String productSlug,
            String productName,
            String categorySlug,
            MoneyDto price,
            PriceUnit priceUnit,
            Availability availability,
            Instant lastVerifiedAt,
            int version) {}

    public record StallDetail(
            UUID publicId,
            String code,
            String name,
            String vendorName,
            MarketRef market,
            SectorRef sector,
            String photoUrl,
            String photoThumbUrl,
            Instant lastVerifiedAt,
            boolean outdated,
            int version,
            List<OfferingView> offerings,
            boolean demo,
            String instagram,
            List<PhotoView> gallery) {}
}
