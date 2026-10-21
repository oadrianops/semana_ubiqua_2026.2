import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import type { MarketSnapshot } from '../api/types';
import { idb } from './idb';
import { isSlow, prefersLightImages, reportSlowRequest } from './network';
import {
  allSnapshots,
  isSnapshotStale,
  readSnapshot,
  refreshSavedMarkets,
  refreshSnapshot,
  saveMarket,
  savedMarkets,
  subscribeSnapshots,
  unsaveMarket,
} from './snapshots';

const server = setupServer();
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(async () => {
  server.resetHandlers();
  Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
  for (const [k] of await idb.entries('snapshot')) await idb.del('snapshot', String(k));
  await idb.del('prefs', 'saved-markets');
});
afterAll(() => server.close());

const snapshot = (version: number) =>
  ({
    market: { slug: 'ver-o-peso', name: 'Ver-o-Peso' },
    dataVersion: version,
    locale: 'pt-BR',
    generatedAt: '2026-10-20T10:00:00Z',
    categories: [],
    products: [],
    stalls: [],
  }) as unknown as MarketSnapshot;

describe('RF-36 snapshot da feira', () => {
  it('RF-36 baixa, guarda com ETag e revalida com If-None-Match sem baixar de novo', async () => {
    let downloads = 0;
    server.use(
      http.get('/api/v1/markets/ver-o-peso/snapshot', ({ request }) => {
        expect(request.headers.get('Accept-Language')).toBe('pt-BR');
        if (request.headers.get('If-None-Match') === '"v7-pt-BR"')
          return new HttpResponse(null, { status: 304 });
        downloads++;
        return HttpResponse.json(snapshot(7), { headers: { ETag: '"v7-pt-BR"' } });
      }),
    );
    const notified = vi.fn();
    const unsubscribe = subscribeSnapshots(notified);
    const first = await refreshSnapshot('ver-o-peso', 'pt-BR');
    expect(first?.etag).toBe('"v7-pt-BR"');
    const second = await refreshSnapshot('ver-o-peso', 'pt-BR');
    expect(second?.data.dataVersion).toBe(7);
    expect(downloads).toBe(1);
    expect(notified).toHaveBeenCalledTimes(2);
    unsubscribe();
    expect(await allSnapshots('pt-BR')).toHaveLength(1);
    expect(await allSnapshots('en')).toHaveLength(0);
  });

  it('RNF-03 sem rede ou com erro do servidor, devolve o que estava salvo', async () => {
    server.use(
      http.get('/api/v1/markets/ver-o-peso/snapshot', () =>
        HttpResponse.json(snapshot(1), { headers: { ETag: '"v1"' } }),
      ),
    );
    await refreshSnapshot('ver-o-peso', 'pt-BR');
    server.use(
      http.get('/api/v1/markets/ver-o-peso/snapshot', () => new HttpResponse(null, { status: 503 })),
    );
    expect((await refreshSnapshot('ver-o-peso', 'pt-BR'))?.data.dataVersion).toBe(1);
    server.use(http.get('/api/v1/markets/ver-o-peso/snapshot', () => HttpResponse.error()));
    expect((await refreshSnapshot('ver-o-peso', 'pt-BR'))?.data.dataVersion).toBe(1);
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true });
    expect((await refreshSnapshot('ver-o-peso', 'pt-BR'))?.data.dataVersion).toBe(1);
  });

  it('RF-36 salvar a feira para usar sem internet e atualizar as salvas', async () => {
    let downloads = 0;
    server.use(
      http.get('/api/v1/markets/:slug/snapshot', () => {
        downloads++;
        return HttpResponse.json(snapshot(downloads));
      }),
    );
    await saveMarket('ver-o-peso', 'pt-BR');
    await saveMarket('ver-o-peso', 'pt-BR');
    expect(await savedMarkets()).toEqual(['ver-o-peso']);
    await refreshSavedMarkets('pt-BR');
    expect((await readSnapshot('ver-o-peso', 'pt-BR'))?.data.dataVersion).toBe(3);
    await unsaveMarket('ver-o-peso');
    expect(await savedMarkets()).toEqual([]);
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true });
    await refreshSavedMarkets('pt-BR');
    expect(downloads).toBe(3);
  });

  it('RN-28 acima de 7 dias os dados salvos podem estar desatualizados', () => {
    const now = new Date('2026-10-20T12:00:00Z');
    expect(isSnapshotStale('2026-10-14T12:00:00Z', now)).toBe(false);
    expect(isSnapshotStale('2026-10-12T12:00:00Z', now)).toBe(true);
  });
});

describe('9.14 conexão lenta', () => {
  it('9.14 depois de um tempo-limite estourado, a conexão conta como lenta por 60 s', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-10-20T12:00:00Z'));
    reportSlowRequest();
    expect(isSlow()).toBe(true);
    expect(prefersLightImages()).toBe(true);
    vi.setSystemTime(new Date('2026-10-20T12:01:01Z'));
    expect(isSlow()).toBe(false);
    vi.useRealTimers();
  });
});
