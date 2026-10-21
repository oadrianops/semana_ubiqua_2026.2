import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page } from '../../../shared/api/types';
import { relativeFromNow } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import type { ReportStatus, ReportView } from '../types';
import { AdminHeader, errorMessage, Pager, Pill, selectClass } from '../ui';

const STATUSES: ReportStatus[] = ['OPEN', 'TASK_CREATED', 'RESOLVED', 'DISMISSED'];

/**
 * Denúncias (RF-07, RF-33). Ao atingir o limite, o sistema já abre a tarefa de verificação (RN-15); aqui a
 * moderação também pode abrir a verificação na hora ou descartar a denúncia.
 */
export default function ReportsPage() {
  const { t, i18n } = useTranslation('admin');
  const client = useQueryClient();
  const [status, setStatus] = useState<ReportStatus>('OPEN');
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ['admin', 'reports', status, page],
    queryFn: () => api.get<Page<ReportView>>(`/moderation/reports?status=${status}&page=${page}&size=20`),
    networkMode: 'online',
  });
  const refresh = () => void client.invalidateQueries({ queryKey: ['admin', 'reports'] });
  const dismiss = useMutation({
    mutationFn: (id: string) => api.post(`/moderation/reports/${id}/dismiss`, undefined, { retries: 0 }),
    onSuccess: () => (toast.success(t('reports.dismissed')), refresh()),
    onError: (e) => toast.error(errorMessage(e, t('reports.error'))),
  });
  const verify = useMutation({
    mutationFn: (r: ReportView) =>
      api.post(
        '/moderation/tasks',
        {
          type: 'VERIFY_REPORT',
          marketSlug: r.marketSlug,
          stallPublicId: r.stallId,
          instructions: r.comment,
          priority: 2,
        },
        { retries: 0 },
      ),
    onSuccess: () => (toast.success(t('reports.taskCreated')), refresh()),
    onError: (e) => toast.error(errorMessage(e, t('reports.error'))),
  });

  return (
    <div className="grid gap-6">
      <AdminHeader title={t('reports.title')} lead={t('reports.lead')} />
      <label className="grid gap-1 justify-self-start">
        <span className="text-sm font-semibold text-support">{t('filters.status')}</span>
        <select
          className={selectClass}
          value={status}
          onChange={(e) => (setStatus(e.target.value as ReportStatus), setPage(0))}
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {t(`reports.status.${s}`)}
            </option>
          ))}
        </select>
      </label>
      {query.isPending && <LoadingRows rows={5} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.items.length === 0 && (
        <EmptyState title={t('reports.emptyTitle')} body={t('reports.emptyBody')} />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {query.data?.items.map((r) => (
          <li key={r.publicId} className="grid gap-2 py-3">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <p className="font-semibold">
                <Link to={`/stalls/${r.stallId}`} className="display tabular">
                  {t('common:stall.code', { code: r.stallCode })}
                </Link>{' '}
                · {r.marketName}
                {r.productName && ` · ${r.productName}`}
              </p>
              <Pill tone="warn">{t(`public:report.reasons.${r.reason}`)}</Pill>
            </div>
            {r.comment && <p className="text-ink">“{r.comment}”</p>}
            <p className="text-sm text-support">{relativeFromNow(r.createdAt, i18n.language)}</p>
            {r.status === 'OPEN' && (
              <div className="flex flex-wrap gap-2">
                <Button
                  size="sm"
                  busy={verify.isPending && verify.variables?.publicId === r.publicId}
                  onClick={() => verify.mutate(r)}
                >
                  {t('reports.createTask')}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  busy={dismiss.isPending && dismiss.variables === r.publicId}
                  onClick={() => dismiss.mutate(r.publicId)}
                >
                  {t('reports.dismiss')}
                </Button>
              </div>
            )}
          </li>
        ))}
      </ul>
      {query.data && <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={setPage} />}
    </div>
  );
}
