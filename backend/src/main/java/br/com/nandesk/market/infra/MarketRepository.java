package br.com.nandesk.market.infra;

import br.com.nandesk.market.domain.Market;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRepository extends JpaRepository<Market, Long> {

    @EntityGraph(attributePaths = {"translations", "cover"})
    List<Market> findAllByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"translations", "cover"})
    List<Market> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"translations", "cover"})
    Optional<Market> findBySlugAndActiveTrue(String slug);

    Optional<Market> findBySlug(String slug);

    Optional<Market> findByPublicId(UUID publicId);

    @Query("select m.dataVersion from Market m where m.slug = :slug and m.active = true")
    Optional<Long> findActiveDataVersion(@Param("slug") String slug);

    /** Incrementa a versão dos dados na mesma transação da mudança publicada (10.3). */
    @Modifying(flushAutomatically = true)
    @Query("update Market m set m.dataVersion = m.dataVersion + 1 where m.id = :id")
    int bumpDataVersion(@Param("id") Long id);
}
