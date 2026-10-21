import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page, PriceUnit } from '../../../shared/api/types';
import { LANGUAGE_NAMES, LANGUAGES, type Language } from '../../../shared/i18n/config';
import { Button } from '../../../shared/ui/Button';
import { SelectField, TextareaField, TextField } from '../../../shared/ui/Field';
import { Sheet } from '../../../shared/ui/Sheet';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useDebounced } from '../../public-api';
import type { AdminCategory, AdminProduct, ProductStatus } from '../types';
import { AdminHeader, errorMessage, Pager, Pill, selectClass } from '../ui';

const UNITS: PriceUnit[] = ['KG', 'UNIT', 'LITER', 'BUNCH', 'PORTION', 'PACKAGE', 'BOTTLE', 'DOZEN'];
const STATUSES: ProductStatus[] = ['ACTIVE', 'PENDING', 'INACTIVE', 'MERGED'];

/**
 * Catálogo (RF-31): produtos com nome, descrição e sinônimos nos 3 idiomas (os sinônimos alimentam a busca —
 * RF-02). A coluna de completude mostra o que falta traduzir.
 */
export default function CatalogAdminPage() {
  const { t } = useTranslation('admin');
  const [status, setStatus] = useState<ProductStatus>('ACTIVE');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<AdminProduct | null>(null);
  const debounced = useDebounced(q);
  const categories = useQuery({
    queryKey: ['admin', 'categories'],
    queryFn: () => api.get<AdminCategory[]>('/admin/categories'),
    networkMode: 'online',
  });
  const params = new URLSearchParams({ status, page: String(page), size: '50' });
  if (debounced.trim()) params.set('q', debounced.trim());
  const list = useQuery({
    queryKey: ['admin', 'products', status, debounced, page],
    queryFn: () => api.get<Page<AdminProduct>>(`/admin/products?${params.toString()}`),
    networkMode: 'online',
  });
  const categoryName = (slug: string) =>
    categories.data?.find((c) => c.slug === slug)?.names['pt-BR'] ?? slug;

  return (
    <div className="grid gap-6">
      <AdminHeader title={t('catalog.title')} lead={t('catalog.lead')} />
      <div className="flex flex-wrap items-end gap-3">
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('filters.status')}</span>
          <select
            className={selectClass}
            value={status}
            onChange={(e) => (setStatus(e.target.value as ProductStatus), setPage(0))}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {t(`catalog.status.${s}`)}
              </option>
            ))}
          </select>
        </label>
        <TextField
          label={t('catalog.search')}
          value={q}
          className="min-w-[14rem]"
          onChange={(e) => (setQ(e.target.value), setPage(0))}
        />
      </div>
      {list.isPending && <LoadingRows rows={8} />}
      {list.isError && <ErrorState error={list.error} onRetry={() => void list.refetch()} />}
      {list.data?.items.length === 0 && (
        <EmptyState title={t('catalog.emptyTitle')} body={t('catalog.emptyBody')} />
      )}
      {list.data && list.data.items.length > 0 && (
        <div className="overflow-x-auto" role="region" aria-label={t('catalog.title')} tabIndex={0}>
          <table className="w-full min-w-[36rem] border-collapse text-left">
            <thead>
              <tr className="border-b-2 border-ink text-sm text-support">
                <th scope="col" className="py-2 pr-3 font-semibold">
                  {t('catalog.product')}
                </th>
                <th scope="col" className="py-2 pr-3 font-semibold">
                  {t('catalog.category')}
                </th>
                <th scope="col" className="py-2 font-semibold">
                  {t('catalog.completeness')}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {list.data.items.map((p) => (
                <tr key={p.slug}>
                  <th scope="row" className="py-2 pr-3">
                    <button
                      type="button"
                      className="min-h-11 text-left font-semibold text-brand underline"
                      onClick={() => setEditing(p)}
                    >
                      {p.translations['pt-BR']?.name ?? p.slug}
                    </button>
                  </th>
                  <td className="py-2 pr-3 text-support">{categoryName(p.categorySlug)}</td>
                  <td className="py-2">
                    <span className="flex flex-wrap gap-1">
                      {LANGUAGES.map((l) => (
                        <Pill key={l} tone={p.completeness[l] ? 'good' : 'warn'}>
                          {l} {p.completeness[l] ? t('catalog.ok') : t('catalog.missing')}
                        </Pill>
                      ))}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {list.data && <Pager page={list.data.page} totalPages={list.data.totalPages} onPage={setPage} />}
      {editing && (
        <ProductSheet product={editing} categories={categories.data ?? []} onClose={() => setEditing(null)} />
      )}
    </div>
  );
}

function ProductSheet({
  product,
  categories,
  onClose,
}: {
  product: AdminProduct;
  categories: AdminCategory[];
  onClose: () => void;
}) {
  const { t } = useTranslation('admin');
  const client = useQueryClient();
  const [categorySlug, setCategorySlug] = useState(product.categorySlug);
  const [unit, setUnit] = useState<PriceUnit>(product.defaultUnit);
  const [status, setStatus] = useState<ProductStatus>(product.status);
  const [names, setNames] = useState<Record<Language, { name: string; description: string }>>(
    () =>
      Object.fromEntries(
        LANGUAGES.map((l) => [
          l,
          {
            name: product.translations[l]?.name ?? '',
            description: product.translations[l]?.description ?? '',
          },
        ]),
      ) as Record<Language, { name: string; description: string }>,
  );
  const [aliases, setAliases] = useState<Record<Language, string>>(
    () =>
      Object.fromEntries(LANGUAGES.map((l) => [l, (product.aliases[l] ?? []).join(', ')])) as Record<
        Language,
        string
      >,
  );
  const save = useMutation({
    mutationFn: () =>
      api.put(
        `/admin/products/${product.slug}`,
        {
          categorySlug,
          defaultUnit: unit,
          seasonMonths: product.seasonMonths,
          status,
          translations: Object.fromEntries(
            LANGUAGES.filter((l) => names[l].name.trim()).map((l) => [
              l,
              { name: names[l].name.trim(), description: names[l].description.trim() || null },
            ]),
          ),
          aliases: Object.fromEntries(
            LANGUAGES.map((l) => [
              l,
              aliases[l]
                .split(',')
                .map((a) => a.trim())
                .filter(Boolean),
            ]),
          ),
          expectedVersion: product.version,
        },
        { retries: 0 },
      ),
    onSuccess: () => {
      toast.success(t('catalog.saved'));
      void client.invalidateQueries({ queryKey: ['admin', 'products'] });
      void client.invalidateQueries({ queryKey: ['public'] });
      onClose();
    },
    onError: (e) => toast.error(errorMessage(e, t('catalog.error'))),
  });
  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate();
  };
  return (
    <Sheet
      open
      onOpenChange={(o) => !o && onClose()}
      title={names['pt-BR'].name || product.slug}
      description={t('catalog.editBody')}
    >
      <form onSubmit={submit} className="grid gap-4">
        <SelectField
          label={t('catalog.category')}
          value={categorySlug}
          onChange={(e) => setCategorySlug(e.target.value)}
        >
          {categories.map((c) => (
            <option key={c.slug} value={c.slug}>
              {c.names['pt-BR'] ?? c.slug}
            </option>
          ))}
        </SelectField>
        <div className="grid grid-cols-2 gap-3">
          <SelectField
            label={t('catalog.unit')}
            value={unit}
            onChange={(e) => setUnit(e.target.value as PriceUnit)}
          >
            {UNITS.map((u) => (
              <option key={u} value={u}>
                {t(`common:unitShort.${u}`)}
              </option>
            ))}
          </SelectField>
          <SelectField
            label={t('filters.status')}
            value={status}
            onChange={(e) => setStatus(e.target.value as ProductStatus)}
          >
            {STATUSES.filter((s) => s !== 'MERGED').map((s) => (
              <option key={s} value={s}>
                {t(`catalog.status.${s}`)}
              </option>
            ))}
          </SelectField>
        </div>
        {LANGUAGES.map((l) => (
          <fieldset key={l} className="grid gap-3 border-t border-line pt-3">
            <legend className="font-semibold text-ink">{LANGUAGE_NAMES[l]}</legend>
            <TextField
              label={t('catalog.name')}
              lang={l}
              value={names[l].name}
              maxLength={120}
              required={l === 'pt-BR'}
              onChange={(e) => setNames((n) => ({ ...n, [l]: { ...n[l], name: e.target.value } }))}
            />
            <TextareaField
              label={t('catalog.description')}
              lang={l}
              value={names[l].description}
              maxLength={600}
              onChange={(e) => setNames((n) => ({ ...n, [l]: { ...n[l], description: e.target.value } }))}
            />
            <TextField
              label={t('catalog.aliases')}
              hint={t('catalog.aliasesHint')}
              lang={l}
              value={aliases[l]}
              onChange={(e) => setAliases((a) => ({ ...a, [l]: e.target.value }))}
            />
          </fieldset>
        ))}
        <Button type="submit" block busy={save.isPending}>
          {t('catalog.save')}
        </Button>
      </form>
    </Sheet>
  );
}
