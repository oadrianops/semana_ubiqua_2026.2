import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ImagePlus, Trash2 } from 'lucide-react';
import { useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { api, request } from '../../../shared/api/http';
import type { Availability, OfferingView, PriceUnit } from '../../../shared/api/types';
import { compressImage } from '../../../shared/lib/image-compress';
import { Button } from '../../../shared/ui/Button';
import { CheckboxField, SelectField, TextField } from '../../../shared/ui/Field';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import { toast } from '../../../shared/ui/toast';
import { useDebounced, useMarket, useSuggestions } from '../../public-api';
import { PriceValue } from '../../shared/board';
import type { AdminPhoto, AdminStall, AdminStallDetail } from '../types';
import { AdminHeader, errorMessage, moneyFrom, Pill, SectionTitle } from '../ui';

const UNITS: PriceUnit[] = ['KG', 'UNIT', 'BUNCH', 'LITER', 'PORTION', 'PACKAGE', 'BOTTLE', 'DOZEN'];
const AVAILABILITY: Availability[] = ['AVAILABLE', 'SEASONAL', 'OUT_OF_STOCK'];

/**
 * Box no painel (RF-29): dados e Instagram, o que vende com preço de referência (RN-15, RN-24), fotos sem pessoas
 * de rosto (RN-22) e consentimento (RN-13). Lançar uma oferta conta como verificação do box (RN-12).
 */
export default function StallEditorPage() {
  const { publicId = '' } = useParams();
  const { t } = useTranslation('admin');
  const detail = useStallDetail(publicId);
  const market = useMarket(detail.data?.stall.marketSlug ?? '');

  if (detail.isPending) return <LoadingRows rows={8} />;
  if (detail.isError) return <ErrorState error={detail.error} onRetry={() => void detail.refetch()} />;
  const { stall, offerings, photos } = detail.data;
  const lead = [market.data?.name, stall.sectorName].filter(Boolean).join(' · ');

  return (
    <div className="grid gap-10">
      <div className="grid gap-2">
        <p className="text-sm">
          <Link to="/admin/stalls" className="inline-flex min-h-11 items-center gap-1.5">
            <ArrowLeft size={16} aria-hidden="true" />
            {t('stalls.title')}
          </Link>
        </p>
        <AdminHeader
          title={[t('common:stall.code', { code: stall.code }), stall.displayName]
            .filter(Boolean)
            .join(' · ')}
          lead={lead || undefined}
          actions={stall.demo ? <Pill>{t('common:demoTag')}</Pill> : undefined}
        />
      </div>
      <StallDataForm key={stall.version} stall={stall} />
      <OfferingsEditor stallId={stall.publicId} offerings={offerings} />
      <PhotosEditor stallId={stall.publicId} photos={photos} />
      <ConsentSection stall={stall} />
    </div>
  );
}

function useStallDetail(publicId: string) {
  return useQuery({
    queryKey: ['admin', 'stall', publicId],
    queryFn: () => api.get<AdminStallDetail>(`/admin/stalls/${publicId}`),
    networkMode: 'online',
  });
}

/** Grava a resposta (o box inteiro) no cache e invalida as listas do painel e da vitrine. */
function useApplyDetail(publicId: string) {
  const client = useQueryClient();
  return (detail?: AdminStallDetail) => {
    if (detail) client.setQueryData(['admin', 'stall', publicId], detail);
    else void client.invalidateQueries({ queryKey: ['admin', 'stall', publicId] });
    void client.invalidateQueries({ queryKey: ['admin', 'stalls'] });
    void client.invalidateQueries({ queryKey: ['public'] });
  };
}

function StallDataForm({ stall }: { stall: AdminStall }) {
  const { t } = useTranslation('admin');
  const apply = useApplyDetail(stall.publicId);
  const market = useMarket(stall.marketSlug);
  const [displayName, setDisplayName] = useState(stall.displayName ?? '');
  const [vendorName, setVendorName] = useState(stall.vendorName ?? '');
  const [showVendorName, setShowVendorName] = useState(stall.showVendorName);
  const [sectorSlug, setSectorSlug] = useState(stall.sectorSlug ?? '');
  const [status, setStatus] = useState(stall.status);
  const [instagram, setInstagram] = useState(stall.instagram ? `@${stall.instagram}` : '');
  const [demo, setDemo] = useState(stall.demo);
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
          instagram: instagram.trim(),
          demo,
          version: stall.version,
        },
        { retries: 0 },
      ),
    onSuccess: () => (toast.success(t('stalls.saved')), apply()),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate();
  };
  return (
    <section aria-labelledby="stall-data" className="grid max-w-[var(--measure)] gap-4">
      <SectionTitle id="stall-data">{t('stalls.dataTitle')}</SectionTitle>
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
        <TextField
          label={t('stalls.instagram')}
          hint={t('stalls.instagramHint')}
          value={instagram}
          maxLength={80}
          autoComplete="off"
          onChange={(e) => setInstagram(e.target.value)}
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
        <div className="grid gap-1">
          <CheckboxField
            label={t('stalls.demo')}
            checked={demo}
            onChange={(e) => setDemo(e.target.checked)}
          />
          <p className="pl-8 text-sm text-support">{t('stalls.demoHint')}</p>
        </div>
        <Button type="submit" className="justify-self-start" busy={save.isPending}>
          {t('stalls.save')}
        </Button>
      </form>
    </section>
  );
}

interface OfferingDraft {
  productSlug: string | null;
  productName: string;
  price: string;
  priceUnit: PriceUnit;
  availability: Availability;
}

const emptyDraft: OfferingDraft = {
  productSlug: null,
  productName: '',
  price: '',
  priceUnit: 'KG',
  availability: 'AVAILABLE',
};

function OfferingsEditor({ stallId, offerings }: { stallId: string; offerings: OfferingView[] }) {
  const { t } = useTranslation('admin');
  const apply = useApplyDetail(stallId);
  const [draft, setDraft] = useState<OfferingDraft>(emptyDraft);
  const [touched, setTouched] = useState(false);
  const money = draft.price.trim() ? moneyFrom(draft.price) : null;
  const priceError = draft.price.trim() && !money ? t('stalls.priceError') : null;

  const save = useMutation({
    mutationFn: () =>
      api.put<AdminStallDetail>(
        `/admin/stalls/${stallId}/offerings/${draft.productSlug}`,
        { price: money?.amount ?? null, priceUnit: draft.priceUnit, availability: draft.availability },
        { retries: 0 },
      ),
    onSuccess: (d) => (
      toast.success(t('stalls.offeringSaved')),
      apply(d),
      setDraft(emptyDraft),
      setTouched(false)
    ),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const remove = useMutation({
    mutationFn: (slug: string) =>
      api.delete<AdminStallDetail>(`/admin/stalls/${stallId}/offerings/${slug}`, { retries: 0 }),
    onSuccess: (d) => (toast.success(t('stalls.offeringRemoved')), apply(d)),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (draft.productSlug && !priceError) save.mutate();
  };
  const edit = (o: OfferingView) =>
    setDraft({
      productSlug: o.productSlug,
      productName: o.productName,
      price: o.price ? o.price.amount.replace('.', ',') : '',
      priceUnit: o.priceUnit ?? 'UNIT',
      availability: o.availability,
    });

  return (
    <section aria-labelledby="stall-offerings" className="grid gap-4">
      <SectionTitle id="stall-offerings">{t('stalls.offeringsTitle')}</SectionTitle>
      {offerings.length === 0 ? (
        <p className="text-support">{t('stalls.noOfferings')}</p>
      ) : (
        <ul className="divide-y divide-line border-t-2 border-ink">
          {offerings.map((o) => (
            <li key={o.publicId} className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 py-2">
              <button
                type="button"
                onClick={() => edit(o)}
                className="min-h-11 text-left font-semibold text-ink underline-offset-4 hover:underline"
              >
                {o.productName}
              </button>
              <span className="flex items-center gap-3">
                {o.price ? (
                  <PriceValue price={o.price} unit={o.priceUnit} availability={o.availability} />
                ) : (
                  <span className="text-support">{t('stalls.noPrice')}</span>
                )}
                <button
                  type="button"
                  onClick={() => remove.mutate(o.productSlug)}
                  disabled={remove.isPending}
                  aria-label={t('stalls.removeOffering', { product: o.productName })}
                  className="inline-flex size-11 items-center justify-center rounded-[var(--radius-control)] text-danger hover:bg-soft disabled:opacity-50"
                >
                  <Trash2 size={18} aria-hidden="true" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
      <form
        onSubmit={submit}
        className="grid max-w-[var(--measure)] gap-4 rounded-[var(--radius-panel)] bg-soft p-4"
      >
        <ProductField
          draft={draft}
          error={touched && !draft.productSlug ? t('stalls.productRequired') : null}
          onPick={(slug, name) => setDraft((d) => ({ ...d, productSlug: slug, productName: name }))}
          onClear={() => setDraft((d) => ({ ...d, productSlug: null }))}
        />
        <TextField
          label={t('stalls.price')}
          hint={t('stalls.priceHint')}
          error={priceError}
          inputMode="decimal"
          value={draft.price}
          onChange={(e) => setDraft((d) => ({ ...d, price: e.target.value }))}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <SelectField
            label={t('stalls.unit')}
            value={draft.priceUnit}
            onChange={(e) => setDraft((d) => ({ ...d, priceUnit: e.target.value as PriceUnit }))}
          >
            {UNITS.map((u) => (
              <option key={u} value={u}>
                {t(`common:unit.${u}`)}
              </option>
            ))}
          </SelectField>
          <SelectField
            label={t('stalls.availability')}
            value={draft.availability}
            onChange={(e) => setDraft((d) => ({ ...d, availability: e.target.value as Availability }))}
          >
            {AVAILABILITY.map((a) => (
              <option key={a} value={a}>
                {t(`common:availability.${a}`)}
              </option>
            ))}
          </SelectField>
        </div>
        <Button type="submit" className="justify-self-start" busy={save.isPending}>
          {t('stalls.saveOffering')}
        </Button>
      </form>
    </section>
  );
}

function ProductField({
  draft,
  error,
  onPick,
  onClear,
}: {
  draft: OfferingDraft;
  error: string | null;
  onPick: (slug: string, name: string) => void;
  onClear: () => void;
}) {
  const { t } = useTranslation('admin');
  const [text, setText] = useState('');
  const debounced = useDebounced(text);
  const suggestions = useSuggestions(draft.productSlug ? '' : debounced);

  if (draft.productSlug) {
    return (
      <div className="grid gap-1.5">
        <p className="text-base font-semibold text-ink">{t('stalls.product')}</p>
        <p className="flex min-h-11 flex-wrap items-center gap-3">
          <span className="font-semibold">{draft.productName}</span>
          <button
            type="button"
            className="min-h-11 text-sm font-semibold text-brand underline"
            onClick={() => (onClear(), setText(''))}
          >
            {t('collab:offerings.change')}
          </button>
        </p>
      </div>
    );
  }
  return (
    <div className="grid gap-1.5">
      <TextField
        label={t('stalls.product')}
        hint={t('stalls.productHint')}
        error={error}
        value={text}
        autoComplete="off"
        onChange={(e) => setText(e.target.value)}
      />
      {suggestions.data && suggestions.data.length > 0 && (
        <ul className="divide-y divide-line rounded-[var(--radius-control)] border border-line bg-surface">
          {suggestions.data.slice(0, 6).map((s) => (
            <li key={s.slug}>
              <button
                type="button"
                className="min-h-11 w-full px-3 text-left hover:bg-soft"
                onClick={() => onPick(s.slug, s.name)}
              >
                {s.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function PhotosEditor({ stallId, photos }: { stallId: string; photos: AdminPhoto[] }) {
  const { t } = useTranslation('admin');
  const apply = useApplyDetail(stallId);
  const input = useRef<HTMLInputElement>(null);
  const [asMain, setAsMain] = useState(photos.length === 0);
  const [uploading, setUploading] = useState(false);

  async function upload(files: FileList | null) {
    if (!files || files.length === 0) return;
    setUploading(true);
    let main = asMain;
    try {
      // Uma foto por vez (RN-27): numa queda de sinal, só a que faltou precisa ser repetida.
      for (const file of Array.from(files)) {
        const form = new FormData();
        form.append('file', await compressImage(file), 'foto.jpg');
        const detail = await request<AdminStallDetail>(`/admin/stalls/${stallId}/photos?main=${main}`, {
          method: 'POST',
          form,
          retries: 0,
        });
        apply(detail);
        main = false;
      }
      toast.success(t('stalls.photoAdded'));
      setAsMain(false);
    } catch (e) {
      toast.error(errorMessage(e, t('stalls.error')));
    } finally {
      setUploading(false);
      if (input.current) input.current.value = '';
    }
  }

  const remove = useMutation({
    mutationFn: (fileId: string) =>
      api.delete<AdminStallDetail>(`/admin/stalls/${stallId}/photos/${fileId}`, { retries: 0 }),
    onSuccess: (d) => (toast.success(t('stalls.photoRemoved')), apply(d)),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });

  return (
    <section aria-labelledby="stall-photos" className="grid gap-4">
      <SectionTitle id="stall-photos">{t('stalls.photosTitle')}</SectionTitle>
      <p className="max-w-[var(--measure)] text-support">{t('stalls.photosHint')}</p>
      {photos.length === 0 ? (
        <p className="text-support">{t('stalls.noPhotos')}</p>
      ) : (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {photos.map((p, i) => (
            <li key={p.fileId} className="grid gap-1.5">
              <img
                src={p.thumbUrl}
                width={400}
                height={300}
                loading="lazy"
                decoding="async"
                alt={t('stalls.photoAlt', { n: i + 1 })}
                className="aspect-[4/3] w-full rounded-[var(--radius-control)] bg-soft object-cover"
              />
              <span className="flex items-center justify-between gap-2">
                {p.main ? <Pill tone="brand">{t('stalls.mainPhoto')}</Pill> : <span />}
                <button
                  type="button"
                  onClick={() => remove.mutate(p.fileId)}
                  disabled={remove.isPending}
                  aria-label={t('stalls.removePhoto', { n: i + 1 })}
                  className="inline-flex size-11 items-center justify-center rounded-[var(--radius-control)] text-danger hover:bg-soft disabled:opacity-50"
                >
                  <Trash2 size={18} aria-hidden="true" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
      <div className="flex flex-wrap items-center gap-4">
        <input
          ref={input}
          type="file"
          accept="image/jpeg,image/png"
          multiple
          className="sr-only"
          tabIndex={-1}
          aria-label={t('stalls.addPhoto')}
          onChange={(e) => void upload(e.target.files)}
        />
        <Button
          variant="secondary"
          icon={<ImagePlus size={18} aria-hidden="true" />}
          busy={uploading}
          onClick={() => input.current?.click()}
        >
          {uploading ? t('stalls.uploading') : t('stalls.addPhoto')}
        </Button>
        <CheckboxField
          label={t('stalls.asMain')}
          checked={asMain}
          onChange={(e) => setAsMain(e.target.checked)}
        />
      </div>
    </section>
  );
}

function ConsentSection({ stall }: { stall: AdminStall }) {
  const { t } = useTranslation('admin');
  const apply = useApplyDetail(stall.publicId);
  const [method, setMethod] = useState<'VERBAL' | 'SIGNED_FORM'>('SIGNED_FORM');
  const revoke = useMutation({
    mutationFn: () => api.post(`/admin/stalls/${stall.publicId}/consent/revoke`, undefined, { retries: 0 }),
    onSuccess: () => (toast.success(t('stalls.revoked')), apply()),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  const grant = useMutation({
    mutationFn: () => api.post(`/admin/stalls/${stall.publicId}/consent/grant`, { method }, { retries: 0 }),
    onSuccess: () => (toast.success(t('stalls.granted')), apply()),
    onError: (e) => toast.error(errorMessage(e, t('stalls.error'))),
  });
  return (
    <section aria-labelledby="stall-consent" className="grid max-w-[var(--measure)] gap-3">
      <SectionTitle id="stall-consent">{t('stalls.consentTitle')}</SectionTitle>
      <p className="text-support">
        {stall.consentStatus === 'GRANTED'
          ? t('stalls.consentGranted', {
              method: stall.consentMethod ? t(`collab:consent.${stall.consentMethod}`) : '—',
            })
          : t('stalls.consentRevoked')}
      </p>
      {stall.consentStatus === 'GRANTED' ? (
        <Button
          variant="danger"
          className="justify-self-start"
          busy={revoke.isPending}
          onClick={() => revoke.mutate()}
        >
          {t('stalls.revoke')}
        </Button>
      ) : (
        <div className="grid gap-3">
          <SelectField
            label={t('stalls.consentMethod')}
            value={method}
            onChange={(e) => setMethod(e.target.value as typeof method)}
          >
            <option value="SIGNED_FORM">{t('collab:consent.SIGNED_FORM')}</option>
            <option value="VERBAL">{t('collab:consent.VERBAL')}</option>
          </SelectField>
          <Button
            variant="secondary"
            className="justify-self-start"
            busy={grant.isPending}
            onClick={() => grant.mutate()}
          >
            {t('stalls.grant')}
          </Button>
        </div>
      )}
    </section>
  );
}
