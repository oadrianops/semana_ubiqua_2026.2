package br.com.nandesk.search.api;

import br.com.nandesk.search.application.SearchLogService;
import br.com.nandesk.search.application.SearchService;
import br.com.nandesk.search.application.SearchViews.SearchResult;
import br.com.nandesk.search.application.SearchViews.Suggestion;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.i18n.AppLocale;
import br.com.nandesk.shared.web.HttpCaching;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Busca de produtos com feira, setor e box (RF-02). */
@RestController
@RequestMapping("/api/v1/search")
@Validated
@Tag(name = "Busca")
public class SearchController {

    private final SearchService search;
    private final SearchLogService logs;
    private final AppProperties properties;
    private final MeterRegistry meters;

    public SearchController(
            SearchService search, SearchLogService logs, AppProperties properties, MeterRegistry meters) {
        this.search = search;
        this.logs = logs;
        this.properties = properties;
        this.meters = meters;
    }

    @Operation(summary = "Buscar produto e ver em qual feira, setor e box encontrar (RF-02)")
    @GetMapping
    ResponseEntity<SearchResult> search(
            @RequestParam @Size(max = 120) String q,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppLocale locale = AppLocale.current();
        Timer.Sample sample = Timer.start(meters);
        SearchResult result =
                search.search(new SearchService.Query(q, market, category, availableOnly, page, size), locale);
        sample.stop(meters.timer("nandesk.search.latency"));
        if (page == 0) {
            logs.record(q, locale.tag(), result.totalItems(), market);
        }
        return HttpCaching.publicResponse(properties.http().publicCacheControl(), locale.tag(), result);
    }

    @Operation(summary = "Sugestões enquanto digita (RF-02)")
    @GetMapping("/suggestions")
    ResponseEntity<List<Suggestion>> suggestions(@RequestParam @Size(max = 120) String q) {
        AppLocale locale = AppLocale.current();
        return HttpCaching.publicResponse(
                properties.http().publicCacheControl(), locale.tag(), search.suggestions(q, locale));
    }
}
