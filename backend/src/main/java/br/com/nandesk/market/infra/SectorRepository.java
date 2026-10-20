package br.com.nandesk.market.infra;

import br.com.nandesk.market.domain.Sector;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Sector> findAllByMarketIdAndActiveTrueOrderBySortOrderAsc(Long marketId);

    @EntityGraph(attributePaths = "translations")
    List<Sector> findAllByMarketIdOrderBySortOrderAsc(Long marketId);

    @EntityGraph(attributePaths = "translations")
    Optional<Sector> findByMarketIdAndSlug(Long marketId, String slug);
}
