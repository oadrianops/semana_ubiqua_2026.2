package br.com.nandesk.stall.infra;

import br.com.nandesk.stall.domain.StallPhoto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StallPhotoRepository extends JpaRepository<StallPhoto, Long> {

    @EntityGraph(attributePaths = "file")
    List<StallPhoto> findByStallIdOrderBySortOrderAscIdAsc(Long stallId);

    @Query("select coalesce(max(p.sortOrder), -1) from StallPhoto p where p.stall.id = :stallId")
    int maxSortOrder(@Param("stallId") Long stallId);

    @EntityGraph(attributePaths = "file")
    Optional<StallPhoto> findByStallIdAndFilePublicId(Long stallId, java.util.UUID filePublicId);

    /** Retenção vencida (RN-13): a foto sai da galeria antes de ser apagada do disco. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StallPhoto p where p.file.id in :fileIds")
    int deleteByFileIds(@Param("fileIds") Collection<Long> fileIds);
}
