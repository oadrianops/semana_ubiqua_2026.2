import { ArrowUpRight, ChevronRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import type { MarketSummary, OpeningHours } from '../../../shared/api/types';
import { useNetworkState } from '../../../shared/offline/network';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useAllSnapshots, useCategories, useMarkets } from '../../public-api';
import { SearchBox } from '../../shared/SearchBox';
import { CategoryIcon } from '../../shared/CategoryIcon';
import { SavedDataNotice } from '../../shared/board';

const QUICK = ['jambu', 'banhoDeCheiro', 'castanha', 'tacaca'] as const;

/** Início (14.2): busca em destaque na faixa Açaí 900, feiras e categorias em lista com régua fina. */
export function HomePage() {
  const { t } = useTranslation('public');
  const markets = useMarkets();
  const categories = useCategories();
  const network = useNetworkState();
  const saved = useAllSnapshots();

  return (
    <>
      <section className="on-band bg-band pb-8 text-on-band">
        <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-4 pt-6 md:pt-10">
          <h1 className="max-w-[18ch] text-3xl text-on-band md:text-5xl">{t('home.title')}</h1>
          <p className="max-w-[var(--measure)] text-lg text-on-band-muted">{t('home.lead')}</p>
          <div className="max-w-2xl">
            <SearchBox />
          </div>
          <div
            className="-mx-4 flex items-center gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:overflow-visible sm:px-0"
            aria-label={t('home.quickLabel')}
            role="group"
          >
            <span className="text-sm text-on-band-muted">{t('home.quickLabel')}</span>
            {QUICK.map((key) => (
              <Link
                key={key}
                to={`/search?q=${encodeURIComponent(t(`home.quick.${key}`))}`}
                className="inline-flex min-h-11 items-center rounded-full border border-on-band-muted/50 px-4 text-on-band no-underline hover:bg-band-hover"
              >
                {t(`home.quick.${key}`)}
              </Link>
            ))}
          </div>
        </div>
      </section>

      <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-10 py-8 md:grid-cols-[1.2fr_1fr] md:gap-12">
        <section aria-labelledby="home-markets" className="grid content-start gap-2">
          <div className="flex items-baseline justify-between gap-3 border-b-2 border-ink pb-1">
            <h2 id="home-markets" className="text-xl">
              {t('home.marketsTitle')}
            </h2>
            <Link to="/markets" className="inline-flex min-h-11 items-center gap-1 font-semibold">
              {t('home.seeMap')}
              <ChevronRight size={18} aria-hidden="true" />
            </Link>
          </div>
          {markets.isPending && <LoadingRows rows={2} />}
          {markets.isError && !markets.data && (
            <ErrorState error={markets.error} onRetry={() => void markets.refetch()} />
          )}
          <ul className="divide-y divide-line">
            {markets.data?.map((m) => (
              <MarketRow key={m.slug} market={m} />
            ))}
          </ul>
          {network === 'offline' && saved[0] && <SavedDataNotice savedAt={saved[0].savedAt} />}
        </section>

        <section aria-labelledby="home-categories" className="grid content-start gap-2">
          <h2 id="home-categories" className="border-b-2 border-ink pb-1 text-xl">
            {t('home.categoriesTitle')}
          </h2>
          {categories.isPending && <LoadingRows rows={5} />}
          {categories.isError && !categories.data && (
            <ErrorState error={categories.error} onRetry={() => void categories.refetch()} />
          )}
          <ul className="grid divide-y divide-line">
            {categories.data?.map((c) => (
              <li key={c.slug}>
                <Link
                  to={`/categories/${c.slug}`}
                  className="flex min-h-13 items-center gap-3 py-2 text-ink no-underline hover:bg-soft"
                >
                  <CategoryIcon name={c.icon} />
                  <span className="flex-1 font-semibold">{c.name}</span>
                  <span className="tabular text-sm text-support">
                    {t('home.productsCount', { count: c.productCount })}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <section className="border-t border-line bg-soft">
        <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-3 py-8 md:grid-cols-[1fr_auto] md:items-center">
          <div>
            <h2 className="text-xl">{t('home.collaborateTitle')}</h2>
            <p className="max-w-[var(--measure)] text-support">{t('home.collaborateBody')}</p>
          </div>
          <Link to="/about" className="inline-flex min-h-11 items-center gap-1 font-semibold">
            {t('home.collaborateLink')}
            <ArrowUpRight size={18} aria-hidden="true" />
          </Link>
        </div>
      </section>
    </>
  );
}

function todayHours(hours: OpeningHours[]): OpeningHours | undefined {
  const day = new Intl.DateTimeFormat('en-US', { weekday: 'short', timeZone: 'America/Belem' })
    .format(new Date())
    .toUpperCase();
  return hours.find((h) => h.days.includes(day));
}

function MarketRow({ market }: { market: MarketSummary }) {
  const { t } = useTranslation('public');
  const today = todayHours(market.openingHours ?? []);
  return (
    <li>
      <Link to={`/markets/${market.slug}`} className="grid gap-1 py-4 text-ink no-underline hover:bg-soft">
        <span className="flex items-baseline justify-between gap-3">
          <span className="display text-2xl">{market.name}</span>
          <span className="display tabular text-lg text-brand">
            {t('home.stallsCount', { count: market.publishedStalls })}
          </span>
        </span>
        {market.description && <span className="line-clamp-2 text-support">{market.description}</span>}
        <span className="text-sm text-support">
          {today ? t('home.todayHours', { opens: today.opens, closes: today.closes }) : t('home.closedToday')}
        </span>
      </Link>
    </li>
  );
}
