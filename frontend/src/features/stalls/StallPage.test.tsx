import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { Route, Routes } from 'react-router';
import { renderWithProviders, setupI18n } from '../../test/render';
import { StallPage } from './pages/StallPage';

const server = setupServer();
beforeAll(async () => {
  await setupI18n();
  server.listen({ onUnhandledRequest: 'bypass' });
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

/** Box como a v1.0.0 guardava no aparelho: sem galeria, posição, Instagram nem marca de exemplo. */
const oldStall = {
  publicId: 's51',
  code: '51',
  name: 'Cheiros do Pará',
  vendorName: null,
  market: { slug: 'ver-o-peso', name: 'Ver-o-Peso' },
  sector: { slug: 'ervas-e-banhos', name: 'Ervas e banhos de cheiro' },
  photoUrl: null,
  photoThumbUrl: null,
  lastVerifiedAt: '2026-08-13T12:00:00Z',
  outdated: true,
  version: 0,
  offerings: [],
};

describe('RF-03 RF-36 página do box com dados salvos de uma versão anterior', () => {
  it('RF-36 abre o box mesmo sem os campos que vieram depois (galeria, posição, exemplo)', async () => {
    server.use(http.get('/api/v1/stalls/s51', () => HttpResponse.json(oldStall)));
    renderWithProviders(
      <Routes>
        <Route path="/stalls/:publicId" element={<StallPage />} />
      </Routes>,
      { route: '/stalls/s51' },
    );
    expect(await screen.findByRole('heading', { level: 1 })).toHaveTextContent('Cheiros do Pará');
    expect(screen.queryByRole('link', { name: /Como chegar ao box/ })).not.toBeInTheDocument();
  });
});
