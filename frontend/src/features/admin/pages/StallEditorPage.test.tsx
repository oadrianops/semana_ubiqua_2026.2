import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { Route, Routes } from 'react-router';
import { renderWithProviders, setupI18n } from '../../../test/render';
import type { AdminStallDetail } from '../types';
import StallEditorPage from './StallEditorPage';

const server = setupServer();
beforeAll(async () => {
  await setupI18n();
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const detail: AdminStallDetail = {
  stall: {
    publicId: 's1',
    marketSlug: 'feira-da-25',
    code: '16',
    displayName: 'Fruteira',
    vendorName: null,
    showVendorName: false,
    sectorSlug: 'hortifruti',
    sectorName: 'Hortifrúti',
    status: 'ACTIVE',
    consentStatus: 'GRANTED',
    consentMethod: 'SIGNED_FORM',
    consentRecordedAt: '2026-10-21T13:00:00Z',
    lastVerifiedAt: null,
    version: 0,
    demo: false,
    instagram: 'fruteira',
    photoThumbUrl: '/api/v1/files/f1?variant=thumb',
    latitude: null,
    longitude: null,
  },
  offerings: [],
  photos: [{ fileId: 'f1', url: '/api/v1/files/f1', thumbUrl: '/api/v1/files/f1?variant=thumb', main: true }],
};

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/admin/stalls/:publicId" element={<StallEditorPage />} />
    </Routes>,
    { route: '/admin/stalls/s1' },
  );
}

describe('RF-29 box conferido em campo no painel', () => {
  it('RF-29 RN-24 lança a oferta com o preço digitado com vírgula e mostra a foto principal', async () => {
    let body: unknown = null;
    server.use(
      http.get('/api/v1/admin/stalls/s1', () => HttpResponse.json(detail)),
      http.get('/api/v1/markets/feira-da-25', () =>
        HttpResponse.json({ slug: 'feira-da-25', name: 'Feira da 25', sectors: [], openingHours: [] }),
      ),
      http.get('/api/v1/search/suggestions', () =>
        HttpResponse.json([{ slug: 'tomate', name: 'Tomate', categorySlug: 'legumes-e-verduras' }]),
      ),
      http.put('/api/v1/admin/stalls/s1/offerings/tomate', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          ...detail,
          offerings: [
            {
              publicId: 'o1',
              productSlug: 'tomate',
              productName: 'Tomate',
              categorySlug: 'legumes-e-verduras',
              price: { amount: '15.00', currency: 'BRL' },
              priceUnit: 'KG',
              availability: 'AVAILABLE',
              lastVerifiedAt: '2026-10-21T13:00:00Z',
              version: 0,
            },
          ],
        });
      }),
    );
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByRole('heading', { level: 1, name: 'Box 16 · Fruteira' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Instagram do box/)).toHaveValue('@fruteira');
    expect(screen.getByText('Principal')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Salvar oferta' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Escolha um produto da lista.');

    await user.type(screen.getByLabelText(/^Produto/), 'tom');
    await user.click(await screen.findByRole('button', { name: 'Tomate' }));
    const price = screen.getByLabelText(/Preço de referência/);
    await user.type(price, '15,5x');
    expect(screen.getByText('Preço inválido. Exemplo: 15,00.')).toBeVisible();
    await user.clear(price);
    await user.type(price, '15,00');
    await user.click(screen.getByRole('button', { name: 'Salvar oferta' }));

    await waitFor(() => expect(body).toEqual({ price: '15.00', priceUnit: 'KG', availability: 'AVAILABLE' }));
    expect(await screen.findByRole('button', { name: 'Remover Tomate' })).toBeInTheDocument();
  });
});
