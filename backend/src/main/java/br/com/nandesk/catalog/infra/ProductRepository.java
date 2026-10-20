package br.com.nandesk.catalog.infra;

import br.com.nandesk.catalog.domain.Product;
import br.com.nandesk.catalog.domain.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"translations", "category", "category.translations", "image"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"translations", "category", "image"})
    Optional<Product> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"translations", "category", "category.translations", "image"})
    List<Product> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"translations", "image"})
    List<Product> findAllByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    @EntityGraph(attributePaths = {"translations", "aliases", "category"})
    List<Product> findAllByStatus(ProductStatus status);

    @EntityGraph(attributePaths = {"translations", "category", "category.translations", "suggestedBy"})
    Page<Product> findAllByStatusOrderByCreatedAtAsc(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"translations", "category"})
    @Query("select distinct p from Product p join p.translations t where p.status = :status"
            + " and (:q is null or lower(t.name) like :q or lower(p.slug) like :q)")
    Page<Product> searchByStatus(@Param("status") ProductStatus status, @Param("q") String q, Pageable pageable);
}
