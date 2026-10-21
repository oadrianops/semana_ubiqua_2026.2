import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../../shared/api/http';
import type { Page } from '../../../shared/api/types';
import { formatDateTime } from '../../../shared/lib/format';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import type { AuditView } from '../types';
import { AdminHeader, Pager, selectClass } from '../ui';

const ENTITIES = [
  '',
  'submission',
  'stall',
  'market',
  'product',
  'category',
  'user',
  'payout',
  'ledger',
  'task',
];

/** Auditoria (RNF-10, só ADMIN): quem fez o quê, quando e o antes/depois. Somente leitura. */
export default function AuditPage() {
  const { t, i18n } = useTranslation('admin');
  const [entity, setEntity] = useState('');
  const [page, setPage] = useState(0);
  const params = new URLSearchParams({ page: String(page), size: '50' });
  if (entity) params.set('entityType', entity);
  const query = useQuery({
    queryKey: ['admin', 'audit', entity, page],
    queryFn: () => api.get<Page<AuditView>>(`/admin/audit-log?${params.toString()}`),
    networkMode: 'online',
  });
  return (
    <div className="grid gap-6">
      <AdminHeader title={t('audit.title')} lead={t('audit.lead')} />
      <label className="grid gap-1 justify-self-start">
        <span className="text-sm font-semibold text-support">{t('audit.entity')}</span>
        <select
          className={selectClass}
          value={entity}
          onChange={(e) => (setEntity(e.target.value), setPage(0))}
        >
          {ENTITIES.map((e) => (
            <option key={e} value={e}>
              {e ? e : t('filters.all')}
            </option>
          ))}
        </select>
      </label>
      {query.isPending && <LoadingRows rows={8} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.items.length === 0 && (
        <EmptyState title={t('audit.emptyTitle')} body={t('audit.emptyBody')} />
      )}
      <ol className="divide-y divide-line border-t-2 border-ink">
        {query.data?.items.map((a, i) => (
          <li key={`${a.createdAt}-${i}`} className="grid gap-1 py-3">
            <p className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="font-semibold">
                <code className="tabular">{a.action}</code> · {a.entityType}{' '}
                <span className="text-support">{a.entityId}</span>
              </span>
              <span className="text-sm text-support">{formatDateTime(a.createdAt, i18n.language)}</span>
            </p>
            <p className="text-sm text-support">
              {t('audit.actor', { id: a.actorUserId ?? '—' })}
              {a.requestId && ` · ${t('audit.request', { id: a.requestId })}`}
            </p>
            {(a.before || a.after) && (
              <details className="text-sm">
                <summary className="min-h-11 cursor-pointer content-center font-semibold text-brand">
                  {t('audit.details')}
                </summary>
                <pre
                  className="overflow-x-auto rounded-[var(--radius-control)] bg-soft p-3 text-xs"
                  role="region"
                  aria-label={t('audit.details')}
                  tabIndex={0}
                >
                  {JSON.stringify({ before: a.before, after: a.after }, null, 2)}
                </pre>
              </details>
            )}
          </li>
        ))}
      </ol>
      {query.data && <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={setPage} />}
    </div>
  );
}
