package br.com.nandesk.search.domain;

import br.com.nandesk.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Registro anônimo de busca (sem IP e sem usuário) para os indicadores (RF-02, RF-32). */
@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog extends BaseEntity {

    @Column(name = "query_normalized", nullable = false, length = 120)
    private String queryNormalized;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "results_count", nullable = false)
    private int resultsCount;

    @Column(name = "market_filter", length = 80)
    private String marketFilter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SearchLog(String queryNormalized, String locale, int resultsCount, String marketFilter, Instant createdAt) {
        this.queryNormalized = queryNormalized.length() > 120 ? queryNormalized.substring(0, 120) : queryNormalized;
        this.locale = locale;
        this.resultsCount = resultsCount;
        this.marketFilter = marketFilter;
        this.createdAt = createdAt;
    }
}
