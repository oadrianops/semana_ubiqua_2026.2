import { useSyncExternalStore } from 'react';

/**
 * Estado da rede para a interface (9.13): "sem conexão" pelo navegador e "conexão lenta" quando uma requisição
 * passa do tempo-limite ou a Network Information API indica 2G/Save-Data (9.14).
 */
type Listener = () => void;

interface NetworkInfo {
  effectiveType?: string;
  saveData?: boolean;
  addEventListener?: (type: 'change', listener: () => void) => void;
}

const listeners = new Set<Listener>();
let slowUntil = 0;

function connection(): NetworkInfo | undefined {
  return (navigator as Navigator & { connection?: NetworkInfo }).connection;
}

function emit() {
  listeners.forEach((l) => l());
}

if (typeof window !== 'undefined') {
  window.addEventListener('online', emit);
  window.addEventListener('offline', emit);
  connection()?.addEventListener?.('change', emit);
}

function subscribe(listener: Listener) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function isOnline(): boolean {
  return typeof navigator === 'undefined' ? true : navigator.onLine;
}

/** Marca a conexão como lenta por 60 s (chamado quando uma requisição estoura o tempo-limite). */
export function reportSlowRequest() {
  slowUntil = Date.now() + 60_000;
  emit();
}

export function isSlow(): boolean {
  const c = connection();
  const effective = c?.effectiveType ?? '4g';
  return Date.now() < slowUntil || effective === 'slow-2g' || effective === '2g';
}

/** Com Save-Data ou conexão lenta, carregar só miniaturas (9.14). */
export function prefersLightImages(): boolean {
  return Boolean(connection()?.saveData) || isSlow();
}

export function connectionType(): string {
  return connection()?.effectiveType ?? 'unknown';
}

export type NetworkState = 'online' | 'slow' | 'offline';

function snapshot(): NetworkState {
  if (!isOnline()) return 'offline';
  return isSlow() ? 'slow' : 'online';
}

export function useNetworkState(): NetworkState {
  return useSyncExternalStore(subscribe, snapshot, () => 'online');
}
