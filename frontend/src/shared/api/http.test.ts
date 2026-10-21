import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { api, ApiError, backoff, refreshOnce, setRefreshHandler, tokenStore } from './http';

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  tokenStore.set(null);
});
afterAll(() => server.close());

describe('RF-09 cliente HTTP', () => {
  it('RF-09 faz uma única renovação simultânea em 401 e repete a requisição uma vez', async () => {
    let refreshCalls = 0;
    setRefreshHandler(async () => {
      refreshCalls++;
      await new Promise((r) => setTimeout(r, 20));
      tokenStore.set('novo');
      return 'novo';
    });
    tokenStore.set('velho');
    server.use(
      http.get('/api/v1/me', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer novo'
          ? HttpResponse.json({ name: 'Ana' })
          : new HttpResponse(null, { status: 401 }),
      ),
    );
    const [a, b, c] = await Promise.all([api.get('/me'), api.get('/me'), api.get('/me')]);
    expect([a, b, c]).toEqual([{ name: 'Ana' }, { name: 'Ana' }, { name: 'Ana' }]);
    expect(refreshCalls).toBe(1);
  });

  it('RF-06 envia o idioma e traduz Problem Details em ApiError', async () => {
    server.use(
      http.post('/api/v1/tasks/x/accept', () =>
        HttpResponse.json(
          {
            status: 409,
            code: 'TASK_ALREADY_TAKEN',
            detail: 'Esta tarefa acabou de ser aceita por outra pessoa.',
          },
          { status: 409 },
        ),
      ),
    );
    await expect(api.post('/tasks/x/accept')).rejects.toMatchObject({
      code: 'TASK_ALREADY_TAKEN',
      status: 409,
    });
  });

  it('RNF-03 tenta de novo só em operação idempotente', async () => {
    let calls = 0;
    server.use(
      http.get('/api/v1/markets', () => {
        calls++;
        return calls < 2 ? new HttpResponse(null, { status: 503 }) : HttpResponse.json([]);
      }),
    );
    await expect(api.get('/markets', { retries: 2 })).resolves.toEqual([]);
    expect(calls).toBe(2);
  });

  it('RNF-03 POST sem chave de idempotência não repete', async () => {
    let calls = 0;
    server.use(
      http.post('/api/v1/reports', () => {
        calls++;
        return new HttpResponse(null, { status: 503 });
      }),
    );
    await expect(api.post('/reports', {})).rejects.toBeInstanceOf(ApiError);
    expect(calls).toBe(1);
  });

  it('RNF-03 espera exponencial com variação aleatória', () => {
    for (let i = 0; i < 20; i++) {
      const wait = backoff(2, 400);
      expect(wait).toBeGreaterThanOrEqual(800);
      expect(wait).toBeLessThanOrEqual(2400);
    }
  });

  it('RF-09 sem handler de renovação, não renova', async () => {
    setRefreshHandler(async () => null);
    await expect(refreshOnce()).resolves.toBeNull();
  });
});
