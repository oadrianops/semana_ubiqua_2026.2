/**
 * Formatação por idioma com Intl, fuso America/Belem e moeda sempre BRL (RF-06, RN-23, RN-24).
 * Nada de moment.js (8.6).
 */
export const TIME_ZONE = 'America/Belem';

export interface Money {
  amount: string;
  currency: string;
}

export function intlLocale(lng: string): string {
  if (lng.startsWith('en')) return 'en-US';
  if (lng.startsWith('es')) return 'es';
  return 'pt-BR';
}

export function formatMoney(money: Money | null | undefined, lng: string): string | null {
  if (!money) return null;
  return new Intl.NumberFormat(intlLocale(lng), { style: 'currency', currency: 'BRL' }).format(
    Number(money.amount),
  );
}

export function formatNumber(value: number, lng: string, digits = 0): string {
  return new Intl.NumberFormat(intlLocale(lng), { maximumFractionDigits: digits }).format(value);
}

/** "19/10 às 15:40" no PT-BR (14.8); o texto de ligação vem do i18n. */
export function formatDayMonth(iso: string | Date, lng: string): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return new Intl.DateTimeFormat(intlLocale(lng), {
    day: '2-digit',
    month: '2-digit',
    timeZone: TIME_ZONE,
  }).format(d);
}

export function formatTime(iso: string | Date, lng: string): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return new Intl.DateTimeFormat(intlLocale(lng), {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: TIME_ZONE,
  }).format(d);
}

export function formatDate(iso: string | Date, lng: string): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return new Intl.DateTimeFormat(intlLocale(lng), { dateStyle: 'medium', timeZone: TIME_ZONE }).format(d);
}

export function formatDateTime(iso: string | Date, lng: string): string {
  const d = typeof iso === 'string' ? new Date(iso) : iso;
  return new Intl.DateTimeFormat(intlLocale(lng), {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: TIME_ZONE,
  }).format(d);
}

/** Dias inteiros desde uma data (para "verificado há N dias"). */
export function daysSince(iso: string | null | undefined, now: Date = new Date()): number | null {
  if (!iso) return null;
  const ms = now.getTime() - new Date(iso).getTime();
  return Math.max(0, Math.floor(ms / 86_400_000));
}

/** Tempo relativo curto com Intl.RelativeTimeFormat ("há 3 dias", "3 days ago", "hace 3 días"). */
export function relativeFromNow(iso: string, lng: string, now: Date = new Date()): string {
  const diffSec = Math.round((new Date(iso).getTime() - now.getTime()) / 1000);
  const rtf = new Intl.RelativeTimeFormat(intlLocale(lng), { numeric: 'auto' });
  const abs = Math.abs(diffSec);
  if (abs < 60) return rtf.format(diffSec, 'second');
  if (abs < 3600) return rtf.format(Math.round(diffSec / 60), 'minute');
  if (abs < 86_400) return rtf.format(Math.round(diffSec / 3600), 'hour');
  return rtf.format(Math.round(diffSec / 86_400), 'day');
}

/** Minutos restantes até um prazo (barra proporcional do stepper). */
export function remainingFraction(start: string, deadline: string, now: Date = new Date()): number {
  const total = new Date(deadline).getTime() - new Date(start).getTime();
  const left = new Date(deadline).getTime() - now.getTime();
  if (total <= 0) return 0;
  return Math.min(1, Math.max(0, left / total));
}

export function formatDuration(ms: number, lng: string): string {
  const totalMin = Math.max(0, Math.round(ms / 60_000));
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  const nf = new Intl.NumberFormat(intlLocale(lng), { minimumIntegerDigits: 2 });
  return h > 0 ? `${h}h${nf.format(m)}` : `${m} min`;
}
