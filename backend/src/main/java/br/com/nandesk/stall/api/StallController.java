package br.com.nandesk.stall.api;

import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.HttpCaching;
import br.com.nandesk.stall.application.StallQueryService;
import br.com.nandesk.stall.application.StallViews.StallDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Página pública do box (RF-03). */
@RestController
@RequestMapping("/api/v1/stalls")
@Tag(name = "Vitrine pública")
public class StallController {

    private final StallQueryService stalls;
    private final AppProperties properties;

    public StallController(StallQueryService stalls, AppProperties properties) {
        this.stalls = stalls;
        this.properties = properties;
    }

    @Operation(summary = "Box com ofertas (RF-03)")
    @GetMapping("/{publicId}")
    ResponseEntity<StallDetail> stall(@PathVariable UUID publicId) {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(
                properties.http().publicCacheControl(), locale.tag(), stalls.publicStall(publicId, locale));
    }
}
