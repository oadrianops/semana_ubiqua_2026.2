package br.com.nandesk.commission.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.commission.domain.CommissionCalculator;
import br.com.nandesk.commission.domain.CommissionCalculator.RuleVersion;
import br.com.nandesk.commission.domain.CommissionRule;
import br.com.nandesk.commission.domain.LedgerBalance;
import br.com.nandesk.commission.infra.CommissionRuleRepository;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.user.domain.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de comissão por tipo de tarefa com início de vigência (RF-25). O histórico é preservado: uma regra nova
 * encerra a anterior. O tipo de tarefa chega como texto para o módulo não depender do de tarefas (ADR-0003).
 */
@Service
public class CommissionRuleService {

    private static final Set<String> TYPES = Set.of("NEW_STALL", "UPDATE_STALL", "VERIFY_REPORT");

    private final CommissionRuleRepository rules;
    private final AuditService audit;
    private final Clock clock;

    public CommissionRuleService(CommissionRuleRepository rules, AuditService audit, Clock clock) {
        this.rules = rules;
        this.audit = audit;
        this.clock = clock;
    }

    public record RuleView(String taskType, MoneyDto amount, Instant validFrom, Instant validTo) {}

    /** Valor vigente agora (RN-07); sem regra, zero. */
    @Transactional(readOnly = true)
    public BigDecimal currentAmount(String taskType) {
        List<RuleVersion> versions = rules.findAllByTaskTypeOrderByValidFromDesc(taskType).stream()
                .map(r -> new RuleVersion(r.getTaskType(), r.getAmount(), r.getValidFrom(), r.getValidTo()))
                .toList();
        return CommissionCalculator.amountAt(versions, taskType, clock.instant());
    }

    @Transactional
    public RuleView create(String taskType, BigDecimal amount, Instant validFrom, User admin) {
        if (!TYPES.contains(taskType) || amount == null || amount.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Instant from = validFrom == null ? clock.instant() : validFrom;
        rules.findFirstByTaskTypeAndValidToIsNullOrderByValidFromDesc(taskType).ifPresent(previous -> {
            if (!from.isAfter(previous.getValidFrom())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            previous.setValidTo(from);
        });
        CommissionRule rule =
                rules.save(new CommissionRule(taskType, LedgerBalance.money(amount), from, admin.getId()));
        audit.record(
                admin.getId(),
                "COMMISSION_RULE_CREATED",
                "commission_rule",
                rule.getId(),
                null,
                Map.of("taskType", taskType, "amount", rule.getAmount().toPlainString(), "validFrom", from.toString()));
        return view(rule);
    }

    /** Usado pelo seed: cria a regra só se o tipo ainda não tiver nenhuma. */
    @Transactional
    public void ensureRule(String taskType, BigDecimal amount, User admin) {
        if (rules.findAllByTaskTypeOrderByValidFromDesc(taskType).isEmpty()) {
            rules.save(new CommissionRule(
                    taskType, LedgerBalance.money(amount), clock.instant().minusSeconds(60), admin.getId()));
        }
    }

    @Transactional(readOnly = true)
    public List<RuleView> list() {
        return rules.findAllByOrderByTaskTypeAscValidFromDesc().stream()
                .map(CommissionRuleService::view)
                .toList();
    }

    private static RuleView view(CommissionRule r) {
        return new RuleView(r.getTaskType(), MoneyDto.brl(r.getAmount()), r.getValidFrom(), r.getValidTo());
    }
}
