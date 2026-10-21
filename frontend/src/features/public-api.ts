import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../shared/api/http';
import type {
  CategoryProducts,
  CategoryView,
  MarketDetail,
  MarketSummary,
  Page,
  ProductPage,
  SearchResult,
  StallDetail,
  StallSummary,
  Suggestion,
} from '../shared/api/types';
import { toLanguage } from '../shared/i18n/config';
import {
  allSnapshots,
  readSnapshot,
  refreshSnapshot,
  subscribeSnapshots,
  type StoredSnapshot,
} from '../shared/offline/snapshots';

/**
 * Consultas públicas (11.2). As chaves começam em "public" e incluem o idioma: só elas vão para o IndexedDB e
 * abrem na hora na próxima visita (9.13).
 */
export function useLang() {
  const { i18n } = useTranslation();
  return toLanguage(i18n.language);
}

export function useMarkets() {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'markets', lng],
    queryFn: () => api.get<MarketSummary[]>('/markets'),
  });
}

export function useMarket(slug: string) {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'market', slug, lng],
    queryFn: () => api.get<MarketDetail>(`/markets/${slug}`),
  });
}

export function useMarketStalls(slug: string, sector: string | null, page: number) {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'market-stalls', slug, sector, page, lng],
    queryFn: () =>
      api.get<Page<StallSummary>>(
        `/markets/${slug}/stalls?page=${page}&size=30${sector ? `&sector=${encodeURIComponent(sector)}` : ''}`,
      ),
    placeholderData: keepPreviousData,
  });
}

export function useStall(publicId: string) {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'stall', publicId, lng],
    queryFn: () => api.get<StallDetail>(`/stalls/${publicId}`),
    enabled: Boolean(publicId),
  });
}

export function useCategories() {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'categories', lng],
    queryFn: () => api.get<CategoryView[]>('/categories'),
  });
}

export function useCategory(slug: string) {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'category', slug, lng],
    queryFn: () => api.get<CategoryProducts>(`/categories/${slug}/products`),
  });
}

export function useProduct(slug: string) {
  const lng = useLang();
  return useQuery({
    queryKey: ['public', 'product', slug, lng],
    queryFn: () => api.get<ProductPage>(`/products/${slug}`),
  });
}

export interface SearchFilters {
  q: string;
  market?: string | null;
  category?: string | null;
  availableOnly?: boolean;
}

export function useSearch(filters: SearchFilters) {
  const lng = useLang();
  const params = new URLSearchParams({ q: filters.q, size: '30' });
  if (filters.market) params.set('market', filters.market);
  if (filters.category) params.set('category', filters.category);
  if (filters.availableOnly) params.set('availableOnly', 'true');
  return useQuery({
    queryKey: [
      'public',
      'search',
      filters.q,
      filters.market ?? null,
      filters.category ?? null,
      Boolean(filters.availableOnly),
      lng,
    ],
    queryFn: () => api.get<SearchResult>(`/search?${params.toString()}`),
    enabled: filters.q.trim().length > 0,
    placeholderData: keepPreviousData,
    retry: 0,
  });
}

export function useSuggestions(text: string) {
  const lng = useLang();
  const q = text.trim();
  return useQuery({
    queryKey: ['public', 'suggestions', q.toLowerCase(), lng],
    queryFn: ({ signal }) =>
      api.get<Suggestion[]>(`/search/suggestions?q=${encodeURIComponent(q)}`, {
        signal,
        retries: 0,
        timeoutMs: 4000,
      }),
    enabled: q.length >= 2,
    staleTime: 60_000,
    retry: 0,
  });
}

/** Snapshot salvo de uma feira; ao abrir a feira, revalida em segundo plano (RF-36). */
export function useStoredSnapshot(slug: string | undefined, revalidate = true) {
  const lng = useLang();
  const [stored, setStored] = useState<StoredSnapshot | undefined>();
  useEffect(() => {
    if (!slug) return;
    let alive = true;
    const load = () => void readSnapshot(slug, lng).then((s) => alive && setStored(s));
    load();
    if (revalidate) void refreshSnapshot(slug, lng).then((s) => alive && s && setStored(s));
    const unsubscribe = subscribeSnapshots(load);
    return () => {
      alive = false;
      unsubscribe();
    };
  }, [slug, lng, revalidate]);
  return stored;
}

export function useAllSnapshots() {
  const lng = useLang();
  const [list, setList] = useState<StoredSnapshot[]>([]);
  useEffect(() => {
    let alive = true;
    const load = () => void allSnapshots(lng).then((s) => alive && setList(s));
    load();
    const unsubscribe = subscribeSnapshots(load);
    return () => {
      alive = false;
      unsubscribe();
    };
  }, [lng]);
  return list;
}

/** Atraso entre teclas da busca (250 ms — RF-02). */
export function useDebounced<T>(value: T, ms = 250): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);
  return debounced;
}
