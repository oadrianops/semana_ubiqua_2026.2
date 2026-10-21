import { CloudOff, RefreshCw, SearchX, TriangleAlert } from 'lucide-react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../api/http';
import { cn } from '../lib/cn';
import { Button } from './Button';

/** Esqueleto de lista com altura fixa: sem saltos de layout (9.14). */
export function LoadingRows({ rows = 4, label }: { rows?: number; label?: string }) {
  const { t } = useTranslation();
  return (
    <div role="status" aria-live="polite" className="divide-y divide-line">
      <span className="sr-only">{label ?? t('state.loading')}</span>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex h-16 items-center gap-3 py-3">
          <div className="skeleton h-4 w-2/5" />
          <div className="flex-1" />
          <div className="skeleton h-4 w-16" />
        </div>
      ))}
    </div>
  );
}

function StateBlock({
  icon,
  title,
  body,
  action,
  tone = 'neutral',
}: {
  icon: ReactNode;
  title: ReactNode;
  body?: ReactNode;
  action?: ReactNode;
  tone?: 'neutral' | 'danger';
}) {
  return (
    <div className="flex gap-3 py-6" role={tone === 'danger' ? 'alert' : undefined}>
      <span
        className={cn('mt-0.5 shrink-0', tone === 'danger' ? 'text-danger' : 'text-muted')}
        aria-hidden="true"
      >
        {icon}
      </span>
      <div className="grid gap-2">
        <p className="font-semibold text-ink">{title}</p>
        {body && <div className="max-w-[var(--measure)] text-support">{body}</div>}
        {action && <div className="pt-1">{action}</div>}
      </div>
    </div>
  );
}

export function EmptyState({
  title,
  body,
  action,
}: {
  title: ReactNode;
  body?: ReactNode;
  action?: ReactNode;
}) {
  return <StateBlock icon={<SearchX size={22} />} title={title} body={body} action={action} />;
}

/** Erro com orientação útil, sem pedir desculpas e sem código técnico (14.8). */
export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { t } = useTranslation();
  const api = error instanceof ApiError ? error : null;
  if (api?.isNetwork) return <OfflineState onRetry={onRetry} />;
  if (api?.status === 404) {
    return (
      <StateBlock
        icon={<SearchX size={22} />}
        title={t('state.notFoundTitle')}
        body={t('state.notFoundBody')}
      />
    );
  }
  return (
    <StateBlock
      tone="danger"
      icon={<TriangleAlert size={22} />}
      title={api?.problem.title ?? t('state.errorTitle')}
      body={api?.problem.detail ?? t('state.errorBody')}
      action={
        onRetry && (
          <Button variant="secondary" size="sm" icon={<RefreshCw size={16} />} onClick={onRetry}>
            {t('action.retry')}
          </Button>
        )
      }
    />
  );
}

/** Sem conexão e sem dados salvos: explica o que fazer. */
export function OfflineState({ onRetry, body }: { onRetry?: () => void; body?: ReactNode }) {
  const { t } = useTranslation();
  return (
    <StateBlock
      icon={<CloudOff size={22} />}
      title={t('state.offlineTitle')}
      body={body ?? t('state.offlineBody')}
      action={
        onRetry && (
          <Button variant="secondary" size="sm" icon={<RefreshCw size={16} />} onClick={onRetry}>
            {t('action.retry')}
          </Button>
        )
      }
    />
  );
}
