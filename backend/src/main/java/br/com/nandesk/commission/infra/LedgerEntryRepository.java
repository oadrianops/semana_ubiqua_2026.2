package br.com.nandesk.commission.infra;

import br.com.nandesk.commission.domain.EntryType;
import br.com.nandesk.commission.domain.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findAllByCollaboratorIdOrderByCreatedAtDescIdDesc(Long collaboratorId, Pageable pageable);

    List<LedgerEntry> findAllByCollaboratorIdOrderByCreatedAtAscIdAsc(Long collaboratorId);

    Optional<LedgerEntry> findByPublicId(UUID publicId);

    boolean existsByTaskIdAndEntryType(Long taskId, EntryType type);

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.collaboratorId = :collaboratorId")
    BigDecimal balanceOf(@Param("collaboratorId") Long collaboratorId);

    @Query("select coalesce(sum(-e.amount), 0) from LedgerEntry e where e.collaboratorId = :collaboratorId"
            + " and e.entryType = br.com.nandesk.commission.domain.EntryType.PAYOUT")
    BigDecimal paidOutOf(@Param("collaboratorId") Long collaboratorId);

    @Query("select e.collaboratorId, sum(e.amount) from LedgerEntry e group by e.collaboratorId"
            + " having sum(e.amount) >= :minimum")
    List<Object[]> balancesAtLeast(@Param("minimum") BigDecimal minimum);

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e"
            + " where e.entryType = br.com.nandesk.commission.domain.EntryType.CREDIT"
            + " and e.createdAt >= :from and e.createdAt < :to")
    BigDecimal creditedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(-e.amount), 0) from LedgerEntry e"
            + " where e.entryType = br.com.nandesk.commission.domain.EntryType.PAYOUT"
            + " and e.createdAt >= :from and e.createdAt < :to")
    BigDecimal paidBetween(@Param("from") Instant from, @Param("to") Instant to);
}
