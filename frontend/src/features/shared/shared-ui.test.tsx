import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { Route, Routes, useLocation } from 'react-router';
import type { MarketHits, StallDetail } from '../../shared/api/types';
import { renderWithProviders, setupI18n } from '../../test/render';
import { AuthLayout } from '../auth/AuthLayout';
import { ReportSheet } from '../stalls/ReportSheet';
import { MarketBoard, OutdatedWarning, SavedDataNotice, Verified } from './board';
import { CategoryIcon } from './CategoryIcon';
import { SearchBox } from './SearchBox';

const server = setupServer();
beforeAll(async () => {
  await setupI18n();
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const daysAgo = (n: number) => new Date(Date.now() - n * 86_400_000).toISOString();

function Where() {
  const location = useLocation();
  return <output data-testid="where">{location.pathname + location.search}</output>;
}

describe('RN-12 quadro de preços', () => {
  it('RF-04 RN-12 mostra preço com unidade, setor, falta e a data da verificação', () => {
    const hits: MarketHits = {
      market: { slug: 'ver-o-peso', name: 'Ver-o-Peso' },
      stalls: [
        {
          publicId: 'a',
          code: '142',
          name: 'Dona Ana',
          sector: 'Hortifruti',
          price: { amount: '15.00', currency: 'BRL' },
          priceUnit: 'LITER',
          availability: 'AVAILABLE',
          lastVerifiedAt: daysAgo(3),
          demo: false,
        },
        {
          publicId: 'b',
          code: '9',
          name: null,
          sector: null,
          price: null,
          priceUnit: null,
          availability: 'OUT_OF_STOCK',
          lastVerifiedAt: daysAgo(0),
          demo: false,
        },
        {
          publicId: 'c',
          code: '10',
          name: null,
          sector: null,
          price: null,
          priceUnit: null,
          availability: 'SEASONAL',
          lastVerifiedAt: daysAgo(1),
          demo: true,
        },
      ],
    };
    renderWithProviders(<MarketBoard hits={hits} headingLevel={2} />);
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Ver-o-Peso');
    expect(screen.getByText('3 boxes')).toBeInTheDocument();
    expect(screen.getByText('Box 142')).toBeInTheDocument();
    expect(screen.getByText(/R\$\s15,00/)).toBeInTheDocument();
    expect(screen.getByText('o litro')).toBeInTheDocument();
    expect(screen.getByText('verificado há 3 dias')).toBeInTheDocument();
    expect(screen.getAllByText('em falta')).not.toHaveLength(0);
    expect(screen.getByText('da safra')).toBeInTheDocument();
    expect(screen.getByText('preço no box')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Box 142/ })).toHaveAttribute('href', '/stalls/a');
    // ADR-0018: só o box fictício leva a marca de exemplo.
    expect(screen.getAllByText('Exemplo')).toHaveLength(1);
    expect(screen.getByRole('link', { name: /Box 10/ })).toHaveTextContent('Exemplo');
  });

  it('RN-12 RN-28 avisa dados antigos e cópia salva há mais de 7 dias', () => {
    renderWithProviders(
      <>
        <Verified at={null} />
        <OutdatedWarning />
        <SavedDataNotice savedAt={daysAgo(10)} />
        <CategoryIcon name="leaf" />
        <CategoryIcon name="desconhecido" />
      </>,
    );
    expect(screen.getByText('ainda não verificado')).toBeInTheDocument();
    expect(screen.getByText(/mais de 60 dias/)).toBeInTheDocument();
    expect(screen.getByText(/Podem estar desatualizados/)).toBeInTheDocument();
  });
});

describe('RF-02 caixa de busca', () => {
  it('RF-02 sugere a partir de 2 letras, navega pelo teclado e abre o produto', async () => {
    server.use(
      http.get('/api/v1/search/suggestions', ({ request }) => {
        expect(new URL(request.url).searchParams.get('q')).toBe('ja');
        return HttpResponse.json([
          { slug: 'jambu', name: 'Jambu', categorySlug: 'ervas' },
          { slug: 'jaca', name: 'Jaca', categorySlug: 'frutas' },
        ]);
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route
          path="*"
          element={
            <>
              <SearchBox focusOnMount />
              <Where />
            </>
          }
        />
      </Routes>,
    );
    const box = screen.getByRole('combobox', { name: 'Buscar produto' });
    expect(box).toHaveFocus();
    await user.type(box, 'ja');
    expect(await screen.findByRole('option', { name: 'Jambu' })).toBeInTheDocument();
    await user.keyboard('{ArrowDown}{ArrowDown}{ArrowUp}');
    expect(screen.getByRole('option', { name: 'Jambu' })).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('{Enter}');
    expect(screen.getByTestId('where')).toHaveTextContent('/products/jambu');
  });

  it('RF-02 sem sugestão escolhida, Enter leva para a busca completa', async () => {
    server.use(http.get('/api/v1/search/suggestions', () => HttpResponse.json([])));
    const user = userEvent.setup();
    renderWithProviders(
      <Routes>
        <Route
          path="*"
          element={
            <>
              <SearchBox initial="farinha d'água" />
              <Where />
            </>
          }
        />
      </Routes>,
    );
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{Enter}');
    expect(screen.getByTestId('where')).toHaveTextContent("/search?q=farinha%20d'%C3%A1gua");
  });
});

describe('RF-07 avisar informação errada', () => {
  const stall: StallDetail = {
    publicId: 'stall-1',
    code: '142',
    name: null,
    vendorName: null,
    market: { slug: 'ver-o-peso', name: 'Ver-o-Peso' },
    sector: null,
    photoUrl: null,
    photoThumbUrl: null,
    lastVerifiedAt: null,
    outdated: false,
    version: 1,
    demo: false,
    instagram: null,
    gallery: [],
    latitude: null,
    longitude: null,
    offerings: [
      {
        publicId: 'o',
        productSlug: 'acai',
        productName: 'Açaí',
        categorySlug: 'frutas',
        price: null,
        priceUnit: null,
        availability: 'AVAILABLE',
        lastVerifiedAt: daysAgo(1),
        version: 1,
      },
    ],
  };

  it('RF-07 RN-16 envia motivo, produto e o campo-armadilha vazio', async () => {
    let body: Record<string, unknown> | null = null;
    server.use(
      http.post('/api/v1/reports', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ received: true }, { status: 202 });
      }),
    );
    const onOpenChange = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<ReportSheet open onOpenChange={onOpenChange} stall={stall} />);
    expect(screen.getByRole('dialog', { name: 'Avisar sobre o box 142' })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('O que está errado?'), 'PRICE_MISMATCH');
    await user.selectOptions(screen.getByLabelText('Sobre qual produto?'), 'acai');
    await user.type(screen.getByLabelText(/Comentário/), 'Está R$ 20');
    await user.click(screen.getByRole('button', { name: 'Enviar aviso' }));
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    expect(body).toEqual({
      stallId: 'stall-1',
      productSlug: 'acai',
      reason: 'PRICE_MISMATCH',
      comment: 'Está R$ 20',
      website: '',
    });
  });

  it('RF-07 mostra o erro do servidor sem fechar', async () => {
    server.use(
      http.post('/api/v1/reports', () =>
        HttpResponse.json(
          { status: 429, code: 'RATE_LIMITED', detail: 'Muitos avisos seguidos. Espere um pouco.' },
          { status: 429 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<ReportSheet open onOpenChange={() => {}} stall={stall} />);
    await user.click(screen.getByRole('button', { name: 'Enviar aviso' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Muitos avisos seguidos');
  });
});

describe('RF-08 layout de autenticação', () => {
  it('mostra título e texto de apoio', () => {
    renderWithProviders(
      <AuthLayout title="Entrar" lead="Para colaborar.">
        <p>conteúdo</p>
      </AuthLayout>,
    );
    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    expect(screen.getByText('Para colaborar.')).toBeInTheDocument();
  });
});
