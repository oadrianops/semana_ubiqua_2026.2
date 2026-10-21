import { useTranslation } from 'react-i18next';
import type { OpeningHours } from '../../shared/api/types';

/** Horários de funcionamento por dia da semana, traduzidos. */
export function OpeningHoursList({ hours }: { hours: OpeningHours[] | null | undefined }) {
  const { t } = useTranslation('public');
  if (!hours?.length) return null;
  return (
    <dl className="grid gap-0.5 text-sm">
      {hours.map((h) => (
        <div key={h.days.join()} className="flex gap-2">
          <dt className="font-semibold text-ink">{h.days.map((d) => t(`weekday.${d}`)).join(', ')}</dt>
          <dd className="tabular text-support">
            {h.opens}–{h.closes}
          </dd>
        </div>
      ))}
    </dl>
  );
}
