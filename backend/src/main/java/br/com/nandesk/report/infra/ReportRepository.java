package br.com.nandesk.report.infra;

import br.com.nandesk.report.domain.Report;
import br.com.nandesk.report.domain.ReportStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByStallIdAndStatusAndCreatedAtAfter(Long stallId, ReportStatus status, Instant after);

    List<Report> findAllByStallIdAndStatus(Long stallId, ReportStatus status);

    List<Report> findAllByTaskId(Long taskId);

    @EntityGraph(attributePaths = {"stall", "stall.market", "product", "product.translations"})
    Page<Report> findAllByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Optional<Report> findByPublicId(UUID publicId);
}
