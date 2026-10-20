package br.com.nandesk.search.application;

import br.com.nandesk.catalog.domain.SearchNormalizer;
import br.com.nandesk.search.domain.SearchLog;
import br.com.nandesk.search.infra.SearchLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro anônimo das buscas (termo normalizado, idioma, nº de resultados — sem IP e sem usuário — RF-02). */
@Service
public class SearchLogService {

    private final SearchLogRepository logs;
    private final MeterRegistry meters;
    private final Clock clock;

    public SearchLogService(SearchLogRepository logs, MeterRegistry meters, Clock clock) {
        this.logs = logs;
        this.meters = meters;
        this.clock = clock;
    }

    @Transactional
    public void record(String query, String locale, long results, String marketFilter) {
        String normalized = SearchNormalizer.normalize(query);
        if (normalized.isEmpty()) {
            return;
        }
        logs.save(new SearchLog(normalized, locale, (int) results, marketFilter, clock.instant()));
        meters.counter("nandesk.search.requests", "locale", locale, "result", results > 0 ? "hit" : "miss")
                .increment();
    }
}
