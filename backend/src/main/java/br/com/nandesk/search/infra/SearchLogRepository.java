package br.com.nandesk.search.infra;

import br.com.nandesk.search.domain.SearchLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    long countByCreatedAtAfter(Instant since);

    @Query("select l.locale, count(l) from SearchLog l where l.createdAt >= :since group by l.locale")
    List<Object[]> countByLocaleSince(@Param("since") Instant since);

    @Query("select l.queryNormalized, count(l) from SearchLog l where l.createdAt >= :since"
            + " and l.resultsCount = 0 group by l.queryNormalized order by count(l) desc")
    List<Object[]> topZeroResultSince(@Param("since") Instant since, org.springframework.data.domain.Pageable page);

    @Query("select count(l) from SearchLog l where l.createdAt >= :since and l.resultsCount > 0")
    long countWithResultsSince(@Param("since") Instant since);

    @Modifying
    @Query("delete from SearchLog l where l.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
