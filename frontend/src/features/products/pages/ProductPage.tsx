import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { formatNumber } from '../../../shared/lib/format';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useProduct } from '../../public-api';
import { MarketBoard } from '../../shared/board';

/** Página do produto (RF-04): o que é, como se consome, safra e onde encontrar (feira → setor → box). */
export default function ProductPage() {
  const { slug = '' } = useParams();
  const { t, i18n } = useTranslation('public');
  const product = useProduct(slug);

  if (!product.data) {
    return (
      <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
        {product.isPending ? (
          <LoadingRows rows={5} />
        ) : (
          <ErrorState error={product.error} onRetry={() => void product.refetch()} />
        )}
      </div>
    );
  }
  const p = product.data;
  const months = p.seasonMonths?.map((m) =>
    new Intl.DateTimeFormat(i18n.language, { month: 'long', timeZone: 'UTC' }).format(
      new Date(Date.UTC(2026, m - 1, 15)),
    ),
  );

  return (
    <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-8 py-6 md:grid-cols-[1fr_1.2fr] md:gap-12">
      <header className="grid content-start gap-3">
        <p className="text-sm">
          <Link to={`/categories/${p.product.category.slug}`}>{p.product.category.name}</Link>
        </p>
        <h1 className="text-[2.5rem] leading-none">{p.product.name}</h1>
        {p.description && <p className="max-w-[var(--measure)] text-lg text-ink">{p.description}</p>}
        <dl className="grid gap-1 text-sm">
          <div className="flex gap-2">
            <dt className="font-semibold">{t('product.unit')}</dt>
            <dd className="text-support">{t(`unit.${p.defaultUnit}`)}</dd>
          </div>
          {months && months.length > 0 && (
            <div className="flex gap-2">
              <dt className="font-semibold">{t('product.season')}</dt>
              <dd className="text-support">{months.join(', ')}</dd>
            </div>
          )}
        </dl>
        <p className="text-sm text-support">{t('product.culturalNote')}</p>
      </header>

      <section aria-labelledby="where" className="grid content-start gap-6">
        <h2 id="where" className="text-2xl">
          {t('product.whereTitle')}
          {p.markets.length > 0 && (
            <span className="ml-2 text-base font-normal text-support">
              {t('product.stallsTotal', {
                count: p.markets.reduce((n, m) => n + m.stalls.length, 0),
                formatted: formatNumber(p.markets.length, i18n.language),
              })}
            </span>
          )}
        </h2>
        {p.markets.length === 0 && <EmptyState title={t('product.noneTitle')} body={t('product.noneBody')} />}
        {p.markets.map((m) => (
          <MarketBoard key={m.market.slug} hits={m} />
        ))}
        {p.markets.length > 0 && <p className="text-sm text-support">{t('search.priceNote')}</p>}
      </section>
    </div>
  );
}
