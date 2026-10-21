import { api, ApiError } from '../api/http';
import type { ChangeSet, SubmissionReceipt, UploadResult } from '../api/types';
import { idb } from './idb';
import { isOnline } from './network';

/**
 * Fila de envios (9.13, RF-18, RN-27, RNF-03). Cada foto sobe sozinha, assim que tirada, com um clientUploadId
 * gerado no aparelho (Idempotency-Key). O envio passa por DRAFT → QUEUED → UPLOADING_PHOTOS → SUBMITTING → SENT,
 * com RETRY_WAIT (tenta de novo sozinho) e NEEDS_ACTION (a pessoa decide). Repetir nunca duplica.
 */

export type PhotoState = 'pending' | 'uploading' | 'uploaded' | 'waiting' | 'failed';

export interface QueuedPhoto {
  clientUploadId: string;
  blob: Blob;
  role: 'STALL' | 'OFFERING' | 'SUGGESTION' | 'EVIDENCE';
  productSlug?: string | null;
  fileId?: string;
  state: PhotoState;
  error?: string;
}

export type OutboxState =
  'DRAFT' | 'QUEUED' | 'UPLOADING_PHOTOS' | 'SUBMITTING' | 'SENT' | 'RETRY_WAIT' | 'NEEDS_ACTION';

export interface OutboxItem {
  taskId: string;
  idempotencyKey: string;
  changeSet: ChangeSet;
  photos: QueuedPhoto[];
  state: OutboxState;
  attempts: number;
  lastError?: string;
  updatedAt: string;
  receipt?: SubmissionReceipt;
}

type Listener = () => void;
const listeners = new Set<Listener>();
let processing: Promise<void> | null = null;

function emit() {
  listeners.forEach((l) => l());
}

export function subscribeOutbox(listener: Listener) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function newId(): string {
  return crypto.randomUUID();
}

const OUTBOX_PREFIX = 'outbox:';

async function save(item: OutboxItem) {
  await idb.set('outbox', OUTBOX_PREFIX + item.taskId, { ...item, updatedAt: new Date().toISOString() });
  emit();
}

export async function getItem(taskId: string): Promise<OutboxItem | undefined> {
  return idb.get<OutboxItem>('outbox', OUTBOX_PREFIX + taskId);
}

export async function listItems(): Promise<OutboxItem[]> {
  const rows = await idb.entries<OutboxItem>('outbox');
  return rows.map(([, v]) => v).sort((a, b) => a.updatedAt.localeCompare(b.updatedAt));
}

/** Itens aguardando sinal ("Na fila: 2 envios aguardando sinal"). */
export async function pendingCount(): Promise<number> {
  return (await listItems()).filter((i) => i.state !== 'SENT' && i.state !== 'DRAFT').length;
}

/** Sobe uma foto (RN-27). Reenviar a mesma chave devolve o mesmo arquivo; sem sinal, fica "aguardando sinal". */
export async function uploadPhoto(photo: QueuedPhoto): Promise<QueuedPhoto> {
  if (photo.fileId) return { ...photo, state: 'uploaded' };
  if (!isOnline()) return { ...photo, state: 'waiting' };
  const form = new FormData();
  form.append('file', photo.blob, `${photo.clientUploadId}.jpg`);
  try {
    const result = await api.post<UploadResult>('/uploads', undefined, {
      form,
      idempotencyKey: photo.clientUploadId,
      retries: 2,
    });
    return { ...photo, fileId: result.fileId, state: 'uploaded', error: undefined };
  } catch (error) {
    const e = error as ApiError;
    if (e.isNetwork || e.status >= 500 || e.status === 429) return { ...photo, state: 'waiting' };
    return { ...photo, state: 'failed', error: e.code };
  }
}

/** Coloca o envio na fila e já tenta mandar (o usuário pode fechar o app). */
export async function enqueue(
  taskId: string,
  changeSet: ChangeSet,
  photos: QueuedPhoto[],
  idempotencyKey = newId(),
): Promise<OutboxItem> {
  const existing = await getItem(taskId);
  const item: OutboxItem = {
    taskId,
    idempotencyKey: existing && existing.state !== 'SENT' ? existing.idempotencyKey : idempotencyKey,
    changeSet,
    photos,
    state: 'QUEUED',
    attempts: 0,
    updatedAt: new Date().toISOString(),
  };
  await save(item);
  void processOutbox();
  return item;
}

export async function removeItem(taskId: string) {
  await idb.del('outbox', OUTBOX_PREFIX + taskId);
  emit();
}

const ACTION_CODES = new Set([
  'RESERVATION_EXPIRED',
  'TASK_NOT_AVAILABLE',
  'CHECKIN_REQUIRED',
  'CONSENT_REQUIRED',
  'INVALID_PHOTO',
  'VALIDATION_ERROR',
  'DAILY_SUBMISSION_LIMIT_REACHED',
  'IDEMPOTENCY_KEY_REUSED',
  'FORBIDDEN',
]);

async function processItem(item: OutboxItem): Promise<OutboxItem> {
  let current: OutboxItem = { ...item, state: 'UPLOADING_PHOTOS' };
  await save(current);
  const photos: QueuedPhoto[] = [];
  for (const photo of current.photos) {
    // Uma a uma (RN-27): a queda de sinal só obriga a reenviar a foto que faltou.
    const uploaded = await uploadPhoto({ ...photo, state: photo.fileId ? 'uploaded' : 'uploading' });
    photos.push(uploaded);
    current = { ...current, photos: [...photos, ...current.photos.slice(photos.length)] };
    await save(current);
  }
  if (photos.some((p) => p.state === 'waiting')) {
    return { ...current, state: 'RETRY_WAIT', attempts: current.attempts + 1, lastError: 'NETWORK' };
  }
  if (photos.some((p) => p.state === 'failed')) {
    return {
      ...current,
      state: 'NEEDS_ACTION',
      lastError: photos.find((p) => p.error)?.error ?? 'INVALID_PHOTO',
    };
  }
  current = { ...current, state: 'SUBMITTING' };
  await save(current);
  const changeSet: ChangeSet = {
    ...current.changeSet,
    photos: photos.map((p) => ({
      fileId: p.fileId as string,
      role: p.role,
      productSlug: p.productSlug ?? null,
    })),
  };
  try {
    const receipt = await api.post<SubmissionReceipt>(`/tasks/${current.taskId}/submission`, changeSet, {
      idempotencyKey: current.idempotencyKey,
      retries: 2,
    });
    return { ...current, changeSet, state: 'SENT', receipt, lastError: undefined };
  } catch (error) {
    const e = error as ApiError;
    if (e.code === 'UPLOAD_NOT_AVAILABLE') {
      // Fotos TEMP vencidas (24 h): sobem de novo com chaves novas e o envio tenta outra vez.
      return {
        ...current,
        state: 'RETRY_WAIT',
        attempts: current.attempts + 1,
        lastError: e.code,
        idempotencyKey: newId(),
        photos: current.photos.map((p) => ({
          ...p,
          fileId: undefined,
          clientUploadId: newId(),
          state: 'pending',
        })),
      };
    }
    if (
      ACTION_CODES.has(e.code) ||
      (e.status >= 400 && e.status < 500 && e.status !== 429 && e.status !== 401)
    ) {
      return { ...current, state: 'NEEDS_ACTION', lastError: e.code };
    }
    return { ...current, state: 'RETRY_WAIT', attempts: current.attempts + 1, lastError: e.code };
  }
}

const ACTIONABLE = new Set<OutboxState>(['QUEUED', 'RETRY_WAIT', 'UPLOADING_PHOTOS', 'SUBMITTING']);

/** Faz a fila andar: ao voltar a conexão, ao abrir o app, a cada 30 s e por Background Sync. */
export function processOutbox(): Promise<void> {
  if (processing) return processing;
  const run = async () => {
    {
      // Cada item no máximo uma vez por rodada; itens que entram na fila durante a rodada também são vistos.
      const done = new Set<string>();
      for (;;) {
        if (!isOnline()) return;
        const next = (await listItems()).find((i) => ACTIONABLE.has(i.state) && !done.has(i.taskId));
        if (!next) return;
        done.add(next.taskId);
        try {
          await save(await processItem(next));
        } catch (error) {
          // Falha inesperada num item não trava a fila: ele volta a tentar sozinho no próximo gatilho.
          await save({
            ...next,
            state: 'RETRY_WAIT',
            attempts: next.attempts + 1,
            lastError: (error as Error)?.name ?? 'ERROR',
          });
        }
      }
    }
  };
  // A limpeza roda depois da atribuição, mesmo quando a rodada termina na hora (ex.: sem sinal).
  processing = run().finally(() => {
    processing = null;
  });
  return processing;
}

let started = false;

/** Liga os gatilhos da fila uma vez por sessão do app. */
export function startOutbox() {
  if (started || typeof window === 'undefined') return;
  started = true;
  window.addEventListener('online', () => void processOutbox());
  setInterval(() => void processOutbox(), 30_000);
  navigator.serviceWorker?.addEventListener('message', (event) => {
    if ((event.data as { type?: string })?.type === 'OUTBOX_SYNC') void processOutbox();
  });
  void processOutbox();
  void registerBackgroundSync();
}

async function registerBackgroundSync() {
  try {
    const registration = await navigator.serviceWorker?.ready;
    const sync = (
      registration as ServiceWorkerRegistration & { sync?: { register(tag: string): Promise<void> } }
    )?.sync;
    await sync?.register('nandesk-outbox');
  } catch {
    // Background Sync só existe no Chromium; os outros gatilhos continuam valendo.
  }
}

// ------------------------------------------------------------------ rascunhos

export interface Draft<T = unknown> {
  taskId: string;
  form: T;
  photos: QueuedPhoto[];
  updatedAt: string;
}

const DRAFT_PREFIX = 'draft:';

/** O rascunho (dados + fotos) fica salvo no aparelho a cada alteração (RF-18). */
export async function saveDraft<T>(draft: Draft<T>) {
  await idb.set('drafts', DRAFT_PREFIX + draft.taskId, { ...draft, updatedAt: new Date().toISOString() });
}

export async function loadDraft<T>(taskId: string): Promise<Draft<T> | undefined> {
  return idb.get<Draft<T>>('drafts', DRAFT_PREFIX + taskId);
}

export async function clearDraft(taskId: string) {
  await idb.del('drafts', DRAFT_PREFIX + taskId);
}
