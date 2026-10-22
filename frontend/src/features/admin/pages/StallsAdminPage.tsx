import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page } from '../../../shared/api/types';
import { formatDate } from '../../../shared/lib/format';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, SelectField, TextField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useDebounced, useMarket, useMarkets } from '../../public-api';
import type { AdminStall, AdminStallDetail } from '../types';
import { AdminHeader, errorMessage, Pager, Pill, selectClass } from '../ui';

/**
 * Boxes (RF-29): lista com estado e consentimento (RN-13) e o cadastro do box conferido em campo, a partir do
 * termo assinado pelo feirante. Os dados, as ofertas e as fotos se editam na página do box.
 */
export default function StallsAdminPage() {
  const { t, i18n } = useTranslation('admin');
  const markets = useMarkets();
  const [market, setMarket] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
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
      <AdminHeader
        title={t('stalls.title')}
        lead={t('stalls.lead')}
        actions={
          <Button icon={<Plus size={18} aria-hidden="true" />} onClick={() => setCreating(true)}>
            {t('stalls.newStall')}
          </Button>
        }
      />
      <div className="flex flex-wrap items-end gap-3">
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('filters.market')}</span>
          <select
            className={selectClass}
            value={market}
            onChange={(e) => (setMarket(e.target.value), setPage(0))}
          >
            <option value="">{t('filters.allMarkets')}</option>
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
            <Link
              to={`/admin/stalls/${s.publicId}`}
              className="grid grid-cols-[3rem_1fr] items-center gap-x-3 gap-y-1 py-3 text-ink no-underline hover:bg-soft sm:grid-cols-[3rem_1fr_auto]"
            >
              {s.photoThumbUrl ? (
                <img
                  src={s.photoThumbUrl}
                  width={48}
                  height={48}
                  loading="lazy"
                  decoding="async"
                  alt=""
                  className="size-12 rounded-[var(--radius-control)] bg-soft object-cover"
                />
              ) : (
                <span aria-hidden="true" className="size-12 rounded-[var(--radius-control)] bg-soft" />
              )}
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
              <span className="col-start-2 flex flex-wrap gap-1 sm:col-start-auto">
                {s.demo && <Pill>{t('common:demoTag')}</Pill>}
                <Pill tone={s.status === 'ACTIVE' ? 'good' : 'neutral'}>
                  {t(`stalls.status.${s.status}`)}
                </Pill>
                <Pill tone={s.consentStatus === 'GRANTED' ? 'good' : 'bad'}>
                  {t(`stalls.consent.${s.consentStatus}`)}
                </Pill>
              </span>
            </Link>
          </li>
        ))}
      </ul>
      {list.data && <Pager page={list.data.page} totalPages={list.data.totalPages} onPage={setPage} />}
      {creating && <NewStallSheet initialMarket={market} onClose={() => setCreating(false)} />}
    </div>
  );
}

/** Cadastro a partir do termo assinado na feira (RN-13): o box nasce real, com o consentimento registrado. */
function NewStallSheet({ initialMarket, onClose }: { initialMarket: string; onClose: () => void }) {
  const { t } = useTranslation('admin');
  const client = useQueryClient();
  const navigate = useNavigate();
  const markets = useMarkets();
  const [marketSlug, setMarketSlug] = useState(initialMarket || markets.data?.[0]?.slug || '');
  const market = useMarket(marketSlug);
  const [sectorSlug, setSectorSlug] = useState('');
  const [code, setCode] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [vendorName, setVendorName] = useState('');
  const [showVendorName, setShowVendorName] = useState(false);
  const [method, setMethod] = useState<'SIGNED_FORM' | 'VERBAL'>('SIGNED_FORM');
  const [instagram, setInstagram] = useState('');
  const [touched, setTouched] = useState(false);
  const chosenMarket = marketSlug || markets.data?.[0]?.slug || '';

  const create = useMutation({
    mutationFn: () =>
      api.post<AdminStallDetail>(
        '/admin/stalls',
        {
          marketSlug: chosenMarket,
          sectorSlug: sectorSlug || null,
          code: code.trim(),
          displayName: displayName.trim() || null,
          vendorName: vendorName.trim() || null,
          showVendorName,
          consentMethod: method,
          instagram: instagram.trim() || null,
        },
        { retries: 0 },
      ),
    onSuccess: (detail) => {
      toast.success(t('stalls.created'));
      client.setQueryData(['admin', 'stall', detail.stall.publicId], detail);
      void client.invalidateQueries({ queryKey: ['admin', 'stalls'] });
      onClose();
      void navigate(`/admin/stalls/${detail.stall.publicId}`);
    },
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (chosenMarket && code.trim()) create.mutate();
  };

  return (
    <Sheet
      open
      onOpenChange={(o) => !o && onClose()}
      title={t('stalls.newTitle')}
      description={t('stalls.newBody')}
    >
      <form onSubmit={submit} className="grid gap-4">
        <SelectField
          label={t('stalls.market')}
          value={chosenMarket}
          onChange={(e) => (setMarketSlug(e.target.value), setSectorSlug(''))}
        >
          {markets.data?.map((m) => (
            <option key={m.slug} value={m.slug}>
              {m.name}
            </option>
          ))}
        </SelectField>
        <TextField
          label={t('stalls.code')}
          value={code}
          maxLength={20}
          required
          error={touched && !code.trim() ? t('stalls.codeRequired') : null}
          onChange={(e) => setCode(e.target.value)}
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
          label={t('stalls.consentMethod')}
          value={method}
          onChange={(e) => setMethod(e.target.value as typeof method)}
        >
          <option value="SIGNED_FORM">{t('collab:consent.SIGNED_FORM')}</option>
          <option value="VERBAL">{t('collab:consent.VERBAL')}</option>
        </SelectField>
        <TextField
          label={t('stalls.instagram')}
          hint={t('stalls.instagramHint')}
          value={instagram}
          maxLength={80}
          autoComplete="off"
          onChange={(e) => setInstagram(e.target.value)}
        />
        <Button type="submit" block busy={create.isPending}>
          {t('stalls.create')}
        </Button>
      </form>
    </Sheet>
  );
}
