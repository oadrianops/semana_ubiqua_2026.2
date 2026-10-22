package br.com.nandesk.search.application;

import br.com.nandesk.catalog.domain.PriceUnit;
import br.com.nandesk.market.application.MarketViews.MarketDetail;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.stall.domain.Availability;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot compacto de uma feira para uso sem conexão (RF-36, seção 9.13): setores, boxes publicados, ofertas,
 * produtos no idioma pedido com os termos normalizados para a busca local. Orçamento: ≤ 150 KB com gzip.
 */
public final class SnapshotViews {

    private SnapshotViews() {}

    public record MarketSnapshot(
            MarketDetail market,
            long dataVersion,
            String locale,
            Instant generatedAt,
            List<SnapshotCategory> categories,
            List<SnapshotProduct> products,
            List<SnapshotStall> stalls) {}

    public record SnapshotCategory(String slug, String name) {}

    /** {@code terms}: nomes e sinônimos normalizados nos três idiomas, para a busca local. */
    public record SnapshotProduct(String slug, String name, String categorySlug, String imageUrl, List<String> terms) {}

    public record SnapshotOffering(
            String productSlug,
            MoneyDto price,
            PriceUnit priceUnit,
            Availability availability,
            Instant lastVerifiedAt) {}

    public record SnapshotStall(
            UUID publicId,
            String code,
            String name,
            String vendorName,
            String sectorSlug,
            String sectorName,
            String photoThumbUrl,
            Instant lastVerifiedAt,
            boolean outdated,
            List<SnapshotOffering> offerings,
            boolean demo) {}
}
