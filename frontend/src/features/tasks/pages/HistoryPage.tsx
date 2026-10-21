import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import type { HistoryItem } from '../../../shared/api/types';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { cn } from '../../../shared/lib/cn';
import { formatDateTime, formatMoney } from '../../../shared/lib/format';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useHistory } from '../api';

/** Histórico (RF-23): cada tarefa com o resultado e, se recusada, o motivo e o comentário da moderação. */
export default function HistoryPage() {
  useNamespaces(['collab']);
  const { t } = useTranslation('collab');
  const history = useHistory();

  return (
    <div className="safe-x mx-auto grid max-w-3xl gap-6 py-6">
      <header className="grid gap-2">
        <h1 className="text-3xl">{t('history.title')}</h1>
        <p className="text-support">{t('history.lead')}</p>
      </header>
      {history.isPending && <LoadingRows rows={5} />}
      {history.isError && <ErrorState error={history.error} onRetry={() => void history.refetch()} />}
      {history.data?.length === 0 && (
        <EmptyState
          title={t('history.emptyTitle')}
          body={t('history.emptyBody')}
          action={
            <Link to="/collaborate" className="font-semibold">
              {t('history.seeTasks')}
            </Link>
          }
        />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {history.data?.map((item) => (
          <HistoryRow key={`${item.taskId}-${item.acceptedAt}`} item={item} />
        ))}
      </ul>
    </div>
  );
}

function outcomeKey(item: HistoryItem): string {
  if (item.submissionStatus) return `submission.${item.submissionStatus}`;
  if (item.outcome) return `outcome.${item.outcome}`;
  return `status.${item.taskStatus}`;
}

function HistoryRow({ item }: { item: HistoryItem }) {
  const { t, i18n } = useTranslation('collab');
  const key = outcomeKey(item);
  const tone =
    item.submissionStatus === 'APPROVED'
      ? 'text-success'
      : item.submissionStatus === 'REJECTED' || item.outcome === 'EXPIRED'
        ? 'text-danger'
        : 'text-support';
  const active = !item.outcome && (item.taskStatus === 'ACCEPTED' || item.taskStatus === 'CHECKED_IN');
  return (
    <li className="grid gap-1 py-3">
      <div className="flex items-baseline justify-between gap-3">
        <p className="font-semibold text-ink">
          {active ? (
            <Link to={`/collaborate/tasks/${item.taskId}`}>{t(`type.${item.type}`)}</Link>
          ) : (
            t(`type.${item.type}`)
          )}
          {item.stallCode && (
            <span className="display tabular ml-1 text-brand">
              {' '}
              {t('common:stall.code', { code: item.stallCode })}
            </span>
          )}
        </p>
        <span className="display tabular shrink-0 text-lg">
          {formatMoney(item.commission, i18n.language)}
        </span>
      </div>
      <p className="text-sm text-support">
        {item.marketName} · {formatDateTime(item.submittedAt ?? item.acceptedAt, i18n.language)}
      </p>
      <p className={cn('text-sm font-semibold', tone)}>{t(key)}</p>
      {item.rejectionReason && (
        <div className="rounded-[var(--radius-control)] bg-soft p-3 text-sm">
          <p className="font-semibold text-ink">
            {t(`rejection.${item.rejectionReason}`, { defaultValue: item.rejectionReason })}
          </p>
          {item.reviewComment && <p className="text-support">{item.reviewComment}</p>}
        </div>
      )}
    </li>
  );
}
