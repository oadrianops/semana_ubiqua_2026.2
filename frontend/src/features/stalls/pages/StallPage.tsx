import { useFreshRows } from '../../../shared/offline/seen';
import { useDocumentTitle } from '../../../shared/lib/useDocumentTitle';
import { AtSign, ExternalLink, Flag, Navigation } from 'lucide-react';
import { directionsUrl } from '../../../shared/config';
import { lazy, Suspense, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import type { OfferingView, StallDetail } from '../../../shared/api/types';
import { useNetworkState } from '../../../shared/offline/network';
import { Button } from '../../../shared/ui/Button';
import { PriceRow } from '../../../shared/ui/PriceRow';
import { ErrorState, LoadingRows, OfflineState } from '../../../shared/ui/States';
import { useAllSnapshots, useStall } from '../../public-api';
import {
  AvailabilityNote,
  DemoTag,
  OutdatedWarning,
  PriceValue,
  SavedDataNotice,
  Verified,
} from '../../shared/board';
import { StallPhotos } from '../StallPhotos';

// A folha de aviso (Radix Dialog) só carrega quando a pessoa abre (orçamento da primeira tela — 9.14).
const ReportSheet = lazy(() => import('../ReportSheet').then((m) => ({ default: m.ReportSheet })));

/**
 * Página do box (RF-03): a lousa do box com preço de referência e unidade (RN-15), disponibilidade e data da
 * verificação; aviso acima de 60 dias (RN-12); nome do feirante só com consentimento (RN-14).
 */
export function StallPage() {
  const { publicId = '' } = useParams();
  const { t } = useTranslation('public');
  const stall = useStall(publicId);
  const snapshots = useAllSnapshots();
  const network = useNetworkState();
  const [reporting, setReporting] = useState(false);

  // Sem rede: monta o box a partir do snapshot salvo da feira.
  const offline = (() => {
    if (stall.data) return null;
    for (const s of snapshots) {
      const found = s.data.stalls.find((x) => x.publicId === publicId);
      if (found) {
        const products = new Map(s.data.products.map((p) => [p.slug, p]));
        const detail: StallDetail = {
          publicId: found.publicId,
          code: found.code,
          name: found.name,
          vendorName: found.vendorName,
          market: { slug: s.data.market.slug, name: s.data.market.name },
          sector: found.sectorSlug ? { slug: found.sectorSlug, name: found.sectorName ?? '' } : null,
          photoUrl: null,
          photoThumbUrl: found.photoThumbUrl,
          lastVerifiedAt: found.lastVerifiedAt,
          outdated: found.outdated,
          version: 0,
          offerings: found.offerings.map((o) => ({
            publicId: `${found.publicId}-${o.productSlug}`,
            productSlug: o.productSlug,
            productName: products.get(o.productSlug)?.name ?? o.productSlug,
            categorySlug: products.get(o.productSlug)?.categorySlug ?? '',
            price: o.price,
            priceUnit: o.priceUnit,
            availability: o.availability,
            lastVerifiedAt: o.lastVerifiedAt,
            version: 0,
          })),
          demo: found.demo ?? false,
          instagram: null,
          gallery: [],
          latitude: null,
          longitude: null,
        };
        return { detail, savedAt: s.savedAt };
      }
    }
    return null;
  })();

  const data = stall.data ?? offline?.detail;
  // Hooks antes do retorno antecipado (regras do React).
  const fresh = useFreshRows(
    data ? `stall:${data.publicId}` : null,
    data?.offerings.map((o) => ({ id: o.publicId, version: 'version' in o ? (o.version as number) : 0 })),
  );
  useDocumentTitle(data ? t('stall.code', { code: data.code }) : null);

  if (!data) {
    return (
      <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
        {stall.isPending && network !== 'offline' ? (
          <LoadingRows rows={6} />
        ) : network === 'offline' ? (
          <OfflineState body={t('stall.offlineNotSaved')} />
        ) : (
          <ErrorState error={stall.error} onRetry={() => void stall.refetch()} />
        )}
      </div>
    );
  }

  const sorted = [...data.offerings].sort(sortOfferings);
  const photos = [
    ...(data.photoThumbUrl ? [{ url: data.photoUrl, thumbUrl: data.photoThumbUrl }] : []),
    ...data.gallery.map((p) => ({ url: p.url, thumbUrl: p.thumbUrl })),
  ];

  return (
    // Celular: título, fotos, lousa e aviso, nessa ordem. Tela larga: a lousa à esquerda; fotos e aviso ao lado.
    <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-6 py-6 md:grid-cols-[minmax(0,1fr)_minmax(0,22rem)] md:items-start md:gap-x-10">
      <header className="grid gap-2 md:col-start-1 md:row-start-1">
        <p className="text-sm">
          <Link to={`/markets/${data.market.slug}`} className="inline-flex min-h-11 items-center">
            {data.market.name}
          </Link>
          {data.sector && <span className="text-support"> · {data.sector.name}</span>}
        </p>
        <h1 className="flex flex-wrap items-baseline gap-x-3 text-3xl">
          <span className="display tabular text-brand">{t('stall.code', { code: data.code })}</span>
          {data.name && <span>{data.name}</span>}
        </h1>
        {data.vendorName && <p className="text-support">{t('stall.vendor', { name: data.vendorName })}</p>}
        {data.instagram && (
          <p>
            <a
              href={`https://www.instagram.com/${data.instagram}/`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex min-h-11 items-center gap-1.5 font-semibold"
            >
              <AtSign size={18} aria-hidden="true" />
              {t('stall.instagram', { handle: data.instagram })}
              <ExternalLink size={14} aria-hidden="true" />
              <span className="sr-only">{t('stall.newTab')}</span>
            </a>
          </p>
        )}
        {data.demo && (
          <p className="flex flex-wrap items-center gap-2 text-sm text-support">
            <DemoTag />
            <span>{t('stall.demoNote')}</span>
          </p>
        )}
        <p className="text-sm text-support">
          {t('stall.verifiedLabel')} <Verified at={data.lastVerifiedAt} className="font-semibold text-ink" />
        </p>
        {data.latitude !== null && data.longitude !== null && (
          <p>
            <a
              href={directionsUrl(data.latitude, data.longitude)}
              target="_blank"
              rel="noopener"
              className="inline-flex min-h-11 items-center gap-2 rounded-[var(--radius-control)] bg-brand px-4 font-semibold text-on-brand no-underline"
            >
              <Navigation size={18} aria-hidden="true" />
              {t('stall.directions')}
              <span className="sr-only">{t('stall.newTab')}</span>
            </a>
          </p>
        )}
        {offline && <SavedDataNotice savedAt={offline.savedAt} />}
        {data.outdated && <OutdatedWarning />}
      </header>

      {photos.length > 0 && (
        <div className="md:col-start-2 md:row-span-2 md:row-start-1">
          <StallPhotos photos={photos} code={data.code} market={data.market.name} />
        </div>
      )}

      <section aria-labelledby="board-title" className="grid md:col-start-1 md:row-span-2 md:row-start-2">
        <h2
          id="board-title"
          className="flex items-baseline justify-between border-b-2 border-ink pb-1 text-xl"
        >
          {t('stall.boardTitle')}
          <span className="text-sm font-normal text-support">
            <span className="sr-only">, </span>
            {t('stall.productsCount', { count: data.offerings.length })}
          </span>
        </h2>
        <ul className="divide-y divide-line">
          {sorted.map((o) => (
            <li key={o.publicId}>
              <PriceRow
                fresh={fresh.has(o.publicId)}
                label={
                  <Link to={`/products/${o.productSlug}`} className="text-ink no-underline hover:underline">
                    {o.productName}
                  </Link>
                }
                value={<PriceValue price={o.price} unit={o.priceUnit} availability={o.availability} />}
                muted={o.availability === 'OUT_OF_STOCK'}
                sub={
                  <span className="flex flex-wrap gap-x-3">
                    <AvailabilityNote availability={o.availability} />
                    <Verified at={o.lastVerifiedAt} />
                  </span>
                }
              />
            </li>
          ))}
        </ul>
        <p className="pt-3 text-sm text-support">{t('stall.priceNote')}</p>
      </section>

      <aside className="grid content-start gap-3 md:col-start-2 md:row-start-3">
        <div className="grid gap-2 rounded-[var(--radius-panel)] bg-soft p-4">
          <p className="font-semibold text-ink">{t('stall.wrongTitle')}</p>
          <p className="text-sm text-support">{t('stall.wrongBody')}</p>
          <Button
            variant="secondary"
            icon={<Flag size={18} aria-hidden="true" />}
            onClick={() => setReporting(true)}
            disabled={network === 'offline'}
          >
            {t('stall.wrongAction')}
          </Button>
          {network === 'offline' && <p className="text-sm text-support">{t('stall.reportOffline')}</p>}
        </div>
      </aside>

      {reporting && (
        <Suspense fallback={null}>
          <ReportSheet open={reporting} onOpenChange={setReporting} stall={data} />
        </Suspense>
      )}
    </div>
  );
}

const AVAILABILITY_ORDER = { AVAILABLE: 0, SEASONAL: 1, OUT_OF_STOCK: 2 } as const;

function sortOfferings(a: OfferingView, b: OfferingView) {
  return (
    AVAILABILITY_ORDER[a.availability] - AVAILABILITY_ORDER[b.availability] ||
    a.productName.localeCompare(b.productName)
  );
}
