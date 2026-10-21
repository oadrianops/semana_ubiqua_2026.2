import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { api } from '../../../shared/api/http';
import { formatDateTime, formatMoney, formatNumber } from '../../../shared/lib/format';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import type { MetricsOverview } from '../types';
import { AdminHeader, SectionTitle } from '../ui';

/**
 * Indicadores (RF-32, 17): cobertura e frescor do acervo, fila de moderação, finanças do mês e busca. Números em
 * fonte tabular, cada um com o rótulo do que mede.
 */
export default function OverviewPage() {
  const { t, i18n } = useTranslation('admin');
  const lng = i18n.language;
  const query = useQuery({
    queryKey: ['admin', 'metrics'],
    queryFn: () => api.get<MetricsOverview>('/admin/metrics/overview'),
    networkMode: 'online',
  });

  if (query.isPending) return <LoadingRows rows={6} />;
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />;
  const m = query.data;
  const pct = (v: number | null) => (v == null ? '—' : `${formatNumber(v, lng, 1)}%`);

  const groups: { title: string; rows: [string, string][] }[] = [
    {
      title: t('overview.collection'),
      rows: [
        [t('overview.publishedStalls'), formatNumber(m.publishedStalls, lng)],
        [t('overview.verified30'), pct(m.verifiedLast30DaysPct)],
        [t('overview.withPhoto'), pct(m.withPhotoPct)],
      ],
    },
    {
      title: t('overview.moderation'),
      rows: [
        [t('overview.pending'), formatNumber(m.pendingSubmissions, lng)],
        [
          t('overview.avgHours'),
          m.avgModerationHours == null
            ? '—'
            : t('overview.hours', { value: formatNumber(m.avgModerationHours, lng, 1) }),
        ],
        [t('overview.approvalRate'), pct(m.approvalRatePct)],
        [t('overview.activeCollaborators'), formatNumber(m.activeCollaboratorsThisMonth, lng)],
      ],
    },
    {
      title: t('overview.money'),
      rows: [
        [t('overview.credited'), formatMoney(m.creditedThisMonth, lng) ?? '—'],
        [t('overview.paid'), formatMoney(m.paidThisMonth, lng) ?? '—'],
        [
          t('overview.budget'),
          m.budget.hasBudget ? (formatMoney(m.budget.budget, lng) ?? '—') : t('overview.noBudget'),
        ],
        [t('overview.available'), formatMoney(m.budget.available, lng) ?? '—'],
      ],
    },
    {
      title: t('overview.search'),
      rows: [
        [t('overview.searches7'), formatNumber(m.searchesLast7Days, lng)],
        [t('overview.hitRate'), pct(m.searchHitRatePct)],
        ...Object.entries(m.searchesByLocale).map(
          ([locale, n]) => [t('overview.byLocale', { locale }), formatNumber(n, lng)] as [string, string],
        ),
      ],
    },
  ];

  return (
    <div className="grid gap-8">
      <AdminHeader
        title={t('overview.title')}
        lead={t('overview.generatedAt', { at: formatDateTime(m.generatedAt, lng) })}
      />
      {m.pendingSubmissions > 0 && (
        <Link
          to="/admin/moderation"
          className="flex min-h-12 items-center justify-between gap-3 rounded-[var(--radius-panel)] bg-band px-4 py-3 text-on-band no-underline"
        >
          <span className="font-semibold">{t('overview.queueCta', { count: m.pendingSubmissions })}</span>
          <span className="display tabular text-2xl text-highlight">{m.pendingSubmissions}</span>
        </Link>
      )}
      <div className="grid gap-8 md:grid-cols-2">
        {groups.map((g) => (
          <section key={g.title} className="grid content-start">
            <SectionTitle>{g.title}</SectionTitle>
            <dl>
              {g.rows.map(([label, value]) => (
                <div key={label} className="price-row py-2">
                  <dt>{label}</dt>
                  <span className="price-row__leader" aria-hidden="true" />
                  <dd className="display tabular text-lg">{value}</dd>
                </div>
              ))}
            </dl>
          </section>
        ))}
      </div>
      <section className="grid gap-2">
        <SectionTitle>{t('overview.tasksByStatus')}</SectionTitle>
        <dl className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {Object.entries(m.tasksByStatus).map(([status, n]) => (
            <div key={status} className="rounded-[var(--radius-control)] bg-soft p-3">
              <dt className="text-sm text-support">{t(`collab:status.${status}`)}</dt>
              <dd className="display tabular text-2xl text-ink">{formatNumber(n ?? 0, lng)}</dd>
            </div>
          ))}
        </dl>
      </section>
      <section className="grid gap-2">
        <SectionTitle aside={t('overview.zeroHint')}>{t('overview.zeroTerms')}</SectionTitle>
        {m.topZeroResultTerms.length === 0 ? (
          <p className="text-support">{t('overview.zeroNone')}</p>
        ) : (
          <ol className="divide-y divide-line">
            {m.topZeroResultTerms.map((row) => (
              <li key={row.term} className="flex justify-between gap-3 py-2">
                <span className="font-semibold">{row.term}</span>
                <span className="tabular text-support">{formatNumber(row.count, lng)}</span>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}
