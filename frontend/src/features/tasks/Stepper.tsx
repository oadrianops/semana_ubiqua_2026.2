import { Check } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../shared/lib/cn';
import { formatDuration, formatTime, remainingFraction } from '../../shared/lib/format';

/** Progresso 1-2-3 sempre visível (14.3). */
export function Stepper({ current }: { current: 1 | 2 | 3 | 4 }) {
  const { t } = useTranslation('collab');
  const steps = [t('step.accept'), t('step.checkIn'), t('step.send')];
  return (
    <ol className="grid grid-cols-3 gap-2" aria-label={t('step.label')}>
      {steps.map((label, i) => {
        const n = i + 1;
        const done = current > n;
        const active = current === n;
        return (
          <li key={label} aria-current={active ? 'step' : undefined} className="grid gap-1.5">
            <span
              className={cn('h-1.5 rounded-full', done || active ? 'bg-brand' : 'bg-[var(--cinza-200)]')}
            />
            <span
              className={cn(
                'flex items-center gap-1.5 text-sm font-semibold',
                active ? 'text-ink' : 'text-support',
              )}
            >
              <span
                className={cn(
                  'display tabular inline-flex size-6 items-center justify-center rounded-full text-xs',
                  done
                    ? 'bg-brand text-on-brand'
                    : active
                      ? 'border-2 border-brand text-brand'
                      : 'border border-line-strong text-support',
                )}
              >
                {done ? <Check size={14} aria-hidden="true" /> : n}
              </span>
              {label}
            </span>
          </li>
        );
      })}
    </ol>
  );
}

/**
 * Prazo como medida (raise da direção): a barra tem o comprimento do tempo que resta. Muda de cor perto do
 * vencimento (RN-02, aviso 15 min antes).
 */
export function DeadlineBar({ start, deadline, label }: { start: string; deadline: string; label: string }) {
  const { t, i18n } = useTranslation('collab');
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(timer);
  }, []);
  const fraction = remainingFraction(start, deadline, now);
  const leftMs = new Date(deadline).getTime() - now.getTime();
  const urgent = leftMs <= 15 * 60_000;
  return (
    <div className="grid gap-1.5">
      <div className="flex items-baseline justify-between gap-3 text-sm">
        <span className="font-semibold text-ink">{label}</span>
        <span className={cn('tabular', urgent ? 'font-bold text-danger' : 'text-support')}>
          {leftMs > 0
            ? t('deadline.left', {
                time: formatDuration(leftMs, i18n.language),
                at: formatTime(deadline, i18n.language),
              })
            : t('deadline.expired')}
        </span>
      </div>
      <div
        className="h-2 overflow-hidden rounded-full bg-[var(--cinza-200)]"
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(fraction * 100)}
      >
        <div
          className={cn('h-full rounded-full', urgent ? 'bg-danger' : 'bg-brand')}
          style={{ width: `${fraction * 100}%` }}
        />
      </div>
    </div>
  );
}
