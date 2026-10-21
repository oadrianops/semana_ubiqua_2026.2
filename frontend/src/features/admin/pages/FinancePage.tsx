import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page, TaskType } from '../../../shared/api/types';
import { formatDate, formatDateTime, formatMoney } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { SelectField, TextField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import type { BalanceView, BudgetOverview, PayoutRow, RuleView } from '../types';
import { AdminHeader, errorMessage, moneyFrom, Pill, SectionTitle } from '../ui';

const TYPES: TaskType[] = ['UPDATE_STALL', 'NEW_STALL', 'VERIFY_REPORT'];
const METHODS = ['PIX', 'TRANSFER', 'CASH', 'OTHER'] as const;

/**
 * Finanças (RF-25 a RF-28, só ADMIN): regras de comissão com vigência, orçamento do mês por patrocinador,
 * saldos acima do mínimo e o registro dos repasses feitos fora da plataforma. Não há pagamento online (Won't).
 */
export default function FinancePage() {
  const { t } = useTranslation('admin');
  return (
    <div className="grid gap-10">
      <AdminHeader title={t('finance.title')} lead={t('finance.lead')} />
      <Budget />
      <Balances />
      <Rules />
      <Payouts />
    </div>
  );
}

function Budget() {
  const { t, i18n } = useTranslation('admin');
  const lng = i18n.language;
  const client = useQueryClient();
  const query = useQuery({
    queryKey: ['admin', 'budgets'],
    queryFn: () => api.get<{ overview: BudgetOverview }>('/admin/budgets'),
    networkMode: 'online',
  });
  const [sponsor, setSponsor] = useState('');
  const [amount, setAmount] = useState('');
  const save = useMutation({
    mutationFn: () =>
      api.put(
        '/admin/budgets',
        { period: query.data?.overview.period, sponsorName: sponsor.trim(), amount: moneyFrom(amount) },
        { retries: 0 },
      ),
    onSuccess: () => {
      toast.success(t('finance.budgetSaved'));
      setSponsor('');
      setAmount('');
      void client.invalidateQueries({ queryKey: ['admin'] });
    },
    onError: (e) => toast.error(errorMessage(e, t('finance.error'))),
  });
  const o = query.data?.overview;
  const used =
    o && Number(o.budget.amount) > 0
      ? Math.min(1, (Number(o.committed.amount) + Number(o.realized.amount)) / Number(o.budget.amount))
      : 0;
  return (
    <section className="grid gap-3" aria-labelledby="budget">
      <SectionTitle id="budget" aside={o?.period}>
        {t('finance.budget')}
      </SectionTitle>
      {query.isPending && <LoadingRows rows={2} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {o && (
        <>
          <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {(['budget', 'committed', 'realized', 'available'] as const).map((k) => (
              <div key={k} className="rounded-[var(--radius-control)] bg-soft p-3">
                <dt className="text-sm text-support">{t(`finance.${k}`)}</dt>
                <dd className="display tabular text-xl text-ink">{formatMoney(o[k], lng)}</dd>
              </div>
            ))}
          </dl>
          <div
            className="h-2 overflow-hidden rounded-full bg-[var(--cinza-200)]"
            role="progressbar"
            aria-label={t('finance.used')}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(used * 100)}
          >
            <div className="h-full rounded-full bg-brand" style={{ width: `${used * 100}%` }} />
          </div>
          {!o.hasBudget && <p className="font-semibold text-warning">{t('finance.noBudget')}</p>}
          {o.sponsors.length > 0 && (
            <ul className="divide-y divide-line">
              {o.sponsors.map((s) => (
                <li key={s.id} className="price-row py-2">
                  <span>{s.sponsorName}</span>
                  <span className="price-row__leader" aria-hidden="true" />
                  <span className="tabular">{formatMoney(s.amount, lng)}</span>
                </li>
              ))}
            </ul>
          )}
          <form
            className="grid gap-3 sm:grid-cols-[1fr_10rem_auto] sm:items-end"
            onSubmit={(e) => {
              e.preventDefault();
              if (sponsor.trim() && moneyFrom(amount)) save.mutate();
            }}
          >
            <TextField
              label={t('finance.sponsor')}
              value={sponsor}
              maxLength={120}
              onChange={(e) => setSponsor(e.target.value)}
            />
            <TextField
              label={t('finance.amount')}
              inputMode="decimal"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
            <Button type="submit" busy={save.isPending} disabled={!sponsor.trim() || !moneyFrom(amount)}>
              {t('finance.saveBudget')}
            </Button>
          </form>
        </>
      )}
    </section>
  );
}

function Balances() {
  const { t, i18n } = useTranslation('admin');
  const query = useQuery({
    queryKey: ['admin', 'balances'],
    queryFn: () => api.get<BalanceView[]>('/admin/balances'),
    networkMode: 'online',
  });
  const [paying, setPaying] = useState<BalanceView | null>(null);
  return (
    <section className="grid gap-3" aria-labelledby="balances">
      <SectionTitle id="balances" aside={t('finance.balancesHint')}>
        {t('finance.balances')}
      </SectionTitle>
      {query.isPending && <LoadingRows rows={3} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.length === 0 && (
        <EmptyState title={t('finance.noBalancesTitle')} body={t('finance.noBalancesBody')} />
      )}
      <ul className="divide-y divide-line">
        {query.data?.map((b) => (
          <li
            key={b.collaborator.publicId}
            className="flex flex-wrap items-center justify-between gap-3 py-3"
          >
            <span className="grid">
              <span className="font-semibold">{b.collaborator.name}</span>
              <span className="text-sm text-support">
                {b.collaborator.emailMasked} {!b.hasPixKey && <Pill tone="warn">{t('finance.noPix')}</Pill>}
              </span>
            </span>
            <span className="flex items-center gap-3">
              <span className="display tabular text-xl">{formatMoney(b.balance, i18n.language)}</span>
              <Button size="sm" onClick={() => setPaying(b)}>
                {t('finance.registerPayout')}
              </Button>
            </span>
          </li>
        ))}
      </ul>
      {paying && <PayoutSheet balance={paying} onClose={() => setPaying(null)} />}
    </section>
  );
}

function PayoutSheet({ balance, onClose }: { balance: BalanceView; onClose: () => void }) {
  const { t, i18n } = useTranslation('admin');
  const client = useQueryClient();
  const [amount, setAmount] = useState(balance.balance.amount.replace('.', ','));
  const [method, setMethod] = useState<(typeof METHODS)[number]>('PIX');
  const [reference, setReference] = useState('');
  const [pix, setPix] = useState<{ type: string; key: string } | null>(null);
  const reveal = useMutation({
    mutationFn: () =>
      api.get<{ type: string; key: string }>(
        `/admin/collaborators/${balance.collaborator.publicId}/pix-key`,
        { retries: 0 },
      ),
    onSuccess: setPix,
    onError: (e) => toast.error(errorMessage(e, t('finance.error'))),
  });
  const pay = useMutation({
    mutationFn: () =>
      api.post(
        '/admin/payouts',
        {
          collaboratorId: balance.collaborator.publicId,
          amount: moneyFrom(amount),
          method,
          externalReference: reference.trim() || null,
          paidAt: null,
          notes: null,
        },
        { retries: 0 },
      ),
    onSuccess: () => {
      toast.success(t('finance.payoutDone'));
      void client.invalidateQueries({ queryKey: ['admin'] });
      onClose();
    },
    onError: (e) => toast.error(errorMessage(e, t('finance.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (moneyFrom(amount)) pay.mutate();
  };
  return (
    <Sheet
      open
      onOpenChange={(o) => !o && onClose()}
      title={t('finance.payoutTitle', { name: balance.collaborator.name })}
      description={t('finance.payoutBody')}
    >
      <div className="mb-4 grid gap-2 rounded-[var(--radius-control)] bg-soft p-3">
        <p className="text-sm text-support">
          {t('finance.balanceNow', { value: formatMoney(balance.balance, i18n.language) })}
        </p>
        {pix ? (
          <p className="font-semibold break-all">
            {t(`account:pix.types.${pix.type}`, { defaultValue: pix.type })}:{' '}
            <span className="tabular">{pix.key || '—'}</span>
          </p>
        ) : (
          <Button
            variant="secondary"
            size="sm"
            className="justify-self-start"
            busy={reveal.isPending}
            disabled={!balance.hasPixKey}
            onClick={() => reveal.mutate()}
          >
            {t('finance.revealPix')}
          </Button>
        )}
        <p className="text-xs text-support">{t('finance.revealAudit')}</p>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        <TextField
          label={t('finance.amount')}
          inputMode="decimal"
          value={amount}
          error={moneyFrom(amount) ? null : t('finance.amountError')}
          onChange={(e) => setAmount(e.target.value)}
        />
        <SelectField
          label={t('finance.method')}
          value={method}
          onChange={(e) => setMethod(e.target.value as typeof method)}
        >
          {METHODS.map((m) => (
            <option key={m} value={m}>
              {t(`finance.methods.${m}`)}
            </option>
          ))}
        </SelectField>
        <TextField
          label={t('finance.reference')}
          hint={t('finance.referenceHint')}
          value={reference}
          maxLength={120}
          onChange={(e) => setReference(e.target.value)}
        />
        <Button type="submit" block busy={pay.isPending} disabled={!moneyFrom(amount)}>
          {t('finance.confirmPayout')}
        </Button>
      </form>
    </Sheet>
  );
}

function Rules() {
  const { t, i18n } = useTranslation('admin');
  const client = useQueryClient();
  const query = useQuery({
    queryKey: ['admin', 'rules'],
    queryFn: () => api.get<RuleView[]>('/admin/commission-rules'),
    networkMode: 'online',
  });
  const [type, setType] = useState<TaskType>('UPDATE_STALL');
  const [amount, setAmount] = useState('');
  const create = useMutation({
    mutationFn: () =>
      api.post(
        '/admin/commission-rules',
        { taskType: type, amount: moneyFrom(amount), validFrom: null },
        { retries: 0 },
      ),
    onSuccess: () => {
      toast.success(t('finance.ruleSaved'));
      setAmount('');
      void client.invalidateQueries({ queryKey: ['admin', 'rules'] });
    },
    onError: (e) => toast.error(errorMessage(e, t('finance.error'))),
  });
  return (
    <section className="grid gap-3" aria-labelledby="rules">
      <SectionTitle id="rules" aside={t('finance.rulesHint')}>
        {t('finance.rules')}
      </SectionTitle>
      <ul className="divide-y divide-line">
        {query.data?.map((r) => (
          <li
            key={`${r.taskType}-${r.validFrom}`}
            className="flex flex-wrap items-baseline justify-between gap-3 py-2"
          >
            <span>
              <span className="font-semibold">{t(`collab:type.${r.taskType}`)}</span>{' '}
              <span className="text-sm text-support">
                {t('finance.validFrom', { date: formatDate(r.validFrom, i18n.language) })}
                {r.validTo
                  ? ` · ${t('finance.validTo', { date: formatDate(r.validTo, i18n.language) })}`
                  : ''}
              </span>
            </span>
            <span className="flex items-center gap-2">
              {!r.validTo && <Pill tone="good">{t('finance.current')}</Pill>}
              <span className="display tabular text-lg">{formatMoney(r.amount, i18n.language)}</span>
            </span>
          </li>
        ))}
      </ul>
      <form
        className="grid gap-3 sm:grid-cols-[1fr_10rem_auto] sm:items-end"
        onSubmit={(e) => {
          e.preventDefault();
          if (moneyFrom(amount)) create.mutate();
        }}
      >
        <SelectField
          label={t('filters.type')}
          value={type}
          onChange={(e) => setType(e.target.value as TaskType)}
        >
          {TYPES.map((ty) => (
            <option key={ty} value={ty}>
              {t(`collab:type.${ty}`)}
            </option>
          ))}
        </SelectField>
        <TextField
          label={t('finance.amount')}
          inputMode="decimal"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <Button type="submit" busy={create.isPending} disabled={!moneyFrom(amount)}>
          {t('finance.newRule')}
        </Button>
      </form>
    </section>
  );
}

function Payouts() {
  const { t, i18n } = useTranslation('admin');
  const query = useQuery({
    queryKey: ['admin', 'payouts'],
    queryFn: () => api.get<Page<PayoutRow>>('/admin/payouts?size=20'),
    networkMode: 'online',
  });
  return (
    <section className="grid gap-3" aria-labelledby="payouts">
      <SectionTitle id="payouts">{t('finance.payouts')}</SectionTitle>
      {query.data?.items.length === 0 && <p className="text-support">{t('finance.noPayouts')}</p>}
      <ul className="divide-y divide-line">
        {query.data?.items.map((p) => (
          <li key={p.publicId} className="price-row py-2">
            <span>
              {formatDateTime(p.paidAt, i18n.language)} · {t(`finance.methods.${p.method}`)}
            </span>
            <span className="price-row__leader" aria-hidden="true" />
            <span className="tabular font-semibold">{formatMoney(p.amount, i18n.language)}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
