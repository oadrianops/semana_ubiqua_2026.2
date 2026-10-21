import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '@playwright/test';
import {
  atMarket,
  crowdedMarketNetwork,
  ensureOpenTask,
  expectAccessible,
  login,
  resetCollaborator,
  token,
} from './support';

const PHOTO = join(import.meta.dirname, 'fixtures', 'box.png');

test.describe('J1 turista', () => {
  test.use({ locale: 'en-US' });

  test('RF-02 RF-03 CA-1 CA-3 busca em inglês e vê preço e data da verificação', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('See which stall');
    await expectAccessible(page);
    const search = page.getByRole('combobox', { name: 'Search for a product' });
    await search.fill('brazil nut');
    await search.press('Enter');
    await expect(page).toHaveURL(/\/search\?q=brazil/);
    await expect(page.getByRole('heading', { name: /Brazil nut/ }).first()).toBeVisible();
    await expectAccessible(page);
    await page
      .getByRole('link', { name: /Stall 12/ })
      .first()
      .click();
    await expect(page).toHaveURL(/\/stalls\//);
    await expect(page.getByText(/R\$/).first()).toBeVisible();
    await expect(page.getByText(/verified/i).first()).toBeVisible();
    await expectAccessible(page);
  });
});

test.describe.serial('J2 colaborador e J3 moderador', () => {
  let stallCode = '';

  test('RF-14 a RF-18 CA-2 aceita, faz check-in na feira e envia com foto', async ({
    page,
    context,
    request,
  }) => {
    await resetCollaborator(request, 'ana', 'Ana');
    stallCode = await ensureOpenTask(request, 'ana');
    await atMarket(context);
    await login(page, 'ana');
    await page.goto('/collaborate');
    await expect(page.getByRole('heading', { name: 'Tarefas', level: 1 })).toBeVisible();
    await expectAccessible(page);
    await page
      .getByRole('link', { name: new RegExp(`Atualizar box Box ${stallCode}\\b`) })
      .first()
      .click();

    await page.getByRole('button', { name: /^Aceitar por/ }).click();
    await expect(page.getByRole('progressbar', { name: 'Prazo para o check-in' })).toBeVisible();
    await page.getByRole('button', { name: 'Fazer check-in aqui' }).click();
    await expect(page.getByRole('progressbar', { name: 'Prazo para enviar' })).toBeVisible();
    await expectAccessible(page);

    await page.getByRole('button', { name: 'Confirmar todos sem mudança' }).click();
    await page.locator('#photo-input').setInputFiles(PHOTO);
    await expect(page.getByText('Foto recebida')).toBeVisible();
    await page.getByRole('button', { name: 'Enviar atualização' }).click();
    await expect(page.getByText('Recebido. Agora a moderação revisa.')).toBeVisible();
  });

  test('RF-21 RF-22 RF-26 a moderação aprova e o saldo da colaboradora aumenta', async ({
    page,
    request,
  }) => {
    const ana = await token(request, 'ana');
    const balance = async () =>
      Number(
        (
          (await (
            await request.get('/api/v1/me/ledger', { headers: { Authorization: `Bearer ${ana}` } })
          ).json()) as {
            balance: { amount: string };
          }
        ).balance.amount,
      );
    const before = await balance();

    await login(page, 'moderador');
    await page.goto('/admin/moderation');
    await expect(page.getByRole('heading', { name: 'Fila de moderação' })).toBeVisible();
    await expectAccessible(page);
    await page
      .getByRole('link', { name: new RegExp(`Box ${stallCode}\\b.*Ana`) })
      .first()
      .click();
    await expect(page.getByRole('columnheader', { name: 'Depois' }).first()).toBeVisible();
    await expectAccessible(page);
    await page.getByRole('button', { name: /^Aprovar e creditar/ }).click();
    await expect(page).toHaveURL(/\/admin\/moderation$/);

    await expect.poll(balance).toBeGreaterThan(before);
  });
});

test.describe('J4 idiomas', () => {
  const keys = new Set<string>();
  const localesDir = join(import.meta.dirname, '..', 'src', 'shared', 'i18n', 'locales', 'pt-BR');
  const walk = (prefix: string, value: unknown) => {
    if (value && typeof value === 'object')
      for (const [k, v] of Object.entries(value)) walk(prefix ? `${prefix}.${k}` : k, v);
    else keys.add(prefix.replace(/_(one|other|many)$/, ''));
  };
  for (const file of readdirSync(localesDir))
    walk('', JSON.parse(readFileSync(join(localesDir, file), 'utf8')));

  for (const locale of ['pt-BR', 'en-US', 'es-ES']) {
    test(`RF-06 CA-3 páginas principais em ${locale} sem chaves cruas`, async ({ browser }) => {
      const context = await browser.newContext({ locale });
      const page = await context.newPage();
      for (const path of [
        '/',
        '/markets',
        '/markets/ver-o-peso',
        '/search?q=jambu',
        '/products/jambu',
        '/about',
        '/login',
      ]) {
        await page.goto(path);
        await page.waitForLoadState('networkidle');
        const text = await page.locator('body').innerText();
        const raw = [...keys].filter((k) => k.includes('.') && text.includes(k));
        expect(raw, `${locale} ${path}`).toEqual([]);
      }
      await context.close();
    });
  }
});

test('J5 RN-05 check-in a 2 km da feira é recusado', async ({ page, context, request }) => {
  await resetCollaborator(request, 'beto', 'Beto');
  const code = await ensureOpenTask(request, 'beto');
  await atMarket(context, 0.018);
  await login(page, 'beto');
  await page.goto('/collaborate');
  await page
    .getByRole('link', { name: new RegExp(`Atualizar box Box ${code}\\b`) })
    .first()
    .click();
  await page.getByRole('button', { name: /^Aceitar por/ }).click();
  await page.getByRole('button', { name: 'Fazer check-in aqui' }).click();
  await expect(page.getByRole('alert')).toContainText(/Você está a 2([,.]\d)? km da feira/);
  await page.getByRole('button', { name: 'Desistir da tarefa' }).click();
  await expect(page.getByRole('dialog', { name: 'Desistir desta tarefa?' })).toBeVisible();
  await page.getByRole('button', { name: 'Sim, desistir' }).click();
  await expect(page).toHaveURL(/\/collaborate$/);
});

test('J6 @chromium-only RNF-03 RN-27 CA-6 rede cai no envio e ele chega uma única vez', async ({
  page,
  context,
  request,
}) => {
  await resetCollaborator(request, 'beto', 'Beto');
  const code = await ensureOpenTask(request, 'beto');
  await atMarket(context);
  await login(page, 'beto');
  await page.goto('/collaborate');
  await page
    .getByRole('link', { name: new RegExp(`Atualizar box Box ${code}\\b`) })
    .first()
    .click();
  await page.getByRole('button', { name: /^Aceitar por/ }).click();
  await page.getByRole('button', { name: 'Fazer check-in aqui' }).click();
  await page.getByRole('button', { name: 'Confirmar todos sem mudança' }).click();
  const taskUrl = page.url();

  let uploads = 0;
  let submissions = 0;
  page.on('request', (r) => {
    if (r.method() === 'POST' && r.url().includes('/api/v1/uploads')) uploads++;
    if (r.method() === 'POST' && r.url().endsWith('/submission')) submissions++;
  });

  await context.setOffline(true);
  await page.locator('#photo-input').setInputFiles(PHOTO);
  await expect(page.getByText('Aguardando sinal').first()).toBeVisible();
  await page.getByRole('button', { name: 'Enviar atualização' }).click();
  await expect(page.getByText('Na fila, aguardando sinal')).toBeVisible();

  await context.setOffline(false);
  await expect(page.getByText('Recebido. Agora a moderação revisa.')).toBeVisible({ timeout: 45_000 });
  expect(uploads).toBe(1);
  expect(submissions).toBe(1);

  const beto = await token(request, 'beto');
  const history = (await (
    await request.get('/api/v1/me/tasks', { headers: { Authorization: `Bearer ${beto}` } })
  ).json()) as {
    taskId: string;
    submissionStatus: string | null;
  }[];
  const taskId = taskUrl.split('/').pop();
  expect(history.filter((h) => h.taskId === taskId && h.submissionStatus === 'PENDING')).toHaveLength(1);
});

test('J7 @chromium-only RF-35 RF-36 CA-6 sem rede a feira salva e a busca local funcionam', async ({
  page,
  context,
}) => {
  await page.goto('/markets/ver-o-peso');
  await expect(page.getByRole('heading', { name: 'Ver-o-Peso', level: 1 })).toBeVisible();
  await page.getByRole('button', { name: 'Salvar para usar sem internet' }).click();
  await expect(page.getByRole('button', { name: /Salva no aparelho/ })).toBeVisible();
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  await page.reload();
  await page.waitForLoadState('networkidle');

  await context.setOffline(true);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Ver-o-Peso', level: 1 })).toBeVisible();
  await expect(page.getByText(/Dados salvos em/).first()).toBeVisible();

  await page.goto('/search?q=castanha');
  await expect(page.getByText('Resultado dos dados salvos no aparelho (sem conexão).')).toBeVisible();
  await expect(page.getByText(/Box 12/).first()).toBeVisible();
  await context.setOffline(false);
});

test('J8 @chromium-only RNF-03 CA-7 Feira lotada: conteúdo útil em até 3 s e depois em até 1 s', async ({
  page,
}) => {
  await crowdedMarketNetwork(page);
  const firstStart = Date.now();
  await page.goto('/', { waitUntil: 'commit' });
  // Conteúdo útil = a busca pronta para usar (o app shell do index.html já a entrega antes do JS).
  await expect(page.getByLabel('Buscar produto').first()).toBeVisible({ timeout: 15_000 });
  const first = Date.now() - firstStart;

  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  const secondStart = Date.now();
  await page.reload({ waitUntil: 'commit' });
  await expect(page.getByRole('combobox', { name: 'Buscar produto' })).toBeVisible();
  const second = Date.now() - secondStart;

  test
    .info()
    .annotations.push({ type: 'medição', description: `1ª visita ${first} ms · 2ª visita ${second} ms` });
  console.log(`J8 Feira lotada: 1ª visita ${first} ms, 2ª visita ${second} ms`);
  expect(first).toBeLessThanOrEqual(3000);
  expect(second).toBeLessThanOrEqual(1000);
});
