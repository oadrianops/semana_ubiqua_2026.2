import { appConfig } from '../config';
import type { MarketSnapshot } from '../api/types';
import { idb, requestPersistentStorage } from './idb';
import { isOnline } from './network';

/**
 * Snapshot da feira para uso sem conexão (RF-36, 9.13): guardado ao abrir a feira e para as "feiras salvas";
 * revalida com If-None-Match (ETag = dataVersion + idioma) e só baixa de novo quando os dados mudaram.
 */
export interface StoredSnapshot {
  etag: string | null;
  data: MarketSnapshot;
  /** Quando o aparelho confirmou os dados com o servidor pela última vez. */
  savedAt: string;
}

const SAVED_KEY = 'saved-markets';
type Listener = () => void;
const listeners = new Set<Listener>();

function key(slug: string, lng: string) {
  return `snapshot:${slug}:${lng}`;
}

function notify() {
  listeners.forEach((l) => l());
}

export function subscribeSnapshots(listener: Listener) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export async function readSnapshot(slug: string, lng: string): Promise<StoredSnapshot | undefined> {
  return idb.get<StoredSnapshot>('snapshot', key(slug, lng));
}

export async function allSnapshots(lng: string): Promise<StoredSnapshot[]> {
  const rows = await idb.entries<StoredSnapshot>('snapshot');
  return rows.filter(([k]) => String(k).endsWith(`:${lng}`)).map(([, v]) => v);
}

/** Baixa ou revalida o snapshot. Sem rede, devolve o salvo. */
export async function refreshSnapshot(slug: string, lng: string): Promise<StoredSnapshot | undefined> {
  const stored = await readSnapshot(slug, lng);
  if (!isOnline()) return stored;
  const headers: Record<string, string> = { Accept: 'application/json', 'Accept-Language': lng };
  if (stored?.etag) headers['If-None-Match'] = stored.etag;
  const base = appConfig().apiBaseUrl.replace(/\/$/, '');
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15_000);
  try {
    const response = await fetch(new URL(`${base}/v1/markets/${slug}/snapshot`, window.location.origin), {
      headers,
      signal: controller.signal,
    });
    const now = new Date().toISOString();
    if (response.status === 304 && stored) {
      const updated = { ...stored, savedAt: now };
      await idb.set('snapshot', key(slug, lng), updated);
      notify();
      return updated;
    }
    if (!response.ok) return stored;
    const data = (await response.json()) as MarketSnapshot;
    const fresh: StoredSnapshot = { etag: response.headers.get('ETag'), data, savedAt: now };
    await idb.set('snapshot', key(slug, lng), fresh);
    notify();
    return fresh;
  } catch {
    return stored;
  } finally {
    clearTimeout(timer);
  }
}

export async function savedMarkets(): Promise<string[]> {
  return (await idb.get<string[]>('prefs', SAVED_KEY)) ?? [];
}

/** "Salvar para usar sem internet" (RF-36): pede armazenamento persistente e baixa o snapshot. */
export async function saveMarket(slug: string, lng: string): Promise<void> {
  const current = await savedMarkets();
  if (!current.includes(slug)) {
    await idb.set('prefs', SAVED_KEY, [...current, slug]);
  }
  await requestPersistentStorage();
  await refreshSnapshot(slug, lng);
  notify();
}

export async function unsaveMarket(slug: string): Promise<void> {
  const current = await savedMarkets();
  await idb.set(
    'prefs',
    SAVED_KEY,
    current.filter((s) => s !== slug),
  );
  notify();
}

/** Atualiza em segundo plano as feiras salvas sempre que houver sinal. */
export async function refreshSavedMarkets(lng: string): Promise<void> {
  if (!isOnline()) return;
  for (const slug of await savedMarkets()) {
    await refreshSnapshot(slug, lng);
  }
}

/** RN-28: acima de 7 dias, os dados salvos podem estar desatualizados. */
export function isSnapshotStale(savedAt: string, now: Date = new Date(), days = 7): boolean {
  return now.getTime() - new Date(savedAt).getTime() > days * 86_400_000;
}
