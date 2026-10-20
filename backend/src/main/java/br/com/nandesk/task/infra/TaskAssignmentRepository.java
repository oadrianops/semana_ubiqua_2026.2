package br.com.nandesk.task.infra;

import br.com.nandesk.task.domain.TaskAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    @EntityGraph(attributePaths = {"task", "collaborator"})
    Optional<TaskAssignment> findFirstByTaskIdAndOutcomeIsNull(Long taskId);

    @EntityGraph(attributePaths = {"collaborator"})
    Optional<TaskAssignment> findFirstByTaskIdOrderByAcceptedAtDesc(Long taskId);

    /** Reservas em andamento (RN-03). */
    long countByCollaboratorIdAndOutcomeIsNull(Long collaboratorId);

    /** Envios no dia civil de Belém (RN-04). */
    long countByCollaboratorIdAndSubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
            Long collaboratorId, Instant from, Instant to);

    @EntityGraph(attributePaths = {"task", "task.market", "task.stall", "collaborator"})
    @Query("select a from TaskAssignment a where a.outcome is null and a.submittedAt is null")
    List<TaskAssignment> findAllOpenReservations();

    @EntityGraph(attributePaths = {"task", "task.market", "task.stall", "task.stall.sector"})
    @Query("select a from TaskAssignment a where a.collaborator.id = :userId and a.outcome is null")
    List<TaskAssignment> findActiveOf(@Param("userId") Long userId);

    /** Resultados para a reputação, do mais recente ao mais antigo (RN-17); cancelamentos do sistema não contam. */
    @Query("select a from TaskAssignment a join a.task t where a.collaborator.id = :userId and a.outcome is not null"
            + " and not (a.outcome = br.com.nandesk.task.domain.AssignmentOutcome.RELEASED"
            + " and t.status = br.com.nandesk.task.domain.TaskStatus.CANCELLED)"
            + " order by a.outcomeAt desc")
    List<TaskAssignment> findClosedOf(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "task.market", "task.stall"})
    Page<TaskAssignment> findAllByCollaboratorIdOrderByAcceptedAtDesc(Long collaboratorId, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "task.market", "task.stall"})
    List<TaskAssignment> findAllByCollaboratorIdOrderByAcceptedAtDesc(Long collaboratorId);

    /** Retenção das coordenadas do check-in (RN-21): ficam só distância e marcação. */
    @Modifying
    @Query("update TaskAssignment a set a.checkinLatitude = null, a.checkinLongitude = null"
            + " where a.checkedInAt < :before and a.checkinLatitude is not null")
    int clearCoordinatesBefore(@Param("before") Instant before);

    @Query("select count(distinct a.collaborator.id) from TaskAssignment a where a.acceptedAt >= :since")
    long countActiveCollaboratorsSince(@Param("since") Instant since);
}
