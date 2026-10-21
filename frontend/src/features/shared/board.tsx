import { CalendarClock, TriangleAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import type { Availability, MarketHits, Money, PriceUnit, StallHit } from '../../shared/api/types';
import { cn } from '../../shared/lib/cn';
import { daysSince, formatDayMonth, formatMoney, formatTime } from '../../shared/lib/format';
import { isSnapshotStale } from '../../shared/offline/snapshots';
import { PriceRow } from '../../shared/ui/PriceRow';

/** "verificado há N dias" (RN-12, P3). */
export function Verified({ at, className }: { at: string | null | undefined; className?: string }) {
  const { t } = useTranslation();
  const days = daysSince(at);
  return (
    <span className={cn('tabular', className)}>
      {days === null
        ? t('freshness.never')
        : days === 0
          ? t('freshness.today')
          : t('freshness.daysAgo', { count: days })}
    </span>
  );
}

/** Aviso de possível desatualização acima de 60 dias (RN-12). */
export function OutdatedWarning() {
  const { t } = useTranslation();
  return (
    <p className="flex items-start gap-2 rounded-[var(--radius-control)] bg-soft p-3 text-sm text-warning">
      <TriangleAlert size={18} aria-hidden="true" className="mt-0.5 shrink-0" />
      <span className="font-semibold">{t('freshness.outdated')}</span>
    </p>
  );
}

/** Preço de referência com unidade (RN-15) ou o estado da oferta. */
export function PriceValue({
  price,
  unit,
  availability,
}: {
  price: Money | null;
  unit: PriceUnit | null;
  availability: Availability;
}) {
  const { t, i18n } = useTranslation();
  if (availability === 'OUT_OF_STOCK')
    return <span className="text-muted">{t('availability.OUT_OF_STOCK')}</span>;
  const money = formatMoney(price, i18n.language);
  if (!money) return <span className="text-support">{t('price.ask')}</span>;
  return (
    <span>
      {money}
      {unit && <span className="text-base font-semibold text-support"> {t(`unitShort.${unit}`)}</span>}
    </span>
  );
}

export function AvailabilityNote({ availability }: { availability: Availability }) {
  const { t } = useTranslation();
  // "Em falta" já aparece no lugar do preço; aqui só a safra.
  if (availability !== 'SEASONAL') return null;
  return (
    <span className={cn('font-semibold', availability === 'SEASONAL' ? 'text-info' : 'text-muted')}>
      {t(`availability.${availability}`)}
    </span>
  );
}

/** No celular só o número do box fica na linha do pontilhado; o nome desce para a linha de apoio. */
function StallLabel({ stall }: { stall: StallHit }) {
  const { t } = useTranslation();
  return (
    <span>
      <span className="display tabular text-brand">{t('stall.code', { code: stall.code })}</span>
      {stall.name && <span className="hidden sm:inline"> {stall.name}</span>}
    </span>
  );
}

/** Quadro de uma feira: um box por linha com preço/unidade, setor e data da verificação. */
export function MarketBoard({ hits, headingLevel = 3 }: { hits: MarketHits; headingLevel?: 2 | 3 }) {
  const { t } = useTranslation();
  const Heading = headingLevel === 2 ? 'h2' : 'h3';
  return (
    <section aria-label={hits.market.name} className="grid">
      <Heading className="flex items-baseline justify-between gap-3 border-b-2 border-ink pb-1 text-lg">
        <Link
          to={`/markets/${hits.market.slug}`}
          className="inline-flex min-h-11 items-center text-ink no-underline hover:underline"
        >
          {hits.market.name}
        </Link>
        <span className="text-sm font-normal text-support">
          <span className="sr-only">, </span>
          {t('board.stallsCount', { count: hits.stalls.length })}
        </span>
      </Heading>
      <ul className="divide-y divide-line">
        {hits.stalls.map((stall) => (
          <li key={stall.publicId}>
            <Link
              to={`/stalls/${stall.publicId}`}
              className="block text-ink no-underline hover:bg-soft focus-visible:bg-soft"
            >
              <PriceRow
                label={<StallLabel stall={stall} />}
                value={
                  <PriceValue price={stall.price} unit={stall.priceUnit} availability={stall.availability} />
                }
                muted={stall.availability === 'OUT_OF_STOCK'}
                sub={
                  <span className="flex flex-wrap gap-x-3 gap-y-0.5">
                    {stall.name && <span className="font-semibold text-ink sm:hidden">{stall.name}</span>}
                    {stall.sector && <span>{stall.sector}</span>}
                    <AvailabilityNote availability={stall.availability} />
                    <Verified at={stall.lastVerifiedAt} />
                  </span>
                }
              />
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

/** "Mostrando dados salvos em 19/10 às 15:40" (RF-35, RN-28). */
export function SavedDataNotice({ savedAt, className }: { savedAt: string; className?: string }) {
  const { t, i18n } = useTranslation();
  const stale = isSnapshotStale(savedAt);
  return (
    <p className={cn('flex items-start gap-2 text-sm', stale ? 'text-warning' : 'text-support', className)}>
      <CalendarClock size={16} aria-hidden="true" className="mt-0.5 shrink-0" />
      <span>
        {t('offline.savedAt', {
          date: formatDayMonth(savedAt, i18n.language),
          time: formatTime(savedAt, i18n.language),
        })}
        {stale && <> {t('offline.mayBeOutdated')}</>}
      </span>
    </p>
  );
}
