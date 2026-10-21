import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page } from '../../../shared/api/types';
import { formatDate } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, SelectField, TextField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useDebounced, useMarket, useMarkets } from '../../public-api';
import type { AdminStall } from '../types';
import { AdminHeader, errorMessage, Pager, Pill, selectClass } from '../ui';

/**
 * Boxes (RF-30): dados, setor, estado e consentimento do feirante (RN-13). Revogar o consentimento tira o box do
 * ar na hora e agenda a remoção das fotos (RN-13).
 */
export default function StallsAdminPage() {
  const { t, i18n } = useTranslation('admin');
  const markets = useMarkets();
  const [market, setMarket] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<AdminStall | null>(null);
  const query = useDebounced(q);
  const params = new URLSearchParams({ page: String(page), size: '30' });
  if (market) params.set('market', market);
  if (query.trim()) params.set('q', query.trim());
  const list = useQuery({
    queryKey: ['admin', 'stalls', market, query, page],
    queryFn: () => api.get<Page<AdminStall>>(`/admin/stalls?${params.toString()}`),
    networkMode: 'online',
  });

  return (
    <div className="grid gap-6">
      <AdminHeader title={t('stalls.title')} lead={t('stalls.lead')} />
      <div className="flex flex-wrap items-end gap-3">
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('filters.market')}</span>
          <select
            className={selectClass}
            value={market}
            onChange={(e) => (setMarket(e.target.value), setPage(0))}
          >
            <option value="">{t('filters.all')}</option>
            {markets.data?.map((m) => (
              <option key={m.slug} value={m.slug}>
                {m.name}
              </option>
            ))}
          </select>
        </label>
        <TextField
          label={t('stalls.search')}
          value={q}
          className="min-w-[14rem]"
          onChange={(e) => (setQ(e.target.value), setPage(0))}
        />
      </div>
      {list.isPending && <LoadingRows rows={6} />}
      {list.isError && <ErrorState error={list.error} onRetry={() => void list.refetch()} />}
      {list.data?.items.length === 0 && (
        <EmptyState title={t('stalls.emptyTitle')} body={t('stalls.emptyBody')} />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {list.data?.items.map((s) => (
          <li key={s.publicId}>
            <button
              type="button"
              onClick={() => setEditing(s)}
              className="grid w-full gap-1 py-3 text-left hover:bg-soft sm:grid-cols-[1fr_auto] sm:items-center"
            >
              <span className="grid gap-0.5">
                <span className="font-semibold">
                  <span className="display tabular text-brand">
                    {t('common:stall.code', { code: s.code })}
                  </span>
                  {s.displayName && ` · ${s.displayName}`}
                </span>
                <span className="text-sm text-support">
                  {s.sectorName ?? t('stalls.noSector')} ·{' '}
                  {s.lastVerifiedAt
                    ? t('stalls.verified', { date: formatDate(s.lastVerifiedAt, i18n.language) })
                    : t('stalls.neverVerified')}
                </span>
              </span>
              <span className="flex flex-wrap gap-1">
                <Pill tone={s.status === 'ACTIVE' ? 'good' : 'neutral'}>
                  {t(`stalls.status.${s.status}`)}
                </Pill>
                <Pill tone={s.consentStatus === 'GRANTED' ? 'good' : 'bad'}>
                  {t(`stalls.consent.${s.consentStatus}`)}
                </Pill>
              </span>
            </button>
          </li>
        ))}
      </ul>
      {list.data && <Pager page={list.data.page} totalPages={list.data.totalPages} onPage={setPage} />}
      {editing && <StallSheet stall={editing} onClose={() => setEditing(null)} />}
    </div>
  );
}

function StallSheet({ stall, onClose }: { stall: AdminStall; onClose: () => void }) {
  const { t } = useTranslation('admin');
  const client = useQueryClient();
  const market = useMarket(stall.marketSlug);
  const [displayName, setDisplayName] = useState(stall.displayName ?? '');
  const [vendorName, setVendorName] = useState(stall.vendorName ?? '');
  const [showVendorName, setShowVendorName] = useState(stall.showVendorName);
  const [sectorSlug, setSectorSlug] = useState(stall.sectorSlug ?? '');
  const [status, setStatus] = useState(stall.status);
  const [method, setMethod] = useState<'VERBAL' | 'SIGNED_FORM'>('VERBAL');
  const after = () => {
    void client.invalidateQueries({ queryKey: ['admin', 'stalls'] });
    void client.invalidateQueries({ queryKey: ['public'] });
    onClose();
  };
  const save = useMutation({
    mutationFn: () =>
      api.patch(
        `/admin/stalls/${stall.publicId}`,
        {
          displayName: displayName.trim() || null,
          vendorName: vendorName.trim() || null,
          showVendorName,
          sectorSlug: sectorSlug || null,
          status,
          version: stall.version,
        },
        { retries: 0 },
      ),
    onSuccess: () => (toast.success(t('stalls.saved')), after()),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const revoke = useMutation({
    mutationFn: () => api.post(`/admin/stalls/${stall.publicId}/consent/revoke`, undefined, { retries: 0 }),
    onSuccess: () => (toast.success(t('stalls.revoked')), after()),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const grant = useMutation({
    mutationFn: () => api.post(`/admin/stalls/${stall.publicId}/consent/grant`, { method }, { retries: 0 }),
    onSuccess: () => (toast.success(t('stalls.granted')), after()),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate();
  };
  return (
    <Sheet
      open
      onOpenChange={(o) => !o && onClose()}
      title={t('common:stall.code', { code: stall.code })}
      description={t('stalls.editBody')}
    >
      <form onSubmit={submit} className="grid gap-4">
        <TextField
          label={t('stalls.displayName')}
          value={displayName}
          maxLength={120}
          onChange={(e) => setDisplayName(e.target.value)}
        />
        <TextField
          label={t('stalls.vendorName')}
          value={vendorName}
          maxLength={120}
          onChange={(e) => setVendorName(e.target.value)}
        />
        <CheckboxField
          label={t('stalls.showVendorName')}
          checked={showVendorName}
          onChange={(e) => setShowVendorName(e.target.checked)}
        />
        <SelectField
          label={t('stalls.sector')}
          value={sectorSlug}
          onChange={(e) => setSectorSlug(e.target.value)}
        >
          <option value="">{t('stalls.noSector')}</option>
          {market.data?.sectors.map((s) => (
            <option key={s.slug} value={s.slug}>
              {s.name}
            </option>
          ))}
        </SelectField>
        <SelectField
          label={t('stalls.statusLabel')}
          value={status}
          onChange={(e) => setStatus(e.target.value as AdminStall['status'])}
        >
          <option value="ACTIVE">{t('stalls.status.ACTIVE')}</option>
          <option value="INACTIVE">{t('stalls.status.INACTIVE')}</option>
        </SelectField>
        <Button type="submit" block busy={save.isPending}>
          {t('stalls.save')}
        </Button>
      </form>
      <section className="mt-6 grid gap-3 border-t border-line pt-4">
        <h3 className="text-lg">{t('stalls.consentTitle')}</h3>
        <p className="text-sm text-support">
          {stall.consentStatus === 'GRANTED'
            ? t('stalls.consentGranted', {
                method: stall.consentMethod ? t(`collab:consent.${stall.consentMethod}`) : '—',
              })
            : t('stalls.consentRevoked')}
        </p>
        {stall.consentStatus === 'GRANTED' ? (
          <Button variant="danger" busy={revoke.isPending} onClick={() => revoke.mutate()}>
            {t('stalls.revoke')}
          </Button>
        ) : (
          <div className="grid gap-3">
            <SelectField
              label={t('stalls.consentMethod')}
              value={method}
              onChange={(e) => setMethod(e.target.value as typeof method)}
            >
              <option value="VERBAL">{t('collab:consent.VERBAL')}</option>
              <option value="SIGNED_FORM">{t('collab:consent.SIGNED_FORM')}</option>
            </SelectField>
            <Button variant="secondary" busy={grant.isPending} onClick={() => grant.mutate()}>
              {t('stalls.grant')}
            </Button>
          </div>
        )}
      </section>
    </Sheet>
  );
}
