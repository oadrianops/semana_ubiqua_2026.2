import { startTransition, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router';
import type { SearchItem } from '../../../shared/api/types';
import { searchSnapshots } from '../../../shared/offline/local-search';
import { useNetworkState } from '../../../shared/offline/network';
import { installSignals } from '../../../shared/pwa/install-signals';
import { EmptyState, ErrorState, LoadingRows, OfflineState } from '../../../shared/ui/States';
import { useAllSnapshots, useCategories, useMarkets, useSearch } from '../../public-api';
import { MarketBoard, SavedDataNotice } from '../../shared/board';
import { SearchBox } from '../../shared/SearchBox';

/**
 * Resultados da busca (RF-02): produto → feira → boxes, com filtros de feira, categoria e "somente disponíveis".
 * Sem rede (ou se a rede falhar), a busca roda nas feiras salvas, com a data dos dados (RF-36).
 */
export function SearchPage() {
  const { t } = useTranslation('public');
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const market = params.get('market');
  const category = params.get('category');
  const availableOnly = params.get('available') === '1';
  const network = useNetworkState();
  const remote = useSearch({ q, market, category, availableOnly });
  const remoteError = remote.error;
  const retryRemote = () => void remote.refetch();
  const snapshots = useAllSnapshots();
  const markets = useMarkets();
  const categories = useCategories();
  const [local, setLocal] = useState<{ items: SearchItem[]; oldestSavedAt: string | null }>({
    items: [],
    oldestSavedAt: null,
  });

  const useLocal = network === 'offline' || (remote.isError && !remote.data);

  useEffect(() => {
    if (!useLocal) return;
    // Filtragem em transição: não trava a digitação (INP — 9.14).
    startTransition(() =>
      setLocal(searchSnapshots(q, snapshots, { availableOnly, marketSlug: market, categorySlug: category })),
    );
  }, [useLocal, q, snapshots, availableOnly, market, category]);

  const items = useLocal ? local.items : (remote.data?.items ?? []);

  useEffect(() => {
    if (items.length > 0) void installSignals.searchWithResult();
  }, [items.length]);

  function setFilter(key: string, value: string | null) {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  }

  const title = useMemo(() => (q ? t('search.resultsFor', { q }) : t('search.title')), [q, t]);

  return (
    <>
      <section className="on-band bg-band pb-5 text-on-band">
        <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-3 pt-4">
          <h1 className="text-2xl text-on-band">{title}</h1>
          <div className="max-w-2xl">
            <SearchBox key={q} initial={q} size="md" focusOnMount={!q} />
          </div>
        </div>
      </section>

      <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-6 py-6 lg:grid-cols-[16rem_1fr]">
        <aside aria-label={t('search.filters')} className="grid content-start gap-3 lg:sticky lg:top-4">
          <label className="grid gap-1">
            <span className="text-sm font-semibold text-support">{t('search.market')}</span>
            <select
              value={market ?? ''}
              onChange={(e) => setFilter('market', e.target.value || null)}
              className="min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3"
            >
              <option value="">{t('search.allMarkets')}</option>
              {markets.data?.map((m) => (
                <option key={m.slug} value={m.slug}>
                  {m.name}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1">
            <span className="text-sm font-semibold text-support">{t('search.category')}</span>
            <select
              value={category ?? ''}
              onChange={(e) => setFilter('category', e.target.value || null)}
              className="min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3"
            >
              <option value="">{t('search.allCategories')}</option>
              {categories.data?.map((c) => (
                <option key={c.slug} value={c.slug}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label className="flex min-h-11 items-center gap-3">
            <input
              type="checkbox"
              checked={availableOnly}
              onChange={(e) => setFilter('available', e.target.checked ? '1' : null)}
              className="size-5 accent-[var(--acai-700)]"
            />
            <span>{t('search.availableOnly')}</span>
          </label>
        </aside>

        <div className="grid content-start gap-8" aria-live="polite" aria-busy={remote.isFetching}>
          {useLocal && local.oldestSavedAt && (
            <div className="rounded-[var(--radius-control)] bg-soft p-3">
              <p className="font-semibold text-ink">{t('search.fromSaved')}</p>
              <SavedDataNotice savedAt={local.oldestSavedAt} />
            </div>
          )}
          {!q && <EmptyState title={t('search.emptyQueryTitle')} body={t('search.emptyQueryBody')} />}
          {q && !useLocal && remote.isPending && <LoadingRows rows={5} />}
          {q && useLocal && snapshots.length === 0 && <OfflineState body={t('search.offlineNoSaved')} />}
          {q && !useLocal && remote.isError && !remote.data && (
            <ErrorState error={remoteError} onRetry={retryRemote} />
          )}
          {q && items.length === 0 && (remote.isSuccess || (useLocal && snapshots.length > 0)) && (
            <EmptyState
              title={t('search.noResultsTitle', { q })}
              body={
                <div className="grid gap-3">
                  {remote.data?.didYouMean && remote.data.didYouMean.length > 0 && (
                    <p>
                      {t('search.didYouMean')}{' '}
                      {remote.data.didYouMean.map((term, i) => (
                        <span key={term}>
                          {i > 0 && ', '}
                          <Link to={`/search?q=${encodeURIComponent(term)}`}>{term}</Link>
                        </span>
                      ))}
                    </p>
                  )}
                  <p>{t('search.noResultsBody')}</p>
                  <ul className="flex flex-wrap gap-2">
                    {categories.data?.slice(0, 5).map((c) => (
                      <li key={c.slug}>
                        <Link
                          to={`/categories/${c.slug}`}
                          className="inline-flex min-h-11 items-center rounded-full border border-line-strong px-4 no-underline"
                        >
                          {c.name}
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              }
            />
          )}
          {items.map((item) => (
            <article key={item.product.slug} className="grid gap-4">
              <header className="flex flex-wrap items-baseline justify-between gap-x-3">
                <h2 className="text-2xl">
                  <Link
                    to={`/products/${item.product.slug}`}
                    className="text-ink no-underline hover:underline"
                  >
                    {item.product.name}
                  </Link>
                </h2>
                <Link to={`/categories/${item.product.category.slug}`} className="text-sm">
                  {item.product.category.name}
                </Link>
              </header>
              {item.markets.map((m) => (
                <MarketBoard key={m.market.slug} hits={m} />
              ))}
            </article>
          ))}
          {items.length > 0 && <p className="text-sm text-support">{t('search.priceNote')}</p>}
        </div>
      </div>
    </>
  );
}
