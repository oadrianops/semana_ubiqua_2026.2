import { useDocumentTitle } from '../../../shared/lib/useDocumentTitle';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { api } from '../../../shared/api/http';
import type { ReputationView, Statement } from '../../../shared/api/types';
import { useNamespaces } from '../../../shared/i18n/useNamespaces';
import { cn } from '../../../shared/lib/cn';
import { formatDateTime, formatMoney } from '../../../shared/lib/format';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';

/**
 * Ganhos (RF-26): saldo, total repassado e extrato com saldo após cada lançamento. O repasse é feito por fora do
 * app, por PIX, pela administração (sem pagamento online — Won't). Mostra também a reputação (RN-17).
 */
export default function EarningsPage() {
  useNamespaces(['collab']);
  const { t, i18n } = useTranslation('collab');
  const { user } = useAuth();
  const statement = useQuery({
    queryKey: ['me', 'ledger'],
    queryFn: () => api.get<Statement>('/me/ledger?size=100'),
    networkMode: 'online',
  });
  const reputation = useQuery({
    queryKey: ['me', 'reputation'],
    queryFn: () => api.get<ReputationView>('/me/reputation'),
    networkMode: 'online',
  });
  const lng = i18n.language;
  useDocumentTitle(t('earnings.title'));

  return (
    <div className="safe-x mx-auto grid max-w-3xl gap-8 py-6">
      <header className="grid gap-2">
        <h1 className="text-3xl">{t('earnings.title')}</h1>
        <p className="text-support">{t('earnings.lead')}</p>
      </header>

      {statement.isPending && <LoadingRows rows={3} />}
      {statement.isError && <ErrorState error={statement.error} onRetry={() => void statement.refetch()} />}
      {statement.data && (
        <section
          aria-labelledby="balance"
          className="grid gap-3 rounded-[var(--radius-panel)] bg-band p-5 text-on-band"
        >
          <h2 id="balance" className="text-base font-semibold text-on-band-muted">
            {t('earnings.balance')}
          </h2>
          <p className="display tabular text-5xl text-highlight">
            {formatMoney(statement.data.balance, lng)}
          </p>
          <dl className="grid gap-1 text-sm">
            <div className="flex justify-between gap-3">
              <dt className="text-on-band-muted">{t('earnings.paidOut')}</dt>
              <dd className="tabular font-semibold">{formatMoney(statement.data.totalPaidOut, lng)}</dd>
            </div>
            <div className="flex justify-between gap-3">
              <dt className="text-on-band-muted">{t('earnings.minimum')}</dt>
              <dd className="tabular font-semibold">{formatMoney(statement.data.minimumPayout, lng)}</dd>
            </div>
          </dl>
          <p className="text-sm text-on-band-muted">
            {user?.pixKeyMasked ? t('earnings.pixSet', { key: user.pixKeyMasked }) : t('earnings.pixMissing')}{' '}
            <Link to="/profile" className="font-semibold text-on-band">
              {t('earnings.pixLink')}
            </Link>
          </p>
        </section>
      )}

      {reputation.data && (
        <section aria-labelledby="reputation" className="grid gap-2">
          <h2
            id="reputation"
            className="flex items-baseline justify-between border-b-2 border-ink pb-1 text-xl"
          >
            {t('reputation.title')}
            <span className="display tabular text-2xl">{reputation.data.score}</span>
          </h2>
          <p className="text-sm text-support">
            {t('reputation.body', {
              window: reputation.data.window,
              minimum: reputation.data.minimumToAccept,
            })}
          </p>
          <dl className="grid grid-cols-3 gap-2 text-center">
            {(['approved', 'rejected', 'expired'] as const).map((k) => (
              <div key={k} className="rounded-[var(--radius-control)] bg-soft p-2">
                <dt className="text-sm text-support">{t(`reputation.${k}`)}</dt>
                <dd className="display tabular text-xl text-ink">{reputation.data[k]}</dd>
              </div>
            ))}
          </dl>
        </section>
      )}

      {statement.data && (
        <section aria-labelledby="entries" className="grid">
          <h2 id="entries" className="border-b-2 border-ink pb-1 text-xl">
            {t('earnings.statement')}
          </h2>
          {statement.data.entries.items.length === 0 && (
            <EmptyState title={t('earnings.emptyTitle')} body={t('earnings.emptyBody')} />
          )}
          <ul className="divide-y divide-line">
            {statement.data.entries.items.map((entry) => {
              const negative = entry.amount.amount.startsWith('-');
              return (
                <li key={entry.publicId} className="grid grid-cols-[1fr_auto] gap-x-3 py-3">
                  <span className="font-semibold text-ink">{t(`ledger.${entry.type}`)}</span>
                  <span className={cn('display tabular text-lg', negative ? 'text-danger' : 'text-ink')}>
                    {formatMoney(entry.amount, lng)}
                  </span>
                  <span className="text-sm text-support">
                    {describeEntry(entry.description, t)} · {formatDateTime(entry.createdAt, lng)}
                  </span>
                  <span className="tabular text-sm text-support">
                    {t('earnings.after', { value: formatMoney(entry.balanceAfter, lng) })}
                  </span>
                </li>
              );
            })}
          </ul>
        </section>
      )}
    </div>
  );
}

/**
 * A descrição do crédito vem gravada no extrato imutável como "Tarefa UPDATE_STALL — box 18 (Ver-o-Peso)";
 * aqui ela vira "Atualizar box · Box 18 · Ver-o-Peso" no idioma da pessoa. Outras descrições passam como estão.
 */
export function describeEntry(
  description: string,
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  const m = /^Tarefa (UPDATE_STALL|NEW_STALL|VERIFY_REPORT) — box (\S+) \((.+)\)$/.exec(description);
  if (!m) return description;
  return `${t(`type.${m[1]}`)} · ${t('common:stall.code', { code: m[2] })} · ${m[3]}`;
}
