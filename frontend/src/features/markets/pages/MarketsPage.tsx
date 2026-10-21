import { List, Map as MapIcon, Navigation } from 'lucide-react';
import { lazy, Suspense, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { directionsUrl } from '../../../shared/config';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useMarkets } from '../../public-api';
import { OpeningHoursList } from '../OpeningHoursList';

const MapView = lazy(() => import('../../../shared/map/MapView'));

/** Feiras (RF-01): lista primeiro; o mapa só carrega no toque em "Ver no mapa" (9.6). */
export function MarketsPage() {
  const { t } = useTranslation('public');
  const markets = useMarkets();
  const [showMap, setShowMap] = useState(false);

  return (
    <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-6 py-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl">{t('markets.title')}</h1>
          <p className="max-w-[var(--measure)] text-support">{t('markets.lead')}</p>
        </div>
        <div
          className="inline-flex rounded-[var(--radius-control)] border border-line-strong p-1"
          role="group"
          aria-label={t('markets.viewMode')}
        >
          <button
            type="button"
            aria-pressed={!showMap}
            onClick={() => setShowMap(false)}
            className="inline-flex min-h-10 items-center gap-2 rounded-[8px] px-3 font-semibold aria-pressed:bg-brand aria-pressed:text-on-brand"
          >
            <List size={18} aria-hidden="true" />
            {t('markets.list')}
          </button>
          <button
            type="button"
            aria-pressed={showMap}
            onClick={() => setShowMap(true)}
            className="inline-flex min-h-10 items-center gap-2 rounded-[8px] px-3 font-semibold aria-pressed:bg-brand aria-pressed:text-on-brand"
          >
            <MapIcon size={18} aria-hidden="true" />
            {t('markets.map')}
          </button>
        </div>
      </header>

      {markets.isPending && <LoadingRows rows={2} />}
      {markets.isError && !markets.data && (
        <ErrorState error={markets.error} onRetry={() => void markets.refetch()} />
      )}

      {showMap && markets.data && (
        <Suspense fallback={<div className="skeleton h-80 w-full" />}>
          <MapView
            label={t('markets.mapLabel')}
            zoom={13}
            center={[-1.447, -48.488]}
            points={markets.data.map((m) => ({
              id: m.slug,
              lat: Number(m.latitude),
              lng: Number(m.longitude),
              title: m.name,
              subtitle: t('home.stallsCount', { count: m.publishedStalls }),
              href: `/markets/${m.slug}`,
            }))}
          />
        </Suspense>
      )}

      <ul className="grid gap-8 md:grid-cols-2">
        {markets.data?.map((m) => (
          <li key={m.slug} className="grid content-start gap-3 border-t-2 border-ink pt-3">
            <div className="flex items-baseline justify-between gap-3">
              <h2 className="text-2xl">
                <Link to={`/markets/${m.slug}`} className="text-ink no-underline hover:underline">
                  {m.name}
                </Link>
              </h2>
              <span className="display tabular text-lg text-brand">
                {t('home.stallsCount', { count: m.publishedStalls })}
              </span>
            </div>
            {m.description && <p className="text-support">{m.description}</p>}
            <p className="text-sm text-support">{m.address}</p>
            <OpeningHoursList hours={m.openingHours} />
            <div className="flex flex-wrap gap-2">
              <Link
                to={`/markets/${m.slug}`}
                className="inline-flex min-h-11 items-center rounded-[var(--radius-control)] bg-brand px-4 font-semibold text-on-brand no-underline"
              >
                {t('markets.seeStalls')}
              </Link>
              <a
                href={directionsUrl(m.latitude, m.longitude)}
                target="_blank"
                rel="noopener"
                className="inline-flex min-h-11 items-center gap-2 rounded-[var(--radius-control)] border border-line-strong px-4 font-semibold no-underline"
              >
                <Navigation size={18} aria-hidden="true" />
                {t('markets.directions')}
              </a>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
