package br.com.nandesk.market.api;

import br.com.nandesk.market.application.MarketQueryService;
import br.com.nandesk.market.domain.Market;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Feiras de interesse do colaborador: definem quem recebe as notificações de tarefas (RF-11, RF-14). */
@RestController
@RequestMapping("/api/v1/me/market-subscriptions")
@Tag(name = "Conta")
public class MarketSubscriptionController {

    private final UserAccountService users;
    private final MarketQueryService markets;

    public MarketSubscriptionController(UserAccountService users, MarketQueryService markets) {
        this.users = users;
        this.markets = markets;
    }

    public record SubscriptionsRequest(
            @NotNull @Size(max = 20) List<String> markets) {}

    public record SubscriptionsResponse(List<String> markets) {}

    @Operation(summary = "Minhas feiras de interesse")
    @GetMapping
    ResponseEntity<SubscriptionsResponse> get() {
        User user = users.require(CurrentUser.require().publicId());
        Set<Long> ids = users.subscribedMarketIds(user.getId());
        List<String> slugs = markets.listActiveEntities().stream()
                .filter(m -> ids.contains(m.getId()))
                .map(Market::getSlug)
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SubscriptionsResponse(slugs));
    }

    @Operation(summary = "Trocar minhas feiras de interesse")
    @PutMapping
    ResponseEntity<SubscriptionsResponse> put(@Valid @RequestBody SubscriptionsRequest body) {
        User user = users.require(CurrentUser.require().publicId());
        List<Market> selected = body.markets().stream()
                .map(slug -> markets.findActive(slug).orElseThrow(BusinessException::notFound))
                .toList();
        users.replaceSubscriptions(
                user.getId(), selected.stream().map(Market::getId).toList());
        return ResponseEntity.ok(
                new SubscriptionsResponse(selected.stream().map(Market::getSlug).toList()));
    }
}
