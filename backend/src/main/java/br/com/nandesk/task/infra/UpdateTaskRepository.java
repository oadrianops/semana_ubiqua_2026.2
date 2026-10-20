package br.com.nandesk.task.infra;

import br.com.nandesk.task.domain.TaskStatus;
import br.com.nandesk.task.domain.TaskType;
import br.com.nandesk.task.domain.UpdateTask;
import java.math.BigDecimal;
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

public interface UpdateTaskRepository extends JpaRepository<UpdateTask, Long> {

    @EntityGraph(
            attributePaths = {
                "market",
                "stall",
                "stall.sector",
                "stall.sector.translations",
                "suggestedSector",
                "suggestedSector.translations"
            })
    Optional<UpdateTask> findByPublicId(UUID publicId);

    @EntityGraph(
            attributePaths = {
                "market",
                "stall",
                "stall.sector",
                "stall.sector.translations",
                "suggestedSector",
                "suggestedSector.translations"
            })
    @Query("select t from UpdateTask t where t.status = br.com.nandesk.task.domain.TaskStatus.OPEN"
            + " and (:marketId is null or t.market.id = :marketId) and (:type is null or t.type = :type)"
            + " order by t.priority desc, t.publishedAt asc")
    List<UpdateTask> findOpen(@Param("marketId") Long marketId, @Param("type") TaskType type);

    boolean existsByStallIdAndStatusIn(Long stallId, Collection<TaskStatus> statuses);

    @EntityGraph(attributePaths = {"market", "stall"})
    List<UpdateTask> findAllByStallIdAndStatusIn(Long stallId, Collection<TaskStatus> statuses);

    @EntityGraph(attributePaths = {"market"})
    List<UpdateTask> findAllByStatusOrderByCreatedAtAsc(TaskStatus status);

    /** Comprometido no orçamento: comissões de tarefas publicadas e ainda não concluídas (RF-28). */
    @Query("select coalesce(sum(t.commissionAmount), 0) from UpdateTask t where t.status in"
            + " (br.com.nandesk.task.domain.TaskStatus.OPEN, br.com.nandesk.task.domain.TaskStatus.ACCEPTED,"
            + " br.com.nandesk.task.domain.TaskStatus.CHECKED_IN, br.com.nandesk.task.domain.TaskStatus.SUBMITTED)")
    BigDecimal committedAmount();

    @Query("select t.status, count(t) from UpdateTask t group by t.status")
    List<Object[]> countByStatus();

    @EntityGraph(attributePaths = {"market", "stall"})
    @Query("select t from UpdateTask t where (:status is null or t.status = :status)"
            + " and (:marketId is null or t.market.id = :marketId) order by t.createdAt desc")
    Page<UpdateTask> searchAdmin(
            @Param("status") TaskStatus status, @Param("marketId") Long marketId, Pageable pageable);
}
