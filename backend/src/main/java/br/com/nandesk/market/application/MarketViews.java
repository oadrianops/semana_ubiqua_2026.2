package br.com.nandesk.market.application;

import br.com.nandesk.market.domain.OpeningHours;
import java.math.BigDecimal;
import java.util.List;

/** Respostas de leitura das feiras (records montados dentro da transação — ADR-0003). */
public final class MarketViews {

    private MarketViews() {}

    public record MarketSummary(
            String slug,
            String name,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int geofenceRadiusMeters,
            List<OpeningHours> openingHours,
            long publishedStalls,
            String coverUrl,
            String coverThumbUrl,
            String coverCredit,
            String coverCreditUrl) {}

    public record SectorView(String slug, String name, long stallCount) {}

    public record MarketDetail(
            String slug,
            String name,
            String description,
            String howToGetThere,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int geofenceRadiusMeters,
            List<OpeningHours> openingHours,
            long publishedStalls,
            String coverUrl,
            long dataVersion,
            List<SectorView> sectors,
            String coverCredit,
            String coverCreditUrl) {}

    public record MarketRef(String slug, String name) {}
}
