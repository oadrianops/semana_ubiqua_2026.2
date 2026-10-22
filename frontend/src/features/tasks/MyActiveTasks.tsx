import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { listItems, subscribeOutbox, type OutboxItem } from '../../shared/offline/outbox';
import { useHistory } from './api';

/** Tarefas em andamento e envios na fila, no topo da lista (14.3: prazos sempre visíveis). */
export function MyActiveTasks() {
  const { t } = useTranslation('collab');
  const history = useHistory();
  const [outbox, setOutbox] = useState<OutboxItem[]>([]);

  useEffect(() => {
    const load = () => void listItems().then(setOutbox);
    load();
    return subscribeOutbox(load);
  }, []);

  const active = (history.data ?? []).filter(
    (h) => h.outcome === null && (h.taskStatus === 'ACCEPTED' || h.taskStatus === 'CHECKED_IN'),
  );
  const queued = outbox.filter((o) => o.state !== 'SENT');
  // Envio entregue continua visível aqui até a moderação decidir; o resultado fica no histórico.
  const inReview = (history.data ?? []).filter(
    (h) => h.submissionStatus === 'PENDING' && !queued.some((o) => o.taskId === h.taskId),
  );
  if (!active.length && !queued.length && !inReview.length) return null;

  return (
    <section
      aria-labelledby="active-tasks"
      className="grid gap-1 rounded-[var(--radius-panel)] border-2 border-brand p-4"
    >
      <h2 id="active-tasks" className="text-lg">
        {t('tasks.activeTitle')}
      </h2>
      <ul className="divide-y divide-line">
        {active.map((h) => (
          <li key={h.taskId}>
            <Link
              to={`/collaborate/tasks/${h.taskId}`}
              className="flex min-h-12 items-center justify-between gap-3 py-2 font-semibold no-underline"
            >
              <span>
                {t(`type.${h.type}`)} · {h.marketName}
                {h.stallCode && ` · ${t('common:stall.code', { code: h.stallCode })}`}
              </span>
              <span className="text-sm text-support">{t(`status.${h.taskStatus}`)}</span>
            </Link>
          </li>
        ))}
        {inReview.map((h) => (
          <li key={`review-${h.taskId}`}>
            <Link
              to="/collaborate/history"
              className="flex min-h-12 items-center justify-between gap-3 py-2 no-underline"
            >
              <span className="font-semibold">
                {t(`type.${h.type}`)} · {h.marketName}
                {h.stallCode && ` · ${t('common:stall.code', { code: h.stallCode })}`}
              </span>
              <span className="text-sm text-support">{t('submission.PENDING')}</span>
            </Link>
          </li>
        ))}
        {queued.map((o) => (
          <li key={o.taskId}>
            <Link
              to={`/collaborate/tasks/${o.taskId}`}
              className="flex min-h-12 items-center justify-between gap-3 py-2 no-underline"
            >
              <span className="font-semibold">{t(`type.${o.changeSet.taskType}`)}</span>
              <span className="text-sm text-support">{t(`outbox.${o.state}`)}</span>
            </Link>
          </li>
        ))}
      </ul>
      <Link to="/collaborate/history" className="justify-self-start py-2 text-sm font-semibold">
        {t('tasks.seeHistory')}
      </Link>
    </section>
  );
}
