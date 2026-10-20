package br.com.nandesk.stall.application;

import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.stall.application.StallAdminService.StallChanged;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Toda mudança publicada num box incrementa {@code markets.data_version} na mesma transação, para o ETag do
 * snapshot nunca ficar atrás dos dados (10.3, RF-36).
 */
@Component
public class StallDataVersionListener {

    private final MarketQueryService markets;

    public StallDataVersionListener(MarketQueryService markets) {
        this.markets = markets;
    }

    @EventListener
    public void onStallChanged(StallChanged event) {
        markets.bumpDataVersion(event.marketId());
    }
}
