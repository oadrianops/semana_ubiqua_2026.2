// Capturas reais do app para o README (docs/images) e para o manifesto do PWA (public/screenshots), feitas com
// Playwright contra o build de produção com o seed de demonstração. Uso: npx vite preview --port 4173 e, em
// outro terminal, node scripts/capturas.mjs (na pasta frontend). A fila de moderação precisa de um envio pendente.
import { chromium, devices } from '@playwright/test';
import { mkdirSync } from 'node:fs';

const BASE = 'http://localhost:4173';
const OUT_DOCS = '../docs/images';
const OUT_PWA = 'public/screenshots';
mkdirSync(OUT_DOCS, { recursive: true });
mkdirSync(OUT_PWA, { recursive: true });

const browser = await chromium.launch();
const phone = { ...devices['Pixel 7'], viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, locale: 'pt-BR' };

async function login(page, email) {
  await page.goto(`${BASE}/login`);
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill('troque-me-dev');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.waitForURL((u) => !u.pathname.startsWith('/login'));
}

async function settle(page) {
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(400);
}

// Celular
{
  const ctx = await browser.newContext(phone);
  const page = await ctx.newPage();
  await page.goto(`${BASE}/`);
  await settle(page);
  await page.screenshot({ path: `${OUT_DOCS}/inicio-celular.png` });
  await page.screenshot({ path: `${OUT_PWA}/home-narrow.png` });
  await page.goto(`${BASE}/search?q=castanha`);
  await settle(page);
  await page.screenshot({ path: `${OUT_DOCS}/busca-celular.png` });
  await page.getByRole('link', { name: /Box 12\b/ }).first().click();
  await settle(page);
  await page.screenshot({ path: `${OUT_DOCS}/box-celular.png` });
  await ctx.close();
}

// Tarefa (colaboradora, sem aceitar)
{
  const ctx = await browser.newContext(phone);
  const page = await ctx.newPage();
  await login(page, 'ana@nandesk.local');
  await page.goto(`${BASE}/collaborate`);
  await settle(page);
  await page.getByRole('link', { name: /^Atualizar box/ }).first().click();
  await settle(page);
  await page.screenshot({ path: `${OUT_DOCS}/tarefa-celular.png` });
  await ctx.close();
}

// Desktop
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1, locale: 'pt-BR' });
  const page = await ctx.newPage();
  await page.goto(`${BASE}/`);
  await settle(page);
  await page.screenshot({ path: `${OUT_PWA}/home-wide.png` });
  await login(page, 'moderador@nandesk.local');
  await page.goto(`${BASE}/admin/moderation`);
  await settle(page);
  const first = page.getByRole('link', { name: /Atualizar box|Cadastrar box/ }).first();
  if (await first.count()) {
    await first.click();
    await settle(page);
  }
  await page.screenshot({ path: `${OUT_DOCS}/moderacao-desktop.png` });
  await ctx.close();
}

await browser.close();
console.log('capturas prontas');
