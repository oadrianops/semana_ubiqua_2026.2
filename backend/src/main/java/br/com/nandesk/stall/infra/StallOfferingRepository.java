package br.com.nandesk.stall.infra;

import br.com.nandesk.stall.domain.StallOffering;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StallOfferingRepository extends JpaRepository<StallOffering, Long> {

    String PUBLIC_STALL = " s.status = br.com.nandesk.stall.domain.StallStatus.ACTIVE"
            + " and s.consentStatus = br.com.nandesk.stall.domain.ConsentStatus.GRANTED"
            + " and m.active = true ";

    /** Ofertas de boxes públicos para os produtos pedidos, com tudo o que a busca exibe (sem N+1). */
    @Query("select o from StallOffering o join fetch o.stall s join fetch s.market m"
            + " left join fetch s.sector sec left join fetch sec.translations"
            + " where o.product.id in :productIds and" + PUBLIC_STALL
            + " and (:marketSlug is null or m.slug = :marketSlug)"
            + " and (:availableOnly = false"
            + " or o.availability <> br.com.nandesk.stall.domain.Availability.OUT_OF_STOCK)")
    List<StallOffering> findPublicByProducts(
            @Param("productIds") Collection<Long> productIds,
            @Param("marketSlug") String marketSlug,
            @Param("availableOnly") boolean availableOnly);

    @Query("select o from StallOffering o join fetch o.product p join fetch p.category"
            + " left join fetch p.translations where o.stall.id = :stallId")
    List<StallOffering> findByStallWithProduct(@Param("stallId") Long stallId);

    @Query("select o from StallOffering o join fetch o.stall s join fetch s.market m"
            + " join fetch o.product p where m.id = :marketId and" + PUBLIC_STALL)
    List<StallOffering> findPublicByMarket(@Param("marketId") Long marketId);

    @Query("select o.stall.id, count(o) from StallOffering o where o.stall.id in :stallIds group by o.stall.id")
    List<Object[]> countByStallIds(@Param("stallIds") Collection<Long> stallIds);

    @Query("select o.product.id, count(distinct o.stall.id) from StallOffering o join o.stall s join s.market m"
            + " where" + PUBLIC_STALL + " group by o.product.id")
    List<Object[]> countPublicStallsByProduct();
}
