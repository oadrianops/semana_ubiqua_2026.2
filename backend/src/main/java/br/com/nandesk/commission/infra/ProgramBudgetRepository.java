package br.com.nandesk.commission.infra;

import br.com.nandesk.commission.domain.ProgramBudget;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramBudgetRepository extends JpaRepository<ProgramBudget, Long> {

    List<ProgramBudget> findAllByOrderByPeriodDescSponsorNameAsc();

    List<ProgramBudget> findAllByPeriod(String period);

    Optional<ProgramBudget> findByPeriodAndSponsorName(String period, String sponsorName);

    @Query("select sum(b.amount) from ProgramBudget b where b.period = :period")
    BigDecimal totalFor(@Param("period") String period);
}
