import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError } from '../../shared/api/http';
import { cn } from '../../shared/lib/cn';
import { Button } from '../../shared/ui/Button';

/** Cabeçalho de cada tela do painel: título curto, contexto em uma linha e ações à direita. */
export function AdminHeader({
  title,
  lead,
  actions,
}: {
  title: string;
  lead?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-3">
      <div className="grid gap-1">
        <h1 className="text-3xl">{title}</h1>
        {lead && <p className="max-w-[var(--measure)] text-support">{lead}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  );
}

export function SectionTitle({
  id,
  children,
  aside,
}: {
  id?: string;
  children: ReactNode;
  aside?: ReactNode;
}) {
  return (
    <h2 id={id} className="flex items-baseline justify-between gap-3 border-b-2 border-ink pb-1 text-xl">
      {children}
      {aside && <span className="text-sm font-normal text-support">{aside}</span>}
    </h2>
  );
}

/** Rótulo de estado em texto (nunca só cor — 14.7). */
export function Pill({
  tone = 'neutral',
  children,
}: {
  tone?: 'neutral' | 'good' | 'warn' | 'bad' | 'brand';
  children: ReactNode;
}) {
  const tones = {
    neutral: 'border-line-strong text-support',
    good: 'border-success text-success',
    warn: 'border-warning text-warning',
    bad: 'border-danger text-danger',
    brand: 'border-brand text-brand',
  };
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold whitespace-nowrap',
        tones[tone],
      )}
    >
      {children}
    </span>
  );
}

export function Pager({
  page,
  totalPages,
  onPage,
}: {
  page: number;
  totalPages: number;
  onPage: (p: number) => void;
}) {
  const { t } = useTranslation('admin');
  if (totalPages <= 1) return null;
  return (
    <nav className="flex items-center justify-between gap-3 pt-3" aria-label={t('pager.label')}>
      <Button variant="secondary" size="sm" disabled={page <= 0} onClick={() => onPage(page - 1)}>
        {t('pager.prev')}
      </Button>
      <span className="tabular text-sm text-support">
        {t('pager.position', { page: page + 1, total: totalPages })}
      </span>
      <Button
        variant="secondary"
        size="sm"
        disabled={page + 1 >= totalPages}
        onClick={() => onPage(page + 1)}
      >
        {t('pager.next')}
      </Button>
    </nav>
  );
}

export function errorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError ? (error.problem.detail ?? fallback) : fallback;
}

/** Texto curto de um valor do diff ("—" para vazio). */
export function show(value: unknown): string {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'object') {
    const v = value as { amount?: string };
    if (v.amount) return `R$ ${v.amount.replace('.', ',')}`;
    return JSON.stringify(value);
  }
  return String(value);
}

/** "12,50" → { amount: "12.50", currency: "BRL" }; inválido → null. */
export function moneyFrom(text: string): { amount: string; currency: 'BRL' } | null {
  const clean = text.trim().replace(/\s|R\$/g, '');
  const normalized = clean.includes(',') ? clean.replace(/\./g, '').replace(',', '.') : clean;
  if (!/^\d{1,7}(\.\d{1,2})?$/.test(normalized)) return null;
  return { amount: Number(normalized).toFixed(2), currency: 'BRL' };
}

export const selectClass =
  'min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3 text-base';
