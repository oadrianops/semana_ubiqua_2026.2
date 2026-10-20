package br.com.nandesk.commission.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.commission.domain.BudgetPolicy;
import br.com.nandesk.commission.domain.LedgerBalance;
import br.com.nandesk.commission.domain.ProgramBudget;
import br.com.nandesk.commission.infra.ProgramBudgetRepository;
import br.com.nandesk.shared.time.BelemTime;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.MoneyDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orçamento mensal por patrocinador (RF-28, RN-10). O valor comprometido (tarefas publicadas e não concluídas)
 * vem do módulo de tarefas como parâmetro.
 */
@Service
public class BudgetService {

    private static final Pattern PERIOD = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final ProgramBudgetRepository budgets;
    private final LedgerService ledger;
    private final AuditService audit;
    private final Clock clock;

    public BudgetService(ProgramBudgetRepository budgets, LedgerService ledger, AuditService audit, Clock clock) {
        this.budgets = budgets;
        this.ledger = ledger;
        this.audit = audit;
        this.clock = clock;
    }

    public record BudgetRow(Long id, String period, String sponsorName, MoneyDto amount) {}

    public record Overview(
            String period,
            MoneyDto budget,
            MoneyDto committed,
            MoneyDto realized,
            MoneyDto available,
            boolean hasBudget,
            List<BudgetRow> sponsors) {}

    public String currentPeriod() {
        return YearMonth.from(BelemTime.today(clock)).toString();
    }

    /** RN-10: com orçamento no mês, publica só se houver saldo para a comissão. */
    @Transactional(readOnly = true)
    public boolean canPublish(BigDecimal committed, BigDecimal commission) {
        String period = currentPeriod();
        BigDecimal total = budgets.totalFor(period);
        return BudgetPolicy.canPublish(total, committed, realized(period), commission);
    }

    @Transactional(readOnly = true)
    public Overview overview(String period, BigDecimal committed) {
        String p = period == null ? currentPeriod() : period;
        BigDecimal total = budgets.totalFor(p);
        BigDecimal realized = realized(p);
        BigDecimal budget = total == null ? BigDecimal.ZERO : total;
        List<BudgetRow> sponsors =
                budgets.findAllByPeriod(p).stream().map(BudgetService::row).toList();
        return new Overview(
                p,
                MoneyDto.brl(budget),
                MoneyDto.brl(committed),
                MoneyDto.brl(realized),
                MoneyDto.brl(BudgetPolicy.available(budget, committed, realized)),
                total != null,
                sponsors);
    }

    @Transactional
    public BudgetRow upsert(String period, String sponsorName, BigDecimal amount, Long adminId) {
        if (period == null || !PERIOD.matcher(period).matches() || amount == null || amount.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        ProgramBudget budget = budgets.findByPeriodAndSponsorName(period, sponsorName.trim())
                .orElseGet(() -> new ProgramBudget(period, sponsorName.trim(), LedgerBalance.money(amount), adminId));
        budget.setAmount(LedgerBalance.money(amount));
        ProgramBudget saved = budgets.save(budget);
        audit.record(
                adminId,
                "BUDGET_SAVED",
                "program_budget",
                saved.getId(),
                null,
                Map.of(
                        "period",
                        period,
                        "sponsor",
                        sponsorName.trim(),
                        "amount",
                        saved.getAmount().toPlainString()));
        return row(saved);
    }

    @Transactional(readOnly = true)
    public List<BudgetRow> list() {
        return budgets.findAllByOrderByPeriodDescSponsorNameAsc().stream()
                .map(BudgetService::row)
                .toList();
    }

    private BigDecimal realized(String period) {
        YearMonth ym = YearMonth.parse(period);
        Instant from = BelemTime.startOfDay(ym.atDay(1));
        Instant to = BelemTime.startOfDay(ym.plusMonths(1).atDay(1));
        return ledger.creditedBetween(from, to);
    }

    private static BudgetRow row(ProgramBudget b) {
        return new BudgetRow(b.getId(), b.getPeriod(), b.getSponsorName(), MoneyDto.brl(b.getAmount()));
    }
}
