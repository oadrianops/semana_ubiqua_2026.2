package br.com.nandesk.commission.application;

import br.com.nandesk.audit.application.AuditService;
import br.com.nandesk.commission.domain.EntryType;
import br.com.nandesk.commission.domain.LedgerBalance;
import br.com.nandesk.commission.domain.LedgerEntry;
import br.com.nandesk.commission.domain.Payout;
import br.com.nandesk.commission.domain.PayoutMethod;
import br.com.nandesk.commission.infra.LedgerEntryRepository;
import br.com.nandesk.commission.infra.PayoutRepository;
import br.com.nandesk.shared.config.AppProperties;
import br.com.nandesk.shared.security.SegregationOfDuties;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.shared.web.MoneyDto;
import br.com.nandesk.shared.web.PageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extrato imutável, saldo e repasses (RF-26, RF-27; RN-08, RN-09, RN-11). A plataforma só registra: o repasse é
 * feito fora dela (P2).
 */
@Service
public class LedgerService {

    private final LedgerEntryRepository entries;
    private final PayoutRepository payouts;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final AppProperties properties;
    private final MeterRegistry meters;
    private final Clock clock;

    public LedgerService(
            LedgerEntryRepository entries,
            PayoutRepository payouts,
            AuditService audit,
            ApplicationEventPublisher events,
            AppProperties properties,
            MeterRegistry meters,
            Clock clock) {
        this.entries = entries;
        this.payouts = payouts;
        this.audit = audit;
        this.events = events;
        this.properties = properties;
        this.meters = meters;
        this.clock = clock;
    }

    /** Evento: repasse registrado (o colaborador é notificado — RF-14). */
    public record PayoutRegistered(Long collaboratorId, BigDecimal amount, UUID payoutId) {}

    public record EntryView(
            UUID publicId,
            EntryType type,
            MoneyDto amount,
            MoneyDto balanceAfter,
            Long taskId,
            String description,
            Instant createdAt) {}

    public record Statement(
            MoneyDto balance, MoneyDto totalPaidOut, MoneyDto minimumPayout, PageResponse<EntryView> entries) {}

    public record BalanceRow(Long collaboratorId, MoneyDto balance) {}

    /** Crédito da comissão na aprovação (RN-07), no máximo um por tarefa. */
    @Transactional
    public void creditCommission(Long collaboratorId, Long taskId, BigDecimal amount, String description) {
        if (amount.signum() <= 0 || entries.existsByTaskIdAndEntryType(taskId, EntryType.CREDIT)) {
            return;
        }
        entries.save(new LedgerEntry(
                collaboratorId,
                EntryType.CREDIT,
                LedgerBalance.signed(EntryType.CREDIT, amount),
                taskId,
                null,
                null,
                description,
                null,
                clock.instant()));
        meters.counter("nandesk.commissions.credited").increment(amount.doubleValue());
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(Long collaboratorId) {
        return LedgerBalance.money(entries.balanceOf(collaboratorId));
    }

    /** Extrato com saldo acumulado por lançamento (RF-26). */
    @Transactional(readOnly = true)
    public Statement statement(Long collaboratorId, int page, int size) {
        List<LedgerEntry> all = entries.findAllByCollaboratorIdOrderByCreatedAtAscIdAsc(collaboratorId);
        Map<Long, BigDecimal> running = new HashMap<>();
        BigDecimal acc = BigDecimal.ZERO;
        for (LedgerEntry e : all) {
            acc = acc.add(e.getAmount());
            running.put(e.getId(), acc);
        }
        List<EntryView> newestFirst = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            LedgerEntry e = all.get(i);
            newestFirst.add(new EntryView(
                    e.getPublicId(),
                    e.getEntryType(),
                    MoneyDto.brl(e.getAmount()),
                    MoneyDto.brl(running.get(e.getId())),
                    e.getTaskId(),
                    e.getDescription(),
                    e.getCreatedAt()));
        }
        int clamped = PageResponse.clampSize(size);
        int from = Math.min(Math.max(0, page) * clamped, newestFirst.size());
        int to = Math.min(from + clamped, newestFirst.size());
        return new Statement(
                MoneyDto.brl(LedgerBalance.money(acc)),
                MoneyDto.brl(entries.paidOutOf(collaboratorId)),
                MoneyDto.brl(properties.payouts().minimumAmount()),
                PageResponse.of(newestFirst.subList(from, to), Math.max(0, page), clamped, newestFirst.size()));
    }

    /** Colaboradores com saldo ≥ mínimo, para o admin registrar repasses (RF-27). */
    @Transactional(readOnly = true)
    public List<BalanceRow> balancesAtLeastMinimum() {
        return entries.balancesAtLeast(properties.payouts().minimumAmount()).stream()
                .map(r -> new BalanceRow((Long) r[0], MoneyDto.brl((BigDecimal) r[1])))
                .toList();
    }

    /** Registra um repasse feito fora da plataforma (RF-27, RN-09, RN-11). */
    @Transactional
    public UUID registerPayout(
            Long adminId,
            Long collaboratorId,
            BigDecimal amount,
            PayoutMethod method,
            String reference,
            Instant paidAt,
            String notes) {
        SegregationOfDuties.checkNotSelf(adminId, collaboratorId);
        BigDecimal value = LedgerBalance.money(amount);
        LedgerBalance.checkPayout(
                balance(collaboratorId), value, properties.payouts().minimumAmount());
        Payout payout = payouts.save(new Payout(
                collaboratorId, value, method, reference, paidAt == null ? clock.instant() : paidAt, adminId, notes));
        entries.save(new LedgerEntry(
                collaboratorId,
                EntryType.PAYOUT,
                LedgerBalance.signed(EntryType.PAYOUT, value),
                null,
                payout.getId(),
                null,
                "Repasse " + method.name(),
                adminId,
                clock.instant()));
        audit.record(
                adminId,
                "PAYOUT_REGISTERED",
                "payout",
                payout.getPublicId(),
                null,
                Map.of("collaboratorId", collaboratorId, "amount", value.toPlainString(), "method", method.name()));
        events.publishEvent(new PayoutRegistered(collaboratorId, value, payout.getPublicId()));
        return payout.getPublicId();
    }

    /** Estorno ou ajuste com justificativa obrigatória e auditoria (RN-08). */
    @Transactional
    public EntryView adjust(
            Long adminId,
            Long collaboratorId,
            EntryType type,
            BigDecimal amount,
            UUID reversesEntry,
            String justification) {
        LedgerBalance.requireJustification(justification);
        if (type != EntryType.REVERSAL && type != EntryType.ADJUSTMENT) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Long reversesId = null;
        BigDecimal value = amount;
        if (type == EntryType.REVERSAL) {
            LedgerEntry original = entries.findByPublicId(reversesEntry).orElseThrow(BusinessException::notFound);
            if (!original.getCollaboratorId().equals(collaboratorId) || original.getEntryType() != EntryType.CREDIT) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            reversesId = original.getId();
            value = original.getAmount();
        }
        LedgerEntry entry = entries.save(new LedgerEntry(
                collaboratorId,
                type,
                LedgerBalance.signed(type, value),
                null,
                null,
                reversesId,
                justification.trim(),
                adminId,
                clock.instant()));
        audit.record(
                adminId,
                "LEDGER_" + type.name(),
                "ledger_entry",
                entry.getPublicId(),
                null,
                Map.of(
                        "collaboratorId",
                        collaboratorId,
                        "amount",
                        entry.getAmount().toPlainString(),
                        "justification",
                        justification.trim()));
        return new EntryView(
                entry.getPublicId(),
                type,
                MoneyDto.brl(entry.getAmount()),
                MoneyDto.brl(balance(collaboratorId)),
                null,
                entry.getDescription(),
                entry.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public BigDecimal creditedBetween(Instant from, Instant to) {
        return LedgerBalance.money(entries.creditedBetween(from, to));
    }

    @Transactional(readOnly = true)
    public BigDecimal paidBetween(Instant from, Instant to) {
        return LedgerBalance.money(entries.paidBetween(from, to));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> allEntries(Long collaboratorId) {
        return entries.findAllByCollaboratorIdOrderByCreatedAtAscIdAsc(collaboratorId);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> payouts(int page, int size) {
        return PageResponse.of(
                payouts.findAllByOrderByPaidAtDesc(PageRequest.of(Math.max(0, page), PageResponse.clampSize(size))),
                p -> Map.<String, Object>of(
                        "publicId", p.getPublicId(),
                        "collaboratorId", p.getCollaboratorId(),
                        "amount", MoneyDto.brl(p.getAmount()),
                        "method", p.getMethod(),
                        "paidAt", p.getPaidAt()));
    }
}
