package br.com.nandesk.commission.infra;

import br.com.nandesk.commission.domain.CommissionRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long> {

    List<CommissionRule> findAllByTaskTypeOrderByValidFromDesc(String taskType);

    List<CommissionRule> findAllByOrderByTaskTypeAscValidFromDesc();

    Optional<CommissionRule> findFirstByTaskTypeAndValidToIsNullOrderByValidFromDesc(String taskType);
}
