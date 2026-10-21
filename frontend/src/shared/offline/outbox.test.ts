import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import type { ChangeSet } from '../api/types';
import {
  enqueue,
  getItem,
  listItems,
  loadDraft,
  newId,
  pendingCount,
  processOutbox,
  removeItem,
  saveDraft,
  uploadPhoto,
} from './outbox';

const server = setupServer();
const changeSet: ChangeSet = {
  taskType: 'UPDATE_STALL',
  stall: { publicId: 's1', baseVersion: 1 },
  offerings: [],
  photos: [],
};
let online = true;

beforeAll(() => {
  // O FormData do jsdom não aceita o Blob que volta do IndexedDB falso; no navegador real isso não acontece.
  globalThis.FormData = class extends URLSearchParams {
    override append(name: string) {
      super.append(name, 'blob');
    }
  } as unknown as typeof FormData;
  server.listen({ onUnhandledRequest: 'error' });
  Object.defineProperty(navigator, 'onLine', { configurable: true, get: () => online });
});
afterEach(async () => {
  server.resetHandlers();
  online = true;
  for (const item of await listItems()) await removeItem(item.taskId);
});
afterAll(() => server.close());

function photo() {
  return {
    clientUploadId: newId(),
    blob: new Blob(['jpeg'], { type: 'image/jpeg' }),
    role: 'STALL' as const,
    state: 'pending' as const,
  };
}

describe('RF-18 fila de envios (outbox)', () => {
  it('RN-27 sem sinal, a foto fica "aguardando sinal"', async () => {
    online = false;
    const result = await uploadPhoto(photo());
    expect(result.state).toBe('waiting');
  });

  it('RNF-03 sem sinal, o envio fica na fila e conta como pendente', async () => {
    online = false;
    await enqueue('t1', changeSet, [photo()]);
    await processOutbox();
    expect((await getItem('t1'))?.state).toBe('QUEUED');
    expect(await pendingCount()).toBe(1);
  });

  it('RN-27 RNF-03 a rede cai entre as fotos: as já enviadas não se repetem e o envio sai uma única vez', async () => {
    const uploadsByKey = new Map<string, number>();
    let failNext = true;
    let submissions = 0;
    const keys = new Set<string>();
    server.use(
      http.post('/api/v1/uploads', ({ request }) => {
        const key = request.headers.get('Idempotency-Key')!;
        uploadsByKey.set(key, (uploadsByKey.get(key) ?? 0) + 1);
        if (uploadsByKey.size === 2 && failNext) {
          failNext = false;
          return HttpResponse.error();
        }
        return HttpResponse.json(
          { fileId: 'f-' + key, status: 'TEMP', width: 1600, height: 1200, expiresAt: null },
          { status: 201 },
        );
      }),
      http.post('/api/v1/tasks/t2/submission', ({ request }) => {
        submissions++;
        keys.add(request.headers.get('Idempotency-Key')!);
        return HttpResponse.json(
          { submissionId: 'sub', taskId: 't2', status: 'PENDING', receivedAt: new Date().toISOString() },
          { status: 201 },
        );
      }),
    );
    const photos = [photo(), photo()];
    await enqueue('t2', changeSet, photos);
    await processOutbox();
    let item = await getItem('t2');
    if (item?.state !== 'SENT') {
      await processOutbox();
      item = await getItem('t2');
    }
    expect(item?.state).toBe('SENT');
    expect(submissions).toBe(1);
    expect(keys.size).toBe(1);
    // A primeira foto subiu uma vez só; a segunda tentou de novo com a mesma chave.
    expect(uploadsByKey.get(photos[0]!.clientUploadId)).toBe(1);
    expect(item?.changeSet.photos.map((p) => p.fileId)).toEqual(photos.map((p) => 'f-' + p.clientUploadId));
  });

  it('RF-19 reserva vencida pede ação da pessoa', async () => {
    server.use(
      http.post('/api/v1/uploads', () =>
        HttpResponse.json(
          { fileId: 'f1', status: 'TEMP', width: 1, height: 1, expiresAt: null },
          { status: 201 },
        ),
      ),
      http.post('/api/v1/tasks/t3/submission', () =>
        HttpResponse.json({ status: 409, code: 'RESERVATION_EXPIRED' }, { status: 409 }),
      ),
    );
    await enqueue('t3', changeSet, [photo()]);
    await processOutbox();
    const item = await getItem('t3');
    expect(item?.state).toBe('NEEDS_ACTION');
    expect(item?.lastError).toBe('RESERVATION_EXPIRED');
  });

  it('RN-27 foto temporária vencida sobe de novo com chave nova', async () => {
    server.use(
      http.post('/api/v1/uploads', () =>
        HttpResponse.json(
          { fileId: 'f1', status: 'TEMP', width: 1, height: 1, expiresAt: null },
          { status: 201 },
        ),
      ),
      http.post('/api/v1/tasks/t4/submission', () =>
        HttpResponse.json({ status: 422, code: 'UPLOAD_NOT_AVAILABLE' }, { status: 422 }),
      ),
    );
    const original = photo();
    await enqueue('t4', changeSet, [original]);
    await processOutbox();
    const item = await getItem('t4');
    expect(item?.state).toBe('RETRY_WAIT');
    expect(item?.photos[0]?.clientUploadId).not.toBe(original.clientUploadId);
    expect(item?.photos[0]?.fileId).toBeUndefined();
  });

  it('RF-18 o rascunho fica salvo no aparelho', async () => {
    await saveDraft({ taskId: 'd1', form: { price: '45.00' }, photos: [], updatedAt: '' });
    expect((await loadDraft<{ price: string }>('d1'))?.form.price).toBe('45.00');
  });
});
