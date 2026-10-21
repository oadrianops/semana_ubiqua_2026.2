import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { api } from '../../../shared/api/http';
import type { Page, TaskType } from '../../../shared/api/types';
import { relativeFromNow } from '../../../shared/lib/format';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useMarkets } from '../../public-api';
import type { QueueItem } from '../types';
import { AdminHeader, Pager, Pill, selectClass } from '../ui';

const TYPES: TaskType[] = ['UPDATE_STALL', 'NEW_STALL', 'VERIFY_REPORT'];

/** Fila de moderação (RF-21): mais antigos primeiro, com o alerta de check-in de baixa precisão à vista. */
export default function QueuePage() {
  const { t, i18n } = useTranslation('admin');
  const [market, setMarket] = useState('');
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);
  const markets = useMarkets();
  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (market) params.set('market', market);
  if (type) params.set('type', type);
  const query = useQuery({
    queryKey: ['admin', 'queue', market, type, page],
    queryFn: () => api.get<Page<QueueItem>>(`/moderation/submissions?${params.toString()}`),
    refetchInterval: 30_000,
    networkMode: 'online',
  });

  return (
    <div className="grid gap-6">
      <AdminHeader title={t('queue.title')} lead={t('queue.lead')} />
      <div className="flex flex-wrap gap-3">
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
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('filters.type')}</span>
          <select
            className={selectClass}
            value={type}
            onChange={(e) => (setType(e.target.value), setPage(0))}
          >
            <option value="">{t('filters.all')}</option>
            {TYPES.map((ty) => (
              <option key={ty} value={ty}>
                {t(`collab:type.${ty}`)}
              </option>
            ))}
          </select>
        </label>
      </div>
      {query.isPending && <LoadingRows rows={6} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.items.length === 0 && (
        <EmptyState title={t('queue.emptyTitle')} body={t('queue.emptyBody')} />
      )}
      {query.data && query.data.items.length > 0 && (
        <ul className="divide-y divide-line border-t-2 border-ink">
          {query.data.items.map((item) => (
            <li key={item.publicId}>
              <Link
                to={`/admin/moderation/${item.publicId}`}
                className="grid gap-1 py-3 text-ink no-underline hover:bg-soft sm:grid-cols-[1fr_auto] sm:items-center"
              >
                <span className="grid gap-0.5">
                  <span className="font-semibold">
                    {t(`collab:type.${item.type}`)}
                    {item.stallCode && (
                      <span className="display tabular ml-1 text-brand">
                        {' '}
                        {t('common:stall.code', { code: item.stallCode })}
                      </span>
                    )}
                    {item.stallName && <span className="text-support"> · {item.stallName}</span>}
                  </span>
                  <span className="text-sm text-support">
                    {item.marketName} · {item.collaboratorName} ·{' '}
                    {relativeFromNow(item.createdAt, i18n.language)}
                  </span>
                </span>
                {item.checkinFlag && <Pill tone="warn">{t('review.lowAccuracy')}</Pill>}
              </Link>
            </li>
          ))}
        </ul>
      )}
      {query.data && <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={setPage} />}
    </div>
  );
}
