import { createStore, del, entries, get, set, type UseStore } from 'idb-keyval';

/**
 * Armazenamento no aparelho (IndexedDB) separado por finalidade (9.13). O sistema pode apagar dados de sites
 * pouco usados, então tudo aqui é reconstruível quando houver sinal.
 */
let stores: Record<'query' | 'snapshot' | 'outbox' | 'drafts' | 'prefs', UseStore> | null = null;

function all() {
  if (!stores) {
    stores = {
      query: createStore('nandesk-query', 'kv'),
      snapshot: createStore('nandesk-snapshots', 'kv'),
      outbox: createStore('nandesk-outbox', 'kv'),
      drafts: createStore('nandesk-drafts', 'kv'),
      prefs: createStore('nandesk-prefs', 'kv'),
    };
  }
  return stores;
}

export type StoreName = 'query' | 'snapshot' | 'outbox' | 'drafts' | 'prefs';

export const idb = {
  get: <T>(store: StoreName, key: string) => get<T>(key, all()[store]),
  set: <T>(store: StoreName, key: string, value: T) => set(key, value, all()[store]),
  del: (store: StoreName, key: string) => del(key, all()[store]),
  entries: <T>(store: StoreName) => entries<string, T>(all()[store]),
};

/** Pede armazenamento persistente depois da instalação ou ao salvar uma feira (9.13). */
export async function requestPersistentStorage(): Promise<boolean> {
  try {
    if (navigator.storage?.persist) {
      return await navigator.storage.persist();
    }
  } catch {
    // Sem suporte: o app continua funcionando com cache que pode ser apagado.
  }
  return false;
}
