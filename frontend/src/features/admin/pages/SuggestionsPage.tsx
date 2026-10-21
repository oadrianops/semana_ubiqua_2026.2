import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page } from '../../../shared/api/types';
import { Button } from '../../../shared/ui/Button';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useDebounced, useSuggestions } from '../../public-api';
import type { AdminProduct } from '../types';
import { AdminHeader, errorMessage, Pager } from '../ui';

/**
 * Sugestões de produto (RF-31): aprovar entra no catálogo (as traduções que faltarem ficam marcadas como
 * incompletas); mesclar aponta a sugestão para um produto que já existe, sem perder as ofertas.
 */
export default function SuggestionsPage() {
  const { t } = useTranslation('admin');
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ['admin', 'suggestions', page],
    queryFn: () => api.get<Page<AdminProduct>>(`/moderation/product-suggestions?page=${page}&size=20`),
    networkMode: 'online',
  });
  return (
    <div className="grid gap-6">
      <AdminHeader title={t('suggestions.title')} lead={t('suggestions.lead')} />
      {query.isPending && <LoadingRows rows={4} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.items.length === 0 && (
        <EmptyState title={t('suggestions.emptyTitle')} body={t('suggestions.emptyBody')} />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {query.data?.items.map((p) => (
          <SuggestionRow key={p.slug} product={p} />
        ))}
      </ul>
      {query.data && <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={setPage} />}
    </div>
  );
}

function SuggestionRow({ product }: { product: AdminProduct }) {
  const { t } = useTranslation('admin');
  const client = useQueryClient();
  const [target, setTarget] = useState('');
  const [chosen, setChosen] = useState<{ slug: string; name: string } | null>(null);
  const options = useSuggestions(useDebounced(chosen ? '' : target));
  const refresh = () => void client.invalidateQueries({ queryKey: ['admin', 'suggestions'] });
  const approve = useMutation({
    mutationFn: () =>
      api.post(`/moderation/product-suggestions/${product.slug}/approve`, undefined, { retries: 0 }),
    onSuccess: () => (toast.success(t('suggestions.approved')), refresh()),
    onError: (e) => toast.error(errorMessage(e, t('suggestions.error'))),
  });
  const merge = useMutation({
    mutationFn: () =>
      api.post(
        `/moderation/product-suggestions/${product.slug}/merge`,
        { targetProductSlug: chosen?.slug },
        { retries: 0 },
      ),
    onSuccess: () => (toast.success(t('suggestions.merged')), refresh()),
    onError: (e) => toast.error(errorMessage(e, t('suggestions.error'))),
  });
  const name = product.translations['pt-BR']?.name ?? product.slug;
  return (
    <li className="grid gap-3 py-4">
      <div className="grid gap-0.5">
        <p className="text-lg font-semibold">{name}</p>
        <p className="text-sm text-support">
          {t('suggestions.category', { category: product.categorySlug })}
          {product.suggestedBy && ` · ${t('suggestions.by', { name: product.suggestedBy })}`}
        </p>
      </div>
      <div className="flex flex-wrap items-end gap-3">
        <Button busy={approve.isPending} onClick={() => approve.mutate()}>
          {t('suggestions.approve')}
        </Button>
        <div className="grid min-w-[16rem] flex-1 gap-1">
          <label className="text-sm font-semibold text-support" htmlFor={`merge-${product.slug}`}>
            {t('suggestions.mergeInto')}
          </label>
          {chosen ? (
            <p className="flex min-h-11 items-center gap-2">
              <strong>{chosen.name}</strong>
              <button
                type="button"
                className="text-sm font-semibold text-brand underline"
                onClick={() => setChosen(null)}
              >
                {t('suggestions.change')}
              </button>
            </p>
          ) : (
            <input
              id={`merge-${product.slug}`}
              className="min-h-11 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3 text-base"
              value={target}
              autoComplete="off"
              onChange={(e) => setTarget(e.target.value)}
            />
          )}
          {!chosen && options.data && options.data.length > 0 && (
            <ul className="divide-y divide-line rounded-[var(--radius-control)] border border-line">
              {options.data
                .filter((o) => o.slug !== product.slug)
                .slice(0, 5)
                .map((o) => (
                  <li key={o.slug}>
                    <button
                      type="button"
                      className="min-h-11 w-full px-3 text-left hover:bg-soft"
                      onClick={() => setChosen({ slug: o.slug, name: o.name })}
                    >
                      {o.name}
                    </button>
                  </li>
                ))}
            </ul>
          )}
        </div>
        <Button variant="secondary" disabled={!chosen} busy={merge.isPending} onClick={() => merge.mutate()}>
          {t('suggestions.merge')}
        </Button>
      </div>
    </li>
  );
}
