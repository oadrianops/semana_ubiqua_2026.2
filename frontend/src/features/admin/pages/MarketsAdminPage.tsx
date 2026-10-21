import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { lazy, Suspense, useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import { formatNumber } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, TextareaField, TextField } from '../../../shared/ui/Field';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import type { AdminMarket } from '../types';
import { AdminHeader, errorMessage, Pill, SectionTitle } from '../ui';

const MapView = lazy(() => import('../../../shared/map/MapView'));

/**
 * Feiras (RF-29): nome, endereço, posição e raio da cerca de check-in (RN-03), clicando no mapa ou digitando as
 * coordenadas (alternativa ao mapa — RNF-07). A Feira da 25 está com posição aproximada para conferir aqui.
 */
export default function MarketsAdminPage() {
  const { t } = useTranslation('admin');
  const query = useQuery({
    queryKey: ['admin', 'markets'],
    queryFn: () => api.get<AdminMarket[]>('/admin/markets'),
    networkMode: 'online',
  });
  const [selected, setSelected] = useState<string | null>(null);
  const current = query.data?.find((m) => m.slug === selected) ?? query.data?.[0];

  return (
    <div className="grid gap-6">
      <AdminHeader title={t('markets.title')} lead={t('markets.lead')} />
      {query.isPending && <LoadingRows rows={4} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data && (
        <div className="flex flex-wrap gap-2" role="tablist" aria-label={t('markets.title')}>
          {query.data.map((m) => (
            <Button
              key={m.slug}
              role="tab"
              aria-selected={current?.slug === m.slug}
              variant={current?.slug === m.slug ? 'primary' : 'secondary'}
              onClick={() => setSelected(m.slug)}
            >
              {m.name}
            </Button>
          ))}
        </div>
      )}
      {current && <MarketEditor key={`${current.slug}-${current.version}`} market={current} />}
    </div>
  );
}

function MarketEditor({ market }: { market: AdminMarket }) {
  const { t, i18n } = useTranslation('admin');
  const client = useQueryClient();
  const [name, setName] = useState(market.name);
  const [address, setAddress] = useState(market.address);
  const [lat, setLat] = useState(String(market.latitude));
  const [lng, setLng] = useState(String(market.longitude));
  const [radius, setRadius] = useState(String(market.radiusMeters));
  const [active, setActive] = useState(market.active);
  const [description, setDescription] = useState(market.translations['pt-BR']?.description ?? '');
  const [howTo, setHowTo] = useState(market.translations['pt-BR']?.howToGetThere ?? '');
  useEffect(() => setActive(market.active), [market.active]);

  const latN = Number(lat);
  const lngN = Number(lng);
  const radiusN = Number(radius);
  const valid =
    Number.isFinite(latN) &&
    Number.isFinite(lngN) &&
    Math.abs(latN) <= 90 &&
    Math.abs(lngN) <= 180 &&
    radiusN >= 50 &&
    radiusN <= 2000;

  const save = useMutation({
    mutationFn: () =>
      api.put<AdminMarket>(
        `/admin/markets/${market.slug}`,
        {
          name: name.trim(),
          address: address.trim(),
          latitude: latN,
          longitude: lngN,
          radiusMeters: radiusN,
          openingHours: market.openingHours,
          active,
          translations: {
            ...market.translations,
            'pt-BR': { description: description.trim() || null, howToGetThere: howTo.trim() || null },
          },
          expectedVersion: market.version,
        },
        { retries: 0 },
      ),
    onSuccess: () => {
      toast.success(t('markets.saved'));
      void client.invalidateQueries({ queryKey: ['admin', 'markets'] });
      void client.invalidateQueries({ queryKey: ['public'] });
    },
    onError: (e) => toast.error(errorMessage(e, t('markets.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (valid) save.mutate();
  };

  return (
    <form onSubmit={submit} className="grid gap-8 lg:grid-cols-2">
      <div className="grid content-start gap-4">
        <TextField
          label={t('markets.name')}
          value={name}
          required
          maxLength={120}
          onChange={(e) => setName(e.target.value)}
        />
        <TextField
          label={t('markets.address')}
          value={address}
          required
          maxLength={255}
          onChange={(e) => setAddress(e.target.value)}
        />
        <div className="grid grid-cols-2 gap-3">
          <TextField
            label={t('markets.latitude')}
            inputMode="decimal"
            value={lat}
            onChange={(e) => setLat(e.target.value)}
          />
          <TextField
            label={t('markets.longitude')}
            inputMode="decimal"
            value={lng}
            onChange={(e) => setLng(e.target.value)}
          />
        </div>
        <TextField
          label={t('markets.radius')}
          hint={t('markets.radiusHint')}
          type="number"
          min={50}
          max={2000}
          step={10}
          value={radius}
          error={radiusN < 50 || radiusN > 2000 ? t('markets.radiusError') : null}
          onChange={(e) => setRadius(e.target.value)}
        />
        <TextareaField
          label={t('markets.description')}
          maxLength={1000}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <TextareaField
          label={t('markets.howTo')}
          maxLength={1000}
          value={howTo}
          onChange={(e) => setHowTo(e.target.value)}
        />
        <CheckboxField
          label={t('markets.active')}
          checked={active}
          onChange={(e) => setActive(e.target.checked)}
        />
        <p className="text-sm text-support">{t('markets.translationsNote')}</p>
        <Button type="submit" className="justify-self-start" busy={save.isPending} disabled={!valid}>
          {t('markets.save')}
        </Button>
      </div>
      <div className="grid content-start gap-4">
        <Suspense fallback={<div className="skeleton h-80 rounded-[var(--radius-panel)]" />}>
          <MapView
            label={t('markets.mapLabel', { name })}
            height={340}
            zoom={16}
            center={valid ? [latN, lngN] : undefined}
            points={valid ? [{ id: market.slug, lat: latN, lng: lngN, title: name, radius: radiusN }] : []}
            onPick={(a, b) => {
              setLat(a.toFixed(6));
              setLng(b.toFixed(6));
            }}
          />
        </Suspense>
        <p className="text-sm text-support">
          {t('markets.mapHint', { radius: formatNumber(radiusN || 0, i18n.language) })}
        </p>
        <section className="grid gap-2">
          <SectionTitle aside={t('markets.dataVersion', { version: market.dataVersion })}>
            {t('markets.sectors')}
          </SectionTitle>
          <ul className="divide-y divide-line">
            {market.sectors.map((s) => (
              <li key={s.slug} className="flex items-center justify-between gap-3 py-2">
                <span>{s.names['pt-BR'] ?? s.slug}</span>
                <Pill tone={s.active ? 'good' : 'neutral'}>
                  {s.active ? t('markets.sectorActive') : t('markets.sectorInactive')}
                </Pill>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </form>
  );
}
