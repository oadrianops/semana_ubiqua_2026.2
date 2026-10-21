import { Camera, Plus, Trash2, X } from 'lucide-react';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../shared/ui/toast';
import type {
  Availability,
  ChangeSet,
  OfferingChange,
  PriceUnit,
  StallDetail,
  TaskDetail,
} from '../../shared/api/types';
import { compressImage } from '../../shared/lib/image-compress';
import { cn } from '../../shared/lib/cn';
import { formatMoney } from '../../shared/lib/format';
import {
  clearDraft,
  enqueue,
  loadDraft,
  newId,
  saveDraft,
  uploadPhoto,
  type QueuedPhoto,
} from '../../shared/offline/outbox';
import { Button } from '../../shared/ui/Button';
import { CheckboxField, SelectField, TextareaField, TextField } from '../../shared/ui/Field';
import { LoadingRows } from '../../shared/ui/States';
import { useCategories, useDebounced, useMarket, useStall, useSuggestions } from '../public-api';

const UNITS: PriceUnit[] = ['KG', 'UNIT', 'LITER', 'BUNCH', 'PORTION', 'PACKAGE', 'BOTTLE', 'DOZEN'];
const AVAILABILITY: Availability[] = ['AVAILABLE', 'OUT_OF_STOCK', 'SEASONAL'];
const MAX_PHOTOS = 5;

type RowAction = 'KEEP' | 'CONFIRM' | 'UPDATE' | 'REMOVE';

interface ExistingRow {
  productSlug: string;
  productName: string;
  baseVersion: number;
  action: RowAction;
  price: string;
  unit: PriceUnit;
  availability: Availability;
}

interface NewRow {
  key: string;
  productSlug: string | null;
  productName: string;
  suggestion: { namePt: string; categorySlug: string } | null;
  price: string;
  unit: PriceUnit;
  availability: Availability;
}

export interface FormState {
  existing: ExistingRow[];
  added: NewRow[];
  code: string;
  displayName: string;
  vendorName: string;
  sectorSlug: string;
  consentPublish: boolean;
  consentShowName: boolean;
  consentMethod: 'VERBAL' | 'SIGNED_FORM';
  problemConfirmed: 'yes' | 'no' | '';
  comment: string;
  notes: string;
}

const EMPTY: FormState = {
  existing: [],
  added: [],
  code: '',
  displayName: '',
  vendorName: '',
  sectorSlug: '',
  consentPublish: false,
  consentShowName: false,
  consentMethod: 'VERBAL',
  problemConfirmed: '',
  comment: '',
  notes: '',
};

/** "12,50" ou "12.50" → "12.50"; vazio → null; inválido → undefined. */
export function parsePrice(text: string): string | null | undefined {
  const clean = text.trim().replace(/\s|R\$/g, '');
  if (!clean) return null;
  const normalized = clean.includes(',') ? clean.replace(/\./g, '').replace(',', '.') : clean;
  if (!/^\d{1,5}(\.\d{1,2})?$/.test(normalized)) return undefined;
  const value = Number(normalized);
  return value > 0 ? value.toFixed(2) : undefined;
}

function rowsFromStall(stall: StallDetail): ExistingRow[] {
  return stall.offerings.map((o) => ({
    productSlug: o.productSlug,
    productName: o.productName,
    baseVersion: o.version,
    action: 'KEEP',
    price: o.price ? o.price.amount.replace('.', ',') : '',
    unit: o.priceUnit ?? 'KG',
    availability: o.availability,
  }));
}

/** Monta o changeset do Anexo A a partir do formulário (as fotos entram na fila). */
export function buildChangeSet(task: TaskDetail, form: FormState): ChangeSet {
  const money = (text: string) => {
    const amount = parsePrice(text);
    return amount ? { amount, currency: 'BRL' } : null;
  };
  const offerings: OfferingChange[] = [];
  for (const row of form.existing) {
    if (row.action === 'KEEP') continue;
    offerings.push({
      action: row.action,
      productSlug: row.productSlug,
      baseVersion: row.baseVersion,
      ...(row.action === 'UPDATE'
        ? { referencePrice: money(row.price), priceUnit: row.unit, availability: row.availability }
        : {}),
    });
  }
  for (const row of form.added) {
    if (row.suggestion) {
      offerings.push({
        action: 'ADD_SUGGESTED',
        suggestion: row.suggestion,
        referencePrice: money(row.price),
        priceUnit: row.unit,
        availability: row.availability,
      });
    } else if (row.productSlug) {
      offerings.push({
        action: 'ADD',
        productSlug: row.productSlug,
        referencePrice: money(row.price),
        priceUnit: row.unit,
        availability: row.availability,
      });
    }
  }
  const stall: ChangeSet['stall'] =
    task.type === 'NEW_STALL'
      ? {
          marketSlug: task.market.slug,
          sectorSlug: form.sectorSlug || task.suggestedSectorSlug || null,
          code: form.code.trim(),
          displayName: form.displayName.trim() || null,
          vendorName: form.vendorName.trim() || null,
          consent: {
            publish: form.consentPublish,
            showVendorName: form.consentShowName,
            method: form.consentMethod,
          },
        }
      : task.stall
        ? { publicId: task.stall.publicId, baseVersion: task.stall.version }
        : null;
  return {
    taskType: task.type,
    stall,
    offerings,
    photos: [],
    notes: form.notes.trim() || null,
    verification:
      task.type === 'VERIFY_REPORT'
        ? { problemConfirmed: form.problemConfirmed === 'yes', comment: form.comment.trim() || null }
        : null,
  };
}

/** Problemas que impedem o envio, já com o texto que diz o que fazer (14.5). */
export function validateForm(task: TaskDetail, form: FormState, photos: QueuedPhoto[]): string[] {
  const errors: string[] = [];
  if (photos.length < 1) errors.push('errors.photoRequired');
  if (task.type === 'NEW_STALL') {
    if (!form.code.trim()) errors.push('errors.codeRequired');
    if (!form.consentPublish) errors.push('errors.consentRequired');
    if (!photos.some((p) => p.role === 'STALL')) errors.push('errors.stallPhotoRequired');
  }
  if (task.type === 'VERIFY_REPORT' && !form.problemConfirmed) errors.push('errors.verificationRequired');
  const prices = [...form.existing.filter((r) => r.action === 'UPDATE'), ...form.added].map((r) =>
    parsePrice(r.price),
  );
  if (prices.some((p) => p === undefined)) errors.push('errors.priceInvalid');
  if (form.added.some((r) => !r.productSlug && !r.suggestion?.namePt.trim()))
    errors.push('errors.productRequired');
  const slugs = [
    ...form.existing.map((r) => r.productSlug),
    ...form.added.map((r) => r.productSlug).filter(Boolean),
  ];
  if (new Set(slugs).size !== slugs.length) errors.push('errors.productDuplicated');
  return errors;
}

/**
 * Passo 3 (RF-18): dados do box, produtos e preços de referência, fotos que sobem assim que tiradas e rascunho
 * salvo no aparelho a cada alteração. Enviar coloca tudo na fila; nada vai direto (9.13).
 */
export function SubmissionForm({ task }: { task: TaskDetail }) {
  const { t } = useTranslation('collab');
  const stallQuery = useStall(task.stall?.publicId ?? '');
  const hasStall = Boolean(task.stall);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [photos, setPhotos] = useState<QueuedPhoto[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [sending, setSending] = useState(false);

  // Restaura o rascunho; sem rascunho, parte da lista atual do box.
  useEffect(() => {
    let alive = true;
    void loadDraft<FormState>(task.publicId).then((draft) => {
      if (!alive) return;
      if (draft) {
        setForm({ ...EMPTY, ...draft.form });
        setPhotos(draft.photos);
      }
      setLoaded(true);
    });
    return () => {
      alive = false;
    };
  }, [task.publicId]);

  useEffect(() => {
    if (loaded && stallQuery.data && form.existing.length === 0 && stallQuery.data.offerings.length > 0) {
      setForm((f) => (f.existing.length ? f : { ...f, existing: rowsFromStall(stallQuery.data) }));
    }
  }, [loaded, stallQuery.data, form.existing.length]);

  // Rascunho a cada alteração (RF-18).
  useEffect(() => {
    if (!loaded) return;
    const timer = setTimeout(
      () => void saveDraft({ taskId: task.publicId, form, photos, updatedAt: '' }),
      400,
    );
    return () => clearTimeout(timer);
  }, [form, photos, loaded, task.publicId]);

  const patch = (p: Partial<FormState>) => setForm((f) => ({ ...f, ...p }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const found = validateForm(task, form, photos);
    setErrors(found);
    if (found.length) {
      document.getElementById('submission-errors')?.focus();
      return;
    }
    setSending(true);
    try {
      await enqueue(task.publicId, buildChangeSet(task, form), photos);
      await clearDraft(task.publicId);
      toast.success(navigator.onLine ? t('submit.sending') : t('submit.queued'));
    } finally {
      setSending(false);
    }
  }

  if (!loaded || (hasStall && stallQuery.isPending)) return <LoadingRows rows={4} />;

  return (
    <form onSubmit={(e) => void onSubmit(e)} className="grid gap-8" noValidate>
      {task.type === 'NEW_STALL' && <NewStallFields task={task} form={form} patch={patch} />}
      {task.type === 'VERIFY_REPORT' && <VerifyFields form={form} patch={patch} />}

      {form.existing.length > 0 && (
        <ExistingOfferings rows={form.existing} onChange={(existing) => patch({ existing })} />
      )}
      <AddedOfferings rows={form.added} onChange={(added) => patch({ added })} />

      <PhotoPicker task={task} photos={photos} onChange={setPhotos} />

      <TextareaField
        label={t('form.notes')}
        hint={t('form.notesHint')}
        maxLength={500}
        value={form.notes}
        onChange={(e) => patch({ notes: e.target.value })}
      />

      {errors.length > 0 && (
        <div
          id="submission-errors"
          tabIndex={-1}
          role="alert"
          className="grid gap-1 rounded-[var(--radius-control)] border-2 border-danger p-3"
        >
          <p className="font-semibold text-danger">{t('errors.title')}</p>
          <ul className="list-disc pl-5 text-ink">
            {errors.map((key) => (
              <li key={key}>{t(key)}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid gap-2">
        <Button type="submit" size="lg" block busy={sending}>
          {t('submit.action')}
        </Button>
        <p className="text-center text-sm text-support">{t('submit.hint')}</p>
      </div>
    </form>
  );
}

function NewStallFields({
  task,
  form,
  patch,
}: {
  task: TaskDetail;
  form: FormState;
  patch: (p: Partial<FormState>) => void;
}) {
  const { t } = useTranslation('collab');
  const market = useMarket(task.market.slug);
  return (
    <fieldset className="grid gap-4">
      <legend className="mb-2 border-b-2 border-ink pb-1 text-xl font-bold">{t('form.stallTitle')}</legend>
      <TextField
        label={t('form.code')}
        hint={t('form.codeHint')}
        value={form.code}
        maxLength={20}
        autoCapitalize="characters"
        onChange={(e) => patch({ code: e.target.value })}
      />
      <TextField
        label={t('form.displayName')}
        hint={t('form.optional')}
        value={form.displayName}
        maxLength={80}
        onChange={(e) => patch({ displayName: e.target.value })}
      />
      <SelectField
        label={t('form.sector')}
        value={form.sectorSlug || task.suggestedSectorSlug || ''}
        onChange={(e) => patch({ sectorSlug: e.target.value })}
      >
        <option value="">{t('form.sectorUnknown')}</option>
        {market.data?.sectors.map((s) => (
          <option key={s.slug} value={s.slug}>
            {s.name}
          </option>
        ))}
      </SelectField>
      <TextField
        label={t('form.vendorName')}
        hint={t('form.vendorNameHint')}
        value={form.vendorName}
        maxLength={80}
        onChange={(e) => patch({ vendorName: e.target.value })}
      />
      <div className="grid gap-2 rounded-[var(--radius-panel)] bg-soft p-4">
        <p className="font-semibold text-ink">{t('consent.title')}</p>
        <p className="text-sm text-support">{t('consent.body')}</p>
        <CheckboxField
          label={t('consent.publish')}
          checked={form.consentPublish}
          onChange={(e) => patch({ consentPublish: e.target.checked })}
        />
        <CheckboxField
          label={t('consent.showName')}
          checked={form.consentShowName}
          disabled={!form.vendorName.trim()}
          onChange={(e) => patch({ consentShowName: e.target.checked })}
        />
        <SelectField
          label={t('consent.method')}
          value={form.consentMethod}
          onChange={(e) => patch({ consentMethod: e.target.value as FormState['consentMethod'] })}
        >
          <option value="VERBAL">{t('consent.VERBAL')}</option>
          <option value="SIGNED_FORM">{t('consent.SIGNED_FORM')}</option>
        </SelectField>
      </div>
    </fieldset>
  );
}

function VerifyFields({ form, patch }: { form: FormState; patch: (p: Partial<FormState>) => void }) {
  const { t } = useTranslation('collab');
  return (
    <fieldset className="grid gap-3">
      <legend className="mb-2 border-b-2 border-ink pb-1 text-xl font-bold">{t('verify.title')}</legend>
      <p className="text-support">{t('verify.body')}</p>
      <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label={t('verify.title')}>
        {(['yes', 'no'] as const).map((v) => (
          <label
            key={v}
            className={cn(
              'flex min-h-12 cursor-pointer items-center justify-center gap-2 rounded-[var(--radius-control)] border px-3 font-semibold',
              form.problemConfirmed === v
                ? 'border-2 border-brand bg-brand-soft text-brand'
                : 'border-line-strong text-ink',
            )}
          >
            <input
              type="radio"
              name="problemConfirmed"
              className="sr-only"
              checked={form.problemConfirmed === v}
              onChange={() => patch({ problemConfirmed: v })}
            />
            {t(`verify.${v}`)}
          </label>
        ))}
      </div>
      <TextareaField
        label={t('verify.comment')}
        maxLength={500}
        value={form.comment}
        onChange={(e) => patch({ comment: e.target.value })}
      />
    </fieldset>
  );
}

function ExistingOfferings({
  rows,
  onChange,
}: {
  rows: ExistingRow[];
  onChange: (rows: ExistingRow[]) => void;
}) {
  const { t } = useTranslation('collab');
  const update = (i: number, p: Partial<ExistingRow>) =>
    onChange(rows.map((r, j) => (j === i ? { ...r, ...p } : r)));
  const confirmAll = () => onChange(rows.map((r) => (r.action === 'KEEP' ? { ...r, action: 'CONFIRM' } : r)));
  return (
    <fieldset className="grid gap-2">
      <legend className="mb-2 flex w-full items-baseline justify-between border-b-2 border-ink pb-1 text-xl font-bold">
        {t('offerings.current')}
      </legend>
      <p className="text-sm text-support">{t('offerings.currentHint')}</p>
      <Button variant="secondary" size="sm" className="justify-self-start" onClick={confirmAll}>
        {t('offerings.confirmAll')}
      </Button>
      <ul className="divide-y divide-line">
        {rows.map((row, i) => (
          <li key={row.productSlug} className="grid gap-2 py-3">
            <p className="font-semibold text-ink">{row.productName}</p>
            <div
              className="grid grid-cols-2 gap-1.5 sm:grid-cols-4"
              role="radiogroup"
              aria-label={row.productName}
            >
              {(['CONFIRM', 'UPDATE', 'REMOVE', 'KEEP'] as const).map((action) => (
                <label
                  key={action}
                  className={cn(
                    'flex min-h-11 cursor-pointer items-center justify-center rounded-[var(--radius-control)] border px-2 text-sm font-semibold',
                    row.action === action
                      ? 'border-2 border-brand bg-brand-soft text-brand'
                      : 'border-line text-support',
                  )}
                >
                  <input
                    type="radio"
                    className="sr-only"
                    name={`action-${row.productSlug}`}
                    checked={row.action === action}
                    onChange={() => update(i, { action })}
                  />
                  {t(`offerings.action.${action}`)}
                </label>
              ))}
            </div>
            {row.action === 'UPDATE' && <PriceFields value={row} onChange={(p) => update(i, p)} />}
          </li>
        ))}
      </ul>
    </fieldset>
  );
}

function PriceFields({
  value,
  onChange,
}: {
  value: { price: string; unit: PriceUnit; availability: Availability };
  onChange: (p: Partial<{ price: string; unit: PriceUnit; availability: Availability }>) => void;
}) {
  const { t, i18n } = useTranslation('collab');
  const parsed = parsePrice(value.price);
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <TextField
        label={t('offerings.price')}
        hint={
          parsed ? formatMoney({ amount: parsed, currency: 'BRL' }, i18n.language) : t('offerings.priceHint')
        }
        error={parsed === undefined ? t('errors.priceInvalid') : null}
        inputMode="decimal"
        value={value.price}
        onChange={(e) => onChange({ price: e.target.value })}
      />
      <SelectField
        label={t('offerings.unit')}
        value={value.unit}
        onChange={(e) => onChange({ unit: e.target.value as PriceUnit })}
      >
        {UNITS.map((u) => (
          <option key={u} value={u}>
            {t(`common:unitShort.${u}`)}
          </option>
        ))}
      </SelectField>
      <SelectField
        label={t('offerings.availability')}
        value={value.availability}
        onChange={(e) => onChange({ availability: e.target.value as Availability })}
      >
        {AVAILABILITY.map((a) => (
          <option key={a} value={a}>
            {t(`common:availability.${a}`)}
          </option>
        ))}
      </SelectField>
    </div>
  );
}

function AddedOfferings({ rows, onChange }: { rows: NewRow[]; onChange: (rows: NewRow[]) => void }) {
  const { t } = useTranslation('collab');
  const update = (i: number, p: Partial<NewRow>) =>
    onChange(rows.map((r, j) => (j === i ? { ...r, ...p } : r)));
  const add = () =>
    onChange([
      ...rows,
      {
        key: newId(),
        productSlug: null,
        productName: '',
        suggestion: null,
        price: '',
        unit: 'KG',
        availability: 'AVAILABLE',
      },
    ]);
  return (
    <fieldset className="grid gap-2">
      <legend className="mb-2 w-full border-b-2 border-ink pb-1 text-xl font-bold">
        {t('offerings.new')}
      </legend>
      <ul className="grid gap-4">
        {rows.map((row, i) => (
          <li key={row.key} className="grid gap-3 rounded-[var(--radius-panel)] border border-line p-3">
            <div className="flex items-start justify-between gap-2">
              <ProductPicker row={row} onChange={(p) => update(i, p)} />
              <button
                type="button"
                className="inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-support hover:bg-soft"
                aria-label={t('offerings.removeRow')}
                onClick={() => onChange(rows.filter((_, j) => j !== i))}
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>
            <PriceFields value={row} onChange={(p) => update(i, p)} />
          </li>
        ))}
      </ul>
      <Button
        variant="secondary"
        className="justify-self-start"
        icon={<Plus size={18} aria-hidden="true" />}
        onClick={add}
      >
        {t('offerings.add')}
      </Button>
    </fieldset>
  );
}

/** Escolha do produto pelo catálogo; se não existir, vira sugestão para a moderação (RF-31). */
function ProductPicker({ row, onChange }: { row: NewRow; onChange: (p: Partial<NewRow>) => void }) {
  const { t } = useTranslation('collab');
  const [text, setText] = useState(row.productName);
  const debounced = useDebounced(text);
  const suggestions = useSuggestions(row.productSlug || row.suggestion ? '' : debounced);
  const categories = useCategories();

  if (row.productSlug) {
    return (
      <p className="min-h-11 flex-1 content-center font-semibold text-ink">
        {row.productName}{' '}
        <button
          type="button"
          className="ml-2 text-sm font-semibold text-brand underline"
          onClick={() => onChange({ productSlug: null })}
        >
          {t('offerings.change')}
        </button>
      </p>
    );
  }
  if (row.suggestion) {
    return (
      <div className="grid flex-1 gap-3">
        <TextField
          label={t('offerings.suggestName')}
          value={row.suggestion.namePt}
          maxLength={80}
          onChange={(e) => onChange({ suggestion: { ...row.suggestion!, namePt: e.target.value } })}
        />
        <SelectField
          label={t('offerings.suggestCategory')}
          value={row.suggestion.categorySlug}
          onChange={(e) => onChange({ suggestion: { ...row.suggestion!, categorySlug: e.target.value } })}
        >
          {categories.data?.map((c) => (
            <option key={c.slug} value={c.slug}>
              {c.name}
            </option>
          ))}
        </SelectField>
        <button
          type="button"
          className="justify-self-start text-sm font-semibold text-brand underline"
          onClick={() => onChange({ suggestion: null })}
        >
          {t('offerings.backToCatalog')}
        </button>
      </div>
    );
  }
  return (
    <div className="grid flex-1 gap-1.5">
      <TextField
        label={t('offerings.product')}
        value={text}
        autoComplete="off"
        onChange={(e) => setText(e.target.value)}
      />
      {suggestions.data && suggestions.data.length > 0 && (
        <ul className="divide-y divide-line rounded-[var(--radius-control)] border border-line">
          {suggestions.data.slice(0, 6).map((s) => (
            <li key={s.slug}>
              <button
                type="button"
                className="min-h-11 w-full px-3 text-left hover:bg-soft"
                onClick={() => onChange({ productSlug: s.slug, productName: s.name })}
              >
                {s.name}
              </button>
            </li>
          ))}
        </ul>
      )}
      {text.trim().length >= 2 && (
        <button
          type="button"
          className="justify-self-start py-2 text-sm font-semibold text-brand underline"
          onClick={() =>
            onChange({ suggestion: { namePt: text.trim(), categorySlug: categories.data?.[0]?.slug ?? '' } })
          }
        >
          {t('offerings.suggest', { name: text.trim() })}
        </button>
      )}
    </div>
  );
}

/**
 * Fotos (RN-06, RN-27): comprimidas no aparelho (lado maior 1600 px, ~300 KB) e enviadas assim que tiradas,
 * cada uma com sua chave. Sem sinal, ficam "aguardando sinal" e seguem pela fila.
 */
function PhotoPicker({
  task,
  photos,
  onChange,
}: {
  task: TaskDetail;
  photos: QueuedPhoto[];
  onChange: (p: QueuedPhoto[]) => void;
}) {
  const { t } = useTranslation('collab');
  const input = useRef<HTMLInputElement>(null);
  const latest = useRef(photos);
  useEffect(() => {
    latest.current = photos;
  }, [photos]);
  const [busy, setBusy] = useState(false);
  const defaultRole: QueuedPhoto['role'] =
    task.type === 'NEW_STALL' && !photos.some((p) => p.role === 'STALL')
      ? 'STALL'
      : task.type === 'VERIFY_REPORT'
        ? 'EVIDENCE'
        : 'STALL';

  const replace = (id: string, next: QueuedPhoto) =>
    onChange(latest.current.map((p) => (p.clientUploadId === id ? next : p)));

  async function onFiles(files: FileList | null) {
    if (!files?.length) return;
    setBusy(true);
    try {
      const room = MAX_PHOTOS - latest.current.length;
      for (const file of Array.from(files).slice(0, room)) {
        let blob: Blob;
        try {
          blob = await compressImage(file);
        } catch {
          toast.error(t('photo.unreadable'));
          continue;
        }
        const photo: QueuedPhoto = { clientUploadId: newId(), blob, role: defaultRole, state: 'uploading' };
        onChange([...latest.current, photo]);
        latest.current = [...latest.current, photo];
        void uploadPhoto(photo).then((done) => replace(photo.clientUploadId, done));
      }
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  }

  return (
    <fieldset className="grid gap-3">
      <legend className="mb-2 w-full border-b-2 border-ink pb-1 text-xl font-bold">{t('photo.title')}</legend>
      <p className="text-sm text-support">
        {task.type === 'NEW_STALL' ? t('photo.hintNew') : t('photo.hint')}
      </p>
      {photos.length > 0 && (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {photos.map((p, i) => (
            <PhotoTile
              key={p.clientUploadId}
              photo={p}
              index={i}
              onRole={(role) => replace(p.clientUploadId, { ...p, role })}
              onRemove={() => onChange(photos.filter((x) => x.clientUploadId !== p.clientUploadId))}
            />
          ))}
        </ul>
      )}
      <input
        ref={input}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        capture="environment"
        multiple
        className="sr-only"
        // O botão "Tirar foto" abaixo abre este campo; ele fica fora da ordem de tabulação.
        tabIndex={-1}
        aria-label={t('photo.title')}
        id="photo-input"
        onChange={(e) => void onFiles(e.target.files)}
      />
      <Button
        variant="secondary"
        size="lg"
        block
        busy={busy}
        disabled={photos.length >= MAX_PHOTOS}
        icon={<Camera size={20} aria-hidden="true" />}
        onClick={() => input.current?.click()}
      >
        {photos.length >= MAX_PHOTOS
          ? t('photo.max', { max: MAX_PHOTOS })
          : t('photo.take', { count: photos.length, max: MAX_PHOTOS })}
      </Button>
    </fieldset>
  );
}

function PhotoTile({
  photo,
  index,
  onRole,
  onRemove,
}: {
  photo: QueuedPhoto;
  index: number;
  onRole: (r: QueuedPhoto['role']) => void;
  onRemove: () => void;
}) {
  const { t } = useTranslation('collab');
  const url = useMemo(() => URL.createObjectURL(photo.blob), [photo.blob]);
  useEffect(() => () => URL.revokeObjectURL(url), [url]);
  return (
    <li className="grid gap-1.5">
      <div className="relative overflow-hidden rounded-[var(--radius-control)] bg-soft">
        <img
          src={url}
          alt={t('photo.label', { n: index + 1 })}
          className="aspect-[4/3] w-full object-cover"
        />
        <button
          type="button"
          className="absolute top-1 right-1 inline-flex size-11 items-center justify-center rounded-full bg-surface/90 text-danger"
          aria-label={t('photo.remove', { n: index + 1 })}
          onClick={onRemove}
        >
          <Trash2 size={18} aria-hidden="true" />
        </button>
      </div>
      <p
        className={cn('text-sm', photo.state === 'failed' ? 'font-semibold text-danger' : 'text-support')}
        aria-live="polite"
      >
        {t(`photo.state.${photo.state}`)}
      </p>
      <select
        aria-label={t('photo.role', { n: index + 1 })}
        value={photo.role}
        onChange={(e) => onRole(e.target.value as QueuedPhoto['role'])}
        className="min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-2 text-base"
      >
        {(['STALL', 'OFFERING', 'EVIDENCE'] as const).map((r) => (
          <option key={r} value={r}>
            {t(`photo.roles.${r}`)}
          </option>
        ))}
      </select>
    </li>
  );
}
