package br.com.nandesk.catalog.infra;

import br.com.nandesk.catalog.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Category> findAllByActiveTrueOrderBySortOrderAsc();

    @EntityGraph(attributePaths = "translations")
    List<Category> findAllByOrderBySortOrderAsc();

    @EntityGraph(attributePaths = "translations")
    Optional<Category> findBySlug(String slug);
}
