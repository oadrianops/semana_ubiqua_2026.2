import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page, TaskStatus, TaskSummary, TaskType } from '../../../shared/api/types';
import { formatMoney } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, SelectField, TextareaField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useMarket, useMarkets, useMarketStalls } from '../../public-api';
import { AdminHeader, errorMessage, Pager, Pill, selectClass } from '../ui';

const STATUSES: TaskStatus[] = [
  'OPEN',
  'ON_HOLD',
  'ACCEPTED',
  'CHECKED_IN',
  'SUBMITTED',
  'COMPLETED',
  'CANCELLED',
];
const TYPES: TaskType[] = ['UPDATE_STALL', 'NEW_STALL', 'VERIFY_REPORT'];

/**
 * Tarefas (RF-13): criar, publicar (com comissão ou como voluntária quando o orçamento acabou — RN-10) e
 * cancelar. Uma tarefa ativa por box (RN-01) é garantida pelo banco.
 */
export default function TasksAdminPage() {
  const { t, i18n } = useTranslation('admin');
  const client = useQueryClient();
  const [status, setStatus] = useState<TaskStatus | ''>('OPEN');
  const [market, setMarket] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const markets = useMarkets();
  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (status) params.set('status', status);
  if (market) params.set('market', market);
  const query = useQuery({
    queryKey: ['admin', 'tasks', status, market, page],
    queryFn: () => api.get<Page<TaskSummary>>(`/admin/tasks?${params.toString()}`),
    networkMode: 'online',
  });
  const refresh = () => void client.invalidateQueries({ queryKey: ['admin', 'tasks'] });
  const publish = useMutation({
    mutationFn: ({ id, volunteer }: { id: string; volunteer: boolean }) =>
      api.post(`/moderation/tasks/${id}/publish`, { volunteer }, { retries: 0 }),
    onSuccess: () => (toast.success(t('tasks.published')), refresh()),
    onError: (e) => toast.error(errorMessage(e, t('tasks.error'))),
  });
  const cancel = useMutation({
    mutationFn: (id: string) => api.post(`/moderation/tasks/${id}/cancel`, { reason: null }, { retries: 0 }),
    onSuccess: () => (toast.success(t('tasks.cancelled')), refresh()),
    onError: (e) => toast.error(errorMessage(e, t('tasks.error'))),
  });

  return (
    <div className="grid gap-6">
      <AdminHeader
        title={t('tasks.title')}
        lead={t('tasks.lead')}
        actions={<Button onClick={() => setCreating(true)}>{t('tasks.create')}</Button>}
      />
      <div className="flex flex-wrap gap-3">
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('filters.status')}</span>
          <select
            className={selectClass}
            value={status}
            onChange={(e) => (setStatus(e.target.value as TaskStatus | ''), setPage(0))}
          >
            <option value="">{t('filters.all')}</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {t(`collab:status.${s}`)}
              </option>
            ))}
          </select>
        </label>
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('filters.market')}</span>
          <select
            className={selectClass}
            value={market}
            onChange={(e) => (setMarket(e.target.value), setPage(0))}
          >
            <option value="">{t('filters.all')}</option>
            {markets.data?.map((m) => (
              <option key={m.slug} value={m.slug}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {query.isPending && <LoadingRows rows={5} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.items.length === 0 && (
        <EmptyState title={t('tasks.emptyTitle')} body={t('tasks.emptyBody')} />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {query.data?.items.map((task) => (
          <li key={task.publicId} className="grid gap-2 py-3 sm:grid-cols-[1fr_auto] sm:items-center">
            <div className="grid gap-0.5">
              <p className="font-semibold">
                {t(`collab:type.${task.type}`)}
                {task.stall && (
                  <span className="display tabular ml-1 text-brand">
                    {' '}
                    {t('common:stall.code', { code: task.stall.code })}
                  </span>
                )}
                <span className="ml-1">
                  {' '}
                  <Pill>{t(`collab:status.${task.status}`)}</Pill>
                </span>
              </p>
              <p className="text-sm text-support">
                {task.market.name}
                {task.suggestedSector && ` · ${task.suggestedSector}`} ·{' '}
                <span className="tabular">{formatMoney(task.commission, i18n.language)}</span>
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              {task.status === 'ON_HOLD' && (
                <>
                  <Button size="sm" onClick={() => publish.mutate({ id: task.publicId, volunteer: false })}>
                    {t('tasks.publish')}
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => publish.mutate({ id: task.publicId, volunteer: true })}
                  >
                    {t('tasks.publishVolunteer')}
                  </Button>
                </>
              )}
              {(task.status === 'OPEN' || task.status === 'ON_HOLD') && (
                <Button size="sm" variant="ghost" onClick={() => cancel.mutate(task.publicId)}>
                  {t('tasks.cancel')}
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
      {query.data && <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={setPage} />}
      <CreateTaskSheet open={creating} onOpenChange={setCreating} onCreated={refresh} />
    </div>
  );
}

function CreateTaskSheet({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (o: boolean) => void;
  onCreated: () => void;
}) {
  const { t } = useTranslation('admin');
  const markets = useMarkets();
  const [type, setType] = useState<TaskType>('UPDATE_STALL');
  const [marketSlug, setMarketSlug] = useState('');
  const [stall, setStall] = useState('');
  const [sector, setSector] = useState('');
  const [instructions, setInstructions] = useState('');
  const [urgent, setUrgent] = useState(false);
  const market = useMarket(marketSlug);
  const stalls = useMarketStalls(marketSlug, null, 0);
  const create = useMutation({
    mutationFn: () =>
      api.post(
        '/moderation/tasks',
        {
          type,
          marketSlug,
          stallPublicId: type === 'NEW_STALL' ? null : stall || null,
          sectorSlug: type === 'NEW_STALL' ? sector || null : null,
          instructions: instructions.trim() || null,
          priority: urgent ? 3 : 1,
        },
        { retries: 0 },
      ),
    onSuccess: () => {
      toast.success(t('tasks.created'));
      onOpenChange(false);
      onCreated();
    },
    onError: (e) => toast.error(errorMessage(e, t('tasks.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    create.mutate();
  };
  const needsStall = type !== 'NEW_STALL';
  return (
    <Sheet
      open={open}
      onOpenChange={onOpenChange}
      title={t('tasks.create')}
      description={t('tasks.createBody')}
    >
      <form onSubmit={submit} className="grid gap-4">
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
        <SelectField
          label={t('filters.market')}
          value={marketSlug}
          required
          onChange={(e) => (setMarketSlug(e.target.value), setStall(''), setSector(''))}
        >
          <option value="">{t('tasks.chooseMarket')}</option>
          {markets.data?.map((m) => (
            <option key={m.slug} value={m.slug}>
              {m.name}
            </option>
          ))}
        </SelectField>
        {marketSlug && needsStall && (
          <SelectField
            label={t('tasks.stall')}
            value={stall}
            required
            onChange={(e) => setStall(e.target.value)}
          >
            <option value="">{t('tasks.chooseStall')}</option>
            {stalls.data?.items.map((s) => (
              <option key={s.publicId} value={s.publicId}>
                {s.code}
                {s.name ? ` · ${s.name}` : ''}
              </option>
            ))}
          </SelectField>
        )}
        {marketSlug && !needsStall && (
          <SelectField label={t('tasks.sector')} value={sector} onChange={(e) => setSector(e.target.value)}>
            <option value="">{t('tasks.anySector')}</option>
            {market.data?.sectors.map((s) => (
              <option key={s.slug} value={s.slug}>
                {s.name}
              </option>
            ))}
          </SelectField>
        )}
        <TextareaField
          label={t('tasks.instructions')}
          maxLength={500}
          value={instructions}
          onChange={(e) => setInstructions(e.target.value)}
        />
        <CheckboxField
          label={t('tasks.urgent')}
          checked={urgent}
          onChange={(e) => setUrgent(e.target.checked)}
        />
        <Button type="submit" block busy={create.isPending} disabled={!marketSlug || (needsStall && !stall)}>
          {t('tasks.createAction')}
        </Button>
      </form>
    </Sheet>
  );
}
