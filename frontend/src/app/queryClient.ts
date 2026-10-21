import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister';
import { QueryClient, type Query } from '@tanstack/react-query';
import { ApiError } from '../shared/api/http';
import { idb } from '../shared/offline/idb';

/**
 * Estado de servidor (9.4, 9.13): `networkMode: offlineFirst`, staleTime de 5 min e gcTime de 7 dias. Só as
 * consultas públicas (chave começando em "public") vão para o IndexedDB; dados pessoais ficam só em memória.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      networkMode: 'offlineFirst',
      staleTime: 5 * 60_000,
      gcTime: 7 * 24 * 60 * 60_000,
      refetchOnWindowFocus: false,
      retry: (count, error) => {
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false;
        return count < 1; // o cliente HTTP já faz até 3 tentativas com espera exponencial
      },
    },
    mutations: { networkMode: 'offlineFirst', retry: 0 },
  },
});

export const persister = createAsyncStoragePersister({
  storage: {
    getItem: (key) => idb.get<string>('query', key).then((v) => v ?? null),
    setItem: (key, value) => idb.set('query', key, value),
    removeItem: (key) => idb.del('query', key),
  },
  key: 'nd-query-cache',
  throttleTime: 1500,
});

export function shouldPersist(query: Query): boolean {
  return query.queryKey[0] === 'public' && query.state.status === 'success';
}
