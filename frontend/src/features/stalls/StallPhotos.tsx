import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../../shared/lib/cn';
import { prefersLightImages } from '../../shared/offline/network';

export interface StallPhoto {
  url: string | null;
  thumbUrl: string;
}

/**
 * Fotos do box (RF-03): uma grande e, havendo mais, miniaturas para trocar. Com economia de dados ou rede lenta
 * só a miniatura de 400 px é baixada (9.14); as miniaturas fora da tela esperam a rolagem.
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
  const [selected, setSelected] = useState(0);
  const light = prefersLightImages();
  const current = photos[Math.min(selected, photos.length - 1)];
  if (!current) return null;
  const total = photos.length;
  const full = light ? null : current.url;

  return (
    <section aria-label={t('stall.photosTitle')} className="grid gap-2">
      <img
        key={current.thumbUrl}
        src={full ?? current.thumbUrl}
        srcSet={full ? `${current.thumbUrl} 400w, ${full} 1600w` : undefined}
        sizes="(min-width: 768px) 640px, 100vw"
        width={1600}
        height={1200}
        loading={selected === 0 ? 'eager' : 'lazy'}
        decoding="async"
        alt={
          total > 1
            ? t('stall.galleryAlt', { n: selected + 1, total, code, market })
            : t('stall.photoAlt', { code, market })
        }
        className="aspect-[4/3] w-full rounded-[var(--radius-panel)] bg-soft object-cover"
      />
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
    </section>
  );
}
