package br.com.nandesk.catalog.infra;

import br.com.nandesk.catalog.domain.ProductSearchTerm;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSearchTermRepository extends JpaRepository<ProductSearchTerm, Long> {

    /**
     * Candidatos da busca (9.10): {@code LIKE 'q%'} usa o índice; {@code LIKE '%token%'} cobre palavras no meio.
     * O ranqueamento fino é feito em Java ({@code SearchRanking}).
     */
    @Query("select t from ProductSearchTerm t join fetch t.product p"
            + " where p.status = br.com.nandesk.catalog.domain.ProductStatus.ACTIVE"
            + " and (t.normalizedTerm like :prefix or t.normalizedTerm like :contains)")
    List<ProductSearchTerm> findCandidates(
            @Param("prefix") String prefix, @Param("contains") String contains, Pageable pageable);

    @Query("select t from ProductSearchTerm t join fetch t.product p"
            + " where p.status = br.com.nandesk.catalog.domain.ProductStatus.ACTIVE")
    List<ProductSearchTerm> findAllActive();

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from ProductSearchTerm t where t.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
