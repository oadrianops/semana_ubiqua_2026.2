import { renderHook, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { enqueue, removeItem } from '../../shared/offline/outbox';
import { renderWithProviders, setupI18n } from '../../test/render';
import { locate, useAvailableTasks, useTaskActions } from './api';
import { MyActiveTasks } from './MyActiveTasks';
import { DeadlineBar, Stepper } from './Stepper';

const server = setupServer();
beforeAll(async () => {
  await setupI18n();
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, networkMode: 'always' } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('CA-2 stepper da tarefa', () => {
  it('14.3 marca a etapa atual e as concluídas', () => {
    renderWithProviders(<Stepper current={2} />);
    const steps = screen.getAllByRole('listitem');
    expect(steps).toHaveLength(3);
    expect(steps[1]).toHaveAttribute('aria-current', 'step');
    expect(steps[0]).not.toHaveAttribute('aria-current');
    expect(screen.getByText('Check-in')).toBeInTheDocument();
  });

  it('RN-02 a barra do prazo mede o tempo que resta e fica urgente nos 15 minutos finais', () => {
    const now = Date.now();
    const { rerender } = renderWithProviders(
      <DeadlineBar
        start={new Date(now - 60 * 60_000).toISOString()}
        deadline={new Date(now + 60 * 60_000).toISOString()}
        label="Prazo para o check-in"
      />,
    );
    const bar = screen.getByRole('progressbar', { name: 'Prazo para o check-in' });
    expect(Number(bar.getAttribute('aria-valuenow'))).toBeGreaterThanOrEqual(49);
    expect(Number(bar.getAttribute('aria-valuenow'))).toBeLessThanOrEqual(51);
    expect(screen.getByText(/faltam/)).not.toHaveClass('text-danger');

    rerender(
      <DeadlineBar
        start={new Date(now - 60 * 60_000).toISOString()}
        deadline={new Date(now + 10 * 60_000).toISOString()}
        label="Prazo para enviar"
      />,
    );
    expect(screen.getByText(/faltam 10 min/)).toHaveClass('text-danger');

    rerender(
      <DeadlineBar
        start={new Date(now - 60 * 60_000).toISOString()}
        deadline={new Date(now - 1000).toISOString()}
        label="Prazo para enviar"
      />,
    );
    expect(screen.getByText('prazo vencido')).toBeInTheDocument();
  });
});

describe('RF-15 RF-17 consultas e ações da tarefa', () => {
  it('RF-15 envia filtros e a posição só como parâmetro da consulta (RN-21)', async () => {
    server.use(
      http.get('/api/v1/tasks/available', ({ request }) => {
        const q = new URL(request.url).searchParams;
        expect(q.get('market')).toBe('ver-o-peso');
        expect(q.get('type')).toBe('NEW_STALL');
        expect(q.get('lat')).toBe('-1.45');
        return HttpResponse.json({
          items: [],
          reputation: 100,
          minimumReputation: 40,
          canAccept: true,
          blockedReason: null,
        });
      }),
    );
    const { result } = renderHook(
      () => useAvailableTasks({ market: 'ver-o-peso', type: 'NEW_STALL', lat: -1.45, lng: -48.5 }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.data?.canAccept).toBe(true));
  });

  it('RN-01 aceitar, check-in e desistir chamam a API sem repetir sozinhos', async () => {
    const calls: string[] = [];
    server.use(
      http.post('/api/v1/tasks/t1/accept', () => {
        calls.push('accept');
        return HttpResponse.json({
          taskId: 't1',
          status: 'ACCEPTED',
          commission: { amount: '8.00', currency: 'BRL' },
          checkinDeadlineAt: '2026-10-20T12:00:00Z',
        });
      }),
      http.post('/api/v1/tasks/t1/check-in', async ({ request }) => {
        calls.push(`check-in:${JSON.stringify(await request.json())}`);
        return HttpResponse.json({
          status: 'CHECKED_IN',
          distanceMeters: 20,
          flag: null,
          submissionDeadlineAt: '2026-10-20T13:00:00Z',
        });
      }),
      http.post('/api/v1/tasks/t1/release', () => {
        calls.push('release');
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { result } = renderHook(() => useTaskActions('t1'), { wrapper });
    await result.current.accept.mutateAsync();
    await result.current.checkIn.mutateAsync({ latitude: -1.45, longitude: -48.5, accuracyMeters: 12 });
    await result.current.release.mutateAsync();
    expect(calls).toEqual([
      'accept',
      'check-in:{"latitude":-1.45,"longitude":-48.5,"accuracyMeters":12}',
      'release',
    ]);
  });

  it('RF-17 sem geolocalização no aparelho, o check-in explica em vez de travar', async () => {
    const original = navigator.geolocation;
    Object.defineProperty(navigator, 'geolocation', { value: undefined, configurable: true });
    // @ts-expect-error simulando navegador sem a API
    delete navigator.geolocation;
    await expect(locate()).rejects.toMatchObject({ code: 0 });
    Object.defineProperty(navigator, 'geolocation', { value: original, configurable: true });
  });

  it('RF-17 usa alta precisão e 20 s de tempo-limite', async () => {
    const getCurrentPosition = vi.fn(
      (ok: PositionCallback, _fail?: PositionErrorCallback | null, _options?: PositionOptions) =>
        ok({ coords: { latitude: 1, longitude: 2, accuracy: 5 } } as GeolocationPosition),
    );
    Object.defineProperty(navigator, 'geolocation', { value: { getCurrentPosition }, configurable: true });
    const pos = await locate();
    expect(pos.coords.accuracy).toBe(5);
    expect(getCurrentPosition.mock.calls[0]?.[2]).toEqual({
      enableHighAccuracy: true,
      timeout: 20_000,
      maximumAge: 0,
    });
  });
});

describe('14.3 tarefas em andamento', () => {
  it('RF-18 lista tarefas aceitas e envios que aguardam sinal', async () => {
    server.use(
      http.get('/api/v1/me/tasks', () =>
        HttpResponse.json([
          {
            taskId: 't1',
            type: 'UPDATE_STALL',
            taskStatus: 'CHECKED_IN',
            marketName: 'Ver-o-Peso',
            stallCode: '142',
            stallName: null,
            commission: { amount: '8.00', currency: 'BRL' },
            acceptedAt: '2026-10-20T10:00:00Z',
            submittedAt: null,
            outcome: null,
            outcomeAt: null,
            submissionStatus: null,
            rejectionReason: null,
            reviewComment: null,
          },
          {
            taskId: 't0',
            type: 'NEW_STALL',
            taskStatus: 'COMPLETED',
            marketName: 'Ver-o-Peso',
            stallCode: null,
            stallName: null,
            commission: { amount: '4.00', currency: 'BRL' },
            acceptedAt: '2026-10-19T10:00:00Z',
            submittedAt: null,
            outcome: 'APPROVED',
            outcomeAt: null,
            submissionStatus: 'APPROVED',
            rejectionReason: null,
            reviewComment: null,
          },
        ]),
      ),
    );
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true });
    await enqueue('t9', { taskType: 'VERIFY_REPORT', stall: null, offerings: [], photos: [] }, []);
    renderWithProviders(<MyActiveTasks />);
    expect(await screen.findByRole('heading', { name: 'Em andamento' })).toBeInTheDocument();
    expect(await screen.findByText(/Atualizar box · Ver-o-Peso · Box 142/)).toBeInTheDocument();
    expect(screen.getByText('Check-in feito')).toBeInTheDocument();
    expect(await screen.findByText('Verificar denúncia')).toBeInTheDocument();
    expect(screen.getByText('Na fila')).toBeInTheDocument();
    expect(screen.queryByText(/Cadastrar box novo/)).not.toBeInTheDocument();
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true });
    await removeItem('t9');
  });

  it('RF-20 envio entregue fica "Em revisão" na lista, com caminho para o histórico', async () => {
    server.use(
      http.get('/api/v1/me/tasks', () =>
        HttpResponse.json([
          {
            taskId: 't2',
            type: 'UPDATE_STALL',
            taskStatus: 'SUBMITTED',
            marketName: 'Feira da 25',
            stallCode: '67',
            stallName: null,
            commission: { amount: '4.00', currency: 'BRL' },
            acceptedAt: '2026-10-22T10:00:00Z',
            submittedAt: '2026-10-22T10:40:00Z',
            outcome: null,
            outcomeAt: null,
            submissionStatus: 'PENDING',
            rejectionReason: null,
            reviewComment: null,
          },
        ]),
      ),
    );
    renderWithProviders(<MyActiveTasks />);
    const row = await screen.findByRole('link', { name: /Atualizar box · Feira da 25 · Box 67/ });
    expect(row).toHaveTextContent('Em revisão');
    expect(row).toHaveAttribute('href', '/collaborate/history');
    expect(screen.getByRole('link', { name: /resultado no histórico/ })).toHaveAttribute(
      'href',
      '/collaborate/history',
    );
  });
});
