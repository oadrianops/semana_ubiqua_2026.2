import { useDocumentTitle } from '../../../shared/lib/useDocumentTitle';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { api } from '../../../shared/api/http';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { cn } from '../../../shared/lib/cn';
import { relativeFromNow } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useNotificationsQuery } from '../useNotifications';

/**
 * Notificações (RF-14). As não lidas levam a marca "novo" até serem vistas (painel de embarque: o que mudou fica
 * marcado). Abrir a tela marca tudo como lido depois de um instante.
 */
export default function NotificationsPage() {
  useNamespaces(['collab']);
  const { t, i18n } = useTranslation('collab');
  useDocumentTitle(t('notifications.title'));
  const query = useNotificationsQuery(0);
  const client = useQueryClient();
  const markAll = useMutation({
    mutationFn: () => api.post('/me/notifications/read', { ids: [], all: true }, { retries: 0 }),
    onSettled: () => client.invalidateQueries({ queryKey: ['me', 'notifications'] }),
  });
  const marked = useRef(false);

  useEffect(() => {
    if (marked.current || !query.data?.unread) return;
    marked.current = true;
    const timer = setTimeout(() => markAll.mutate(), 2500);
    return () => clearTimeout(timer);
  }, [query.data?.unread, markAll]);

  return (
    <div className="safe-x mx-auto grid max-w-3xl gap-6 py-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-3xl">{t('notifications.title')}</h1>
        {Boolean(query.data?.unread) && (
          <Button variant="ghost" size="sm" busy={markAll.isPending} onClick={() => markAll.mutate()}>
            {t('notifications.markAll')}
          </Button>
        )}
      </header>
      {query.isPending && <LoadingRows rows={5} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.page.items.length === 0 && (
        <EmptyState title={t('notifications.emptyTitle')} body={t('notifications.emptyBody')} />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {query.data?.page.items.map((n) => {
          const unread = !n.readAt;
          const body = (
            <>
              <span className="flex items-baseline justify-between gap-3">
                <span className={cn('text-ink', unread ? 'font-bold' : 'font-semibold')}>
                  {n.title}
                  {unread && (
                    <span className="ml-2 inline-flex items-center gap-1 align-middle text-xs font-bold text-brand">
                      <span aria-hidden="true" className="inline-block size-2 rounded-full bg-brand" />
                      {t('notifications.new')}
                    </span>
                  )}
                </span>
                <span className="shrink-0 text-sm text-support">
                  {relativeFromNow(n.createdAt, i18n.language)}
                </span>
              </span>
              <span className="text-support">{n.body}</span>
            </>
          );
          return (
            <li key={n.publicId}>
              {n.link ? (
                <Link to={n.link} className="grid gap-0.5 py-3 no-underline hover:bg-soft">
                  {body}
                </Link>
              ) : (
                <div className="grid gap-0.5 py-3">{body}</div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
