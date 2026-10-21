import AxeBuilder from '@axe-core/playwright';
import { expect, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test';

export const PASSWORD = process.env.E2E_SEED_PASSWORD ?? 'troque-me-dev';
export const VER_O_PESO = { latitude: -1.4525, longitude: -48.5035 };
const API = '/api/v1';

/** RNF-07: nenhuma violação "serious" ou "critical" do axe na página aberta. */
export async function expectAccessible(page: Page) {
  const result = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa', 'wcag22aa'])
    .analyze();
  const serious = result.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
  expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).join(' | ')}`)).toEqual([]);
}

export async function login(page: Page, user: string) {
  await page.goto('/login');
  await page.getByLabel('E-mail').fill(`${user}@nandesk.local`);
  await page.getByLabel('Senha').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

const tokens = new Map<string, string>();

export async function token(request: APIRequestContext, user: string): Promise<string> {
  const cached = tokens.get(user);
  if (cached) return cached;
  const res = await request.post(`${API}/auth/login`, {
    data: { email: `${user}@nandesk.local`, password: PASSWORD },
  });
  expect(res.ok()).toBeTruthy();
  const value = ((await res.json()) as { accessToken: string }).accessToken;
  tokens.set(user, value);
  return value;
}

interface TaskRow {
  publicId: string;
  type: string;
  stall: { code: string } | null;
  market: { slug: string };
}

/**
 * Garante uma tarefa UPDATE_STALL aberta no Ver-o-Peso para a jornada: usa uma do seed ou pede à moderação que
 * crie uma num box sem tarefa ativa (RN-01). Devolve o número do box.
 */
/**
 * Deixa a pessoa sem tarefas em andamento (RN-03 limita a 2): desiste das aceitas e a moderação aprova os envios
 * pendentes que sobraram de execuções anteriores. Aprovar (e não recusar) preserva a reputação (RN-17), para a
 * jornada poder rodar várias vezes no mesmo banco. No CI o banco começa vazio e isto não faz nada.
 */
export async function resetCollaborator(request: APIRequestContext, collaborator: string, name: string) {
  const auth = (t: string) => ({ Authorization: `Bearer ${t}` });
  const mine = await token(request, collaborator);
  const history = (await (await request.get(`${API}/me/tasks`, { headers: auth(mine) })).json()) as {
    taskId: string;
    taskStatus: string;
    outcome: string | null;
  }[];
  for (const h of history.filter(
    (x) => !x.outcome && (x.taskStatus === 'ACCEPTED' || x.taskStatus === 'CHECKED_IN'),
  )) {
    await request.post(`${API}/tasks/${h.taskId}/release`, { headers: auth(mine) });
  }
  const mod = await token(request, 'moderador');
  const queue = (await (
    await request.get(`${API}/moderation/submissions?size=50`, { headers: auth(mod) })
  ).json()) as {
    items: { publicId: string; collaboratorName: string }[];
  };
  for (const item of queue.items.filter((i) => i.collaboratorName === name)) {
    await request.post(`${API}/moderation/submissions/${item.publicId}/approve`, {
      headers: auth(mod),
      data: { acceptConflicts: true },
    });
  }
}

export async function ensureOpenTask(request: APIRequestContext, collaborator: string): Promise<string> {
  const auth = (t: string) => ({ Authorization: `Bearer ${t}` });
  const mine = await token(request, collaborator);
  const available = async () =>
    (
      (await (
        await request.get(`${API}/tasks/available?market=ver-o-peso&type=UPDATE_STALL`, {
          headers: auth(mine),
        })
      ).json()) as {
        items: TaskRow[];
      }
    ).items;
  const open = await available();
  if (open[0]?.stall) return open[0].stall.code;

  const mod = await token(request, 'moderador');
  const stalls = (
    (await (await request.get(`${API}/markets/ver-o-peso/stalls?size=50`)).json()) as {
      items: { publicId: string; code: string }[];
    }
  ).items;
  for (const stall of stalls) {
    const res = await request.post(`${API}/moderation/tasks`, {
      headers: auth(mod),
      data: { type: 'UPDATE_STALL', marketSlug: 'ver-o-peso', stallPublicId: stall.publicId, priority: 3 },
    });
    if (res.status() === 201) return stall.code;
  }
  throw new Error('Nenhum box livre para criar tarefa');
}

export async function atMarket(context: BrowserContext, offsetLatitude = 0) {
  await context.grantPermissions(['geolocation']);
  await context.setGeolocation({
    ...VER_O_PESO,
    latitude: VER_O_PESO.latitude + offsetLatitude,
    accuracy: 15,
  });
}

/** Perfil de rede "Feira lotada" (seção 9.14): 400 ms, 400 kbps de descida e 150 kbps de subida (só Chromium). */
export async function crowdedMarketNetwork(page: Page) {
  const cdp = await page.context().newCDPSession(page);
  await cdp.send('Network.enable');
  await cdp.send('Network.emulateNetworkConditions', {
    offline: false,
    latency: 400,
    downloadThroughput: (400 * 1024) / 8,
    uploadThroughput: (150 * 1024) / 8,
  });
  return cdp;
}
