import { useDocumentTitle } from '../../../shared/lib/useDocumentTitle';
import { Check, Download, Map as MapIcon, Navigation } from 'lucide-react';
import { lazy, Suspense, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams, useSearchParams } from 'react-router';
import type { StallSummary } from '../../../shared/api/types';
import { directionsUrl } from '../../../shared/config';
import { cn } from '../../../shared/lib/cn';
import { useNetworkState } from '../../../shared/offline/network';
import {
  saveMarket,
  savedMarkets,
  subscribeSnapshots,
  unsaveMarket,
} from '../../../shared/offline/snapshots';
import { Button } from '../../../shared/ui/Button';
import { ErrorState, LoadingRows, OfflineState } from '../../../shared/ui/States';
import { useLang, useMarket, useMarketStalls, useStoredSnapshot } from '../../public-api';
import { DemoTag, SavedDataNotice, Verified } from '../../shared/board';
import { prefersLightImages } from '../../../shared/offline/network';
import { OpeningHoursList } from '../OpeningHoursList';

const MapView = lazy(() => import('../../../shared/map/MapView'));

/**
 * Página da feira (RF-01): descrição, horários, "Como chegar", setores e boxes publicados. Ao abrir, o snapshot
 * da feira fica salvo no aparelho; "Salvar para usar sem internet" mantém a feira atualizada em segundo plano
 * (RF-36). Sem rede, tudo vem do snapshot, com a data.
 */
export function MarketPage() {
  const { slug = '' } = useParams();
  const { t } = useTranslation('public');
  const lng = useLang();
  const [params, setParams] = useSearchParams();
  const sector = params.get('sector');
  const [page, setPage] = useState(0);
  const network = useNetworkState();
  const market = useMarket(slug);
  const stalls = useMarketStalls(slug, sector, page);
  const stored = useStoredSnapshot(slug);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [showMap, setShowMap] = useState(false);
  const [accumulated, setAccumulated] = useState<StallSummary[]>([]);

  useEffect(() => {
    const check = () => void savedMarkets().then((list) => setSaved(list.includes(slug)));
    check();
    return subscribeSnapshots(check);
  }, [slug]);

  useEffect(() => {
    setPage(0);
  }, [sector, slug]);

  // "Carregar mais": a página 0 substitui a lista; as seguintes se somam a ela.
  useEffect(() => {
    if (!stalls.data) return;
    const incoming = stalls.data.items;
    setAccumulated((prev) =>
      stalls.data.page === 0
        ? incoming
        : [...prev, ...incoming.filter((s) => !prev.some((p) => p.publicId === s.publicId))],
    );
  }, [stalls.data]);

  const detail = market.data ?? stored?.data.market;
  useDocumentTitle(detail?.name);
  const fromSnapshot = !market.data && Boolean(stored);

  // Sem rede: boxes do snapshot, filtrados pelo setor.
  const offlineStalls = useMemo<StallSummary[] | null>(() => {
    if (stalls.data || !stored) return null;
    return stored.data.stalls
      .filter((s) => !sector || s.sectorSlug === sector)
      .map((s) => ({
        publicId: s.publicId,
        code: s.code,
        name: s.name,
        sector: s.sectorSlug ? { slug: s.sectorSlug, name: s.sectorName ?? s.sectorSlug } : null,
        photoThumbUrl: s.photoThumbUrl,
        lastVerifiedAt: s.lastVerifiedAt,
        outdated: s.outdated,
        offeringCount: s.offerings.length,
        demo: s.demo ?? false,
      }));
  }, [stalls.data, stored, sector]);

  const list = offlineStalls ?? accumulated;
  const totalPages = stalls.data?.totalPages ?? 1;

  async function toggleSave() {
    setSaving(true);
    try {
      if (saved) await unsaveMarket(slug);
      else await saveMarket(slug, lng);
    } finally {
      setSaving(false);
    }
  }

  if (!detail) {
    if (market.isPending) {
      return (
        <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
          <LoadingRows rows={6} />
        </div>
      );
    }
    return (
      <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
        {network === 'offline' ? (
          <OfflineState body={t('market.offlineNotSaved')} />
        ) : (
          <ErrorState error={market.error} onRetry={() => void market.refetch()} />
        )}
      </div>
    );
  }

  function setSector(value: string | null) {
    const next = new URLSearchParams(params);
    if (value) next.set('sector', value);
    else next.delete('sector');
    setParams(next, { replace: true });
  }

  return (
    <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-8 py-6 lg:grid-cols-[1fr_20rem]">
      <div className="grid content-start gap-6">
        <header className="grid gap-2">
          <p className="text-sm">
            <Link to="/markets" className="inline-flex min-h-11 items-center">
              {t('markets.title')}
            </Link>
          </p>
          <h1 className="text-4xl leading-none">{detail.name}</h1>
          {detail.description && (
            <p className="max-w-[var(--measure)] text-lg text-support">{detail.description}</p>
          )}
          {fromSnapshot && stored && <SavedDataNotice savedAt={stored.savedAt} />}
        </header>

        {detail.coverUrl && <MarketCover url={detail.coverUrl} name={detail.name} />}

        <section aria-labelledby="sectors" className="grid gap-2">
          <h2 id="sectors" className="sr-only">
            {t('market.sectors')}
          </h2>
          <div
            className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1"
            style={{ overscrollBehaviorX: 'contain' }}
          >
            <SectorChip
              active={!sector}
              onClick={() => setSector(null)}
              label={t('market.allSectors')}
              count={detail.publishedStalls}
            />
            {detail.sectors.map((s) => (
              <SectorChip
                key={s.slug}
                active={sector === s.slug}
                onClick={() => setSector(s.slug)}
                label={s.name}
                count={s.stallCount}
              />
            ))}
          </div>
        </section>

        <section aria-labelledby="stalls-title" className="grid gap-1">
          <h2
            id="stalls-title"
            className="flex items-baseline justify-between border-b-2 border-ink pb-1 text-xl"
          >
            {t('market.stallsTitle')}
          </h2>
          {stalls.isPending && !offlineStalls && <LoadingRows rows={6} />}
          {stalls.isError && !offlineStalls && (
            <ErrorState error={stalls.error} onRetry={() => void stalls.refetch()} />
          )}
          <ul className="divide-y divide-line">
            {list.map((s) => (
              <li key={s.publicId}>
                <Link
                  to={`/stalls/${s.publicId}`}
                  className="grid grid-cols-[4.5rem_1fr] items-baseline gap-x-3 py-3 text-ink no-underline hover:bg-soft"
                >
                  <span className="display tabular text-2xl text-brand">{s.code}</span>
                  <span className="grid gap-0.5">
                    <span className="font-semibold">
                      {s.name ?? t('stall.fallbackName', { code: s.code })}
                    </span>
                    <span className="flex flex-wrap items-center gap-x-3 text-sm text-support">
                      {s.demo && <DemoTag />}
                      {s.sector && <span>{s.sector.name}</span>}
                      <span>{t('market.productsCount', { count: s.offeringCount })}</span>
                      <Verified
                        at={s.lastVerifiedAt}
                        className={cn(s.outdated && 'font-semibold text-warning')}
                      />
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          {!offlineStalls && page + 1 < totalPages && (
            <Button
              variant="secondary"
              className="mt-3 justify-self-start"
              busy={stalls.isFetching}
              onClick={() => setPage((p) => p + 1)}
            >
              {t('market.loadMore')}
            </Button>
          )}
        </section>
      </div>

      <aside className="grid content-start gap-4 lg:sticky lg:top-4">
        <div className="grid gap-3 rounded-[var(--radius-panel)] bg-soft p-4">
          <p className="text-sm text-support">{detail.address}</p>
          <OpeningHoursList hours={detail.openingHours} />
          <a
            href={directionsUrl(detail.latitude, detail.longitude)}
            target="_blank"
            rel="noopener"
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-[var(--radius-control)] bg-brand px-4 font-semibold text-on-brand no-underline"
          >
            <Navigation size={18} aria-hidden="true" />
            {t('markets.directions')}
          </a>
          {detail.howToGetThere && <p className="text-sm text-support">{detail.howToGetThere}</p>}
          <Button
            variant="secondary"
            icon={saved ? <Check size={18} aria-hidden="true" /> : <Download size={18} aria-hidden="true" />}
            busy={saving}
            onClick={() => void toggleSave()}
            aria-pressed={saved}
          >
            {saved ? t('market.savedOffline') : t('market.saveOffline')}
          </Button>
          <p className="text-sm text-support">{saved ? t('market.savedHint') : t('market.saveHint')}</p>
          {stored && !fromSnapshot && <SavedDataNotice savedAt={stored.savedAt} />}
        </div>
        <Button
          variant="ghost"
          icon={<MapIcon size={18} aria-hidden="true" />}
          onClick={() => setShowMap((v) => !v)}
          aria-expanded={showMap}
        >
          {showMap ? t('market.hideMap') : t('market.showMap')}
        </Button>
        {showMap && (
          <Suspense fallback={<div className="skeleton h-64 w-full" />}>
            <MapView
              label={t('market.mapLabel', { name: detail.name })}
              height={260}
              zoom={16}
              points={[
                {
                  id: detail.slug,
                  lat: Number(detail.latitude),
                  lng: Number(detail.longitude),
                  title: detail.name,
                  radius: detail.geofenceRadiusMeters,
                },
              ]}
            />
          </Suspense>
        )}
      </aside>
    </div>
  );
}

function SectorChip({
  active,
  onClick,
  label,
  count,
}: {
  active: boolean;
  onClick: () => void;
  label: string;
  count: number;
}) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={cn(
        'inline-flex min-h-11 shrink-0 items-center gap-2 rounded-full border px-4 font-semibold whitespace-nowrap',
        active
          ? 'border-brand bg-brand text-on-brand'
          : 'border-line-strong bg-surface text-ink hover:bg-soft',
      )}
    >
      {label}
      <span className={cn('tabular text-sm', active ? 'text-on-brand' : 'text-support')}>{count}</span>
    </button>
  );
}

/** Foto real da feira (14.6): miniatura com rede ruim ou economia de dados; nunca atrasa a lista de boxes. */
function MarketCover({ url, name }: { url: string; name: string }) {
  const { t } = useTranslation('public');
  const thumb = `${url}?variant=thumb`;
  const light = prefersLightImages();
  return (
    <img
      src={light ? thumb : url}
      srcSet={light ? undefined : `${thumb} 400w, ${url} 1600w`}
      sizes="(min-width: 1024px) 720px, 100vw"
      width={1600}
      height={900}
      loading="lazy"
      decoding="async"
      alt={t('market.coverAlt', { name })}
      className="aspect-[16/9] w-full rounded-[var(--radius-panel)] bg-soft object-cover"
    />
  );
}
