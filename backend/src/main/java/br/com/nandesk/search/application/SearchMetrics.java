package br.com.nandesk.search.application;

import br.com.nandesk.search.infra.SearchLogRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indicadores de busca para o painel (RF-32) e retenção dos registros (180 d — 13.1). */
@Service
public class SearchMetrics {

    private final SearchLogRepository logs;
    private final Clock clock;

    public SearchMetrics(SearchLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public long countSince(Instant since) {
        return logs.countByCreatedAtAfter(since);
    }

    @Transactional(readOnly = true)
    public long countWithResultsSince(Instant since) {
        return logs.countWithResultsSince(since);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topZeroResultSince(Instant since, int limit) {
        return logs.topZeroResultSince(since, PageRequest.of(0, limit)).stream()
                .map(r -> Map.<String, Object>of("term", r[0], "count", r[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> byLocaleSince(Instant since) {
        Map<String, Long> map = new LinkedHashMap<>();
        logs.countByLocaleSince(since).forEach(r -> map.put((String) r[0], ((Number) r[1]).longValue()));
        return map;
    }

    @Transactional
    public int purgeOlderThan(Duration age) {
        return logs.deleteOlderThan(clock.instant().minus(age));
    }
}
