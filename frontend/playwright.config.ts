import { defineConfig, devices } from '@playwright/test';

/**
 * E2E (seção 15.4): jornadas J1 a J8 contra o stack com o seed. No CI, E2E_BASE_URL aponta para o
 * `docker compose --profile app` (porta 8081); localmente, sobe o build de produção no `vite preview`, com o
 * proxy de /api para a API em :8080.
 */
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:4173';
const allBrowsers = process.env.E2E_ALL_BROWSERS === '1';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['github']] : [['list']],
  use: {
    baseURL,
    locale: 'pt-BR',
    timezoneId: 'America/Belem',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    serviceWorkers: 'allow',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    ...(allBrowsers
      ? [
          { name: 'pixel', use: { ...devices['Pixel 7'] }, grepInvert: /@chromium-only/ },
          { name: 'iphone', use: { ...devices['iPhone 14'] }, grepInvert: /@chromium-only/ },
        ]
      : []),
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run build && npx vite preview --port 4173 --strictPort',
        url: 'http://localhost:4173',
        reuseExistingServer: true,
        timeout: 180_000,
      },
});
