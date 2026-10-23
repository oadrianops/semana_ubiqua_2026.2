import { Maximize2 } from 'lucide-react';
import { lazy, Suspense, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../shared/lib/cn';
import { prefersLightImages } from '../../shared/offline/network';

const ImageViewer = lazy(() => import('../../shared/ui/ImageViewer'));

export interface StallPhoto {
  url: string | null;
  thumbUrl: string;
}

/**
 * Fotos do box (RF-03): uma grande e, havendo mais, miniaturas para trocar. Com economia de dados ou rede lenta
 * só a miniatura de 400 px é baixada (9.14); as miniaturas fora da tela esperam a rolagem. Tocar na foto grande
 * abre a foto ampliada (a versão cheia só é baixada nesse pedido explícito).
 */
export function StallPhotos({
  photos,
  code,
  market,
}: {
  photos: StallPhoto[];
  code: string;
  market: string;
}) {
  const { t } = useTranslation('public');
  const { t: tc } = useTranslation();
  const [selected, setSelected] = useState(0);
  const [viewer, setViewer] = useState<number | null>(null);
  const light = prefersLightImages();
  const current = photos[Math.min(selected, photos.length - 1)];
  if (!current) return null;
  const total = photos.length;
  const full = light ? null : current.url;
  const altOf = (i: number) =>
    total > 1
      ? t('stall.galleryAlt', { n: i + 1, total, code, market })
      : t('stall.photoAlt', { code, market });

  return (
    <section aria-label={t('stall.photosTitle')} className="grid gap-2">
      <button
        type="button"
        onClick={() => setViewer(selected)}
        aria-label={tc('viewer.openNamed', { alt: altOf(selected) })}
        className="group relative block w-full cursor-zoom-in overflow-hidden rounded-[var(--radius-panel)]"
      >
        <img
          key={current.thumbUrl}
          src={full ?? current.thumbUrl}
          srcSet={full ? `${current.thumbUrl} 400w, ${full} 1600w` : undefined}
          sizes="(min-width: 768px) 352px, 100vw"
          width={1600}
          height={1200}
          loading={selected === 0 ? 'eager' : 'lazy'}
          decoding="async"
          alt={altOf(selected)}
          className="aspect-[4/3] w-full bg-soft object-cover"
        />
        <span
          aria-hidden="true"
          className="absolute right-2 bottom-2 inline-flex min-h-9 items-center gap-1.5 rounded-[var(--radius-control)] bg-band/85 px-2.5 text-sm font-semibold text-on-band group-hover:bg-band"
        >
          <Maximize2 size={16} />
          {tc('viewer.open')}
        </span>
      </button>
      {total > 1 && (
        <ul className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1" style={{ overscrollBehaviorX: 'contain' }}>
          {photos.map((p, i) => (
            <li key={p.thumbUrl} className="shrink-0">
              <button
                type="button"
                onClick={() => setSelected(i)}
                aria-pressed={i === selected}
                aria-label={t('stall.photoShow', { n: i + 1, total })}
                className={cn(
                  'block size-16 overflow-hidden rounded-[var(--radius-control)] border-2',
                  i === selected ? 'border-brand' : 'border-transparent',
                )}
              >
                <img
                  src={p.thumbUrl}
                  width={64}
                  height={64}
                  loading="lazy"
                  decoding="async"
                  alt=""
                  className="size-full object-cover"
                />
              </button>
            </li>
          ))}
        </ul>
      )}
      {viewer !== null && (
        <Suspense fallback={null}>
          <ImageViewer
            photos={photos.map((p, i) => ({ url: p.url ?? p.thumbUrl, thumbUrl: p.thumbUrl, alt: altOf(i) }))}
            index={viewer}
            onIndexChange={(i) => {
              setViewer(i);
              setSelected(i);
            }}
            onClose={() => setViewer(null)}
          />
        </Suspense>
      )}
    </section>
  );
}
