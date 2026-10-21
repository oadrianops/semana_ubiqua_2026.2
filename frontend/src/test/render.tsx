import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react';
import { initReactI18next } from 'react-i18next';
import { MemoryRouter } from 'react-router';
import type { ReactElement, ReactNode } from 'react';
import i18n from '../shared/i18n/config';
import account from '../shared/i18n/locales/pt-BR/account.json';
import admin from '../shared/i18n/locales/pt-BR/admin.json';
import collab from '../shared/i18n/locales/pt-BR/collab.json';
import common from '../shared/i18n/locales/pt-BR/common.json';
import publicNs from '../shared/i18n/locales/pt-BR/public.json';

/** i18n em PT-BR com todos os namespaces já carregados: os testes conferem o texto que a pessoa lê. */
export async function setupI18n() {
  if (i18n.isInitialized) return i18n;
  await i18n.use(initReactI18next).init({
    lng: 'pt-BR',
    fallbackLng: 'pt-BR',
    ns: ['common', 'public', 'account', 'collab', 'admin'],
    defaultNS: 'common',
    fallbackNS: 'common',
    resources: { 'pt-BR': { common, public: publicNs, account, collab, admin } },
    interpolation: { escapeValue: false },
  });
  return i18n;
}

export function renderWithProviders(
  ui: ReactElement,
  { route = '/', ...options }: RenderOptions & { route?: string } = {},
) {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0, networkMode: 'always' },
      mutations: { retry: false },
    },
  });
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  }
  return { client, ...render(ui, { wrapper: Wrapper, ...options }) };
}
