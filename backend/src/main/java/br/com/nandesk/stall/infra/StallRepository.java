package br.com.nandesk.stall.infra;

import br.com.nandesk.stall.domain.Stall;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StallRepository extends JpaRepository<Stall, Long> {

    /** RN-13: a foto do box com consentimento revogado sai do box quando a retenção vence. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stall s set s.photo = null where s.photo.id in :fileIds")
    int clearPhotos(@Param("fileIds") java.util.Collection<Long> fileIds);

    String PUBLIC = " s.status = br.com.nandesk.stall.domain.StallStatus.ACTIVE"
            + " and s.consentStatus = br.com.nandesk.stall.domain.ConsentStatus.GRANTED ";

    @EntityGraph(attributePaths = {"sector", "sector.translations", "photo", "market"})
    @Query(
            value = "select s from Stall s where s.market.id = :marketId and" + PUBLIC
                    + " and (:sectorId is null or s.sector.id = :sectorId)",
            countQuery = "select count(s) from Stall s where s.market.id = :marketId and" + PUBLIC
                    + " and (:sectorId is null or s.sector.id = :sectorId)")
    Page<Stall> findPublicByMarket(
            @Param("marketId") Long marketId, @Param("sectorId") Long sectorId, Pageable pageable);

    @EntityGraph(attributePaths = {"sector", "sector.translations", "photo"})
    @Query("select s from Stall s where s.market.id = :marketId and" + PUBLIC + " order by s.code")
    List<Stall> findAllPublicByMarket(@Param("marketId") Long marketId);

    @EntityGraph(attributePaths = {"market", "market.translations", "sector", "sector.translations", "photo"})
    @Query("select s from Stall s where s.publicId = :publicId and" + PUBLIC)
    Optional<Stall> findPublicByPublicId(@Param("publicId") UUID publicId);

    @EntityGraph(attributePaths = {"market", "sector", "sector.translations", "photo"})
    Optional<Stall> findByPublicId(UUID publicId);

    Optional<Stall> findByMarketIdAndCode(Long marketId, String code);

    @Query("select s.market.id, count(s) from Stall s where" + PUBLIC + " group by s.market.id")
    List<Object[]> countPublicByMarket();

    @Query("select s.sector.id, count(s) from Stall s where s.market.id = :marketId and" + PUBLIC
            + " and s.sector is not null group by s.sector.id")
    List<Object[]> countPublicBySector(@Param("marketId") Long marketId);

    /** Boxes públicos sem verificação recente (RN-12), para o job de frescor. */
    @Query("select s from Stall s where" + PUBLIC + " and (s.lastVerifiedAt is null or s.lastVerifiedAt < :before)")
    List<Stall> findPublicStale(@Param("before") Instant before);

    @EntityGraph(attributePaths = {"market", "sector", "sector.translations"})
    @Query("select s from Stall s where (:marketId is null or s.market.id = :marketId)"
            + " and (:q is null or lower(s.code) like :q or lower(s.displayName) like :q)")
    Page<Stall> searchAdmin(@Param("marketId") Long marketId, @Param("q") String q, Pageable pageable);

    @Query("select count(s) from Stall s where" + PUBLIC)
    long countPublic();

    @Query("select count(s) from Stall s where" + PUBLIC + " and s.lastVerifiedAt >= :since")
    long countPublicVerifiedSince(@Param("since") Instant since);

    @Query("select count(s) from Stall s where" + PUBLIC + " and s.photo is not null")
    long countPublicWithPhoto();
}
