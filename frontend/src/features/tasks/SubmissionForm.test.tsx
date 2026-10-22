import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import type { StallDetail, TaskDetail } from '../../shared/api/types';
import { getItem, loadDraft, removeItem } from '../../shared/offline/outbox';
import { renderWithProviders, setupI18n } from '../../test/render';
import { buildChangeSet, parsePrice, SubmissionForm, validateForm, type FormState } from './SubmissionForm';

vi.mock('../../shared/lib/image-compress', () => ({
  compressImage: async (file: Blob) => new Blob([await file.arrayBuffer()], { type: 'image/jpeg' }),
}));

const server = setupServer();
beforeAll(async () => {
  await setupI18n();
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const market = {
  slug: 'ver-o-peso',
  name: 'Ver-o-Peso',
  latitude: -1.45,
  longitude: -48.5,
  radiusMeters: 300,
};

function task(overrides: Partial<TaskDetail> = {}): TaskDetail {
  return {
    publicId: 'task-1',
    type: 'UPDATE_STALL',
    status: 'CHECKED_IN',
    market,
    stall: {
      publicId: 'stall-1',
      code: '142',
      name: 'Box da Dona Ana',
      sectorName: 'Hortifruti',
      version: 3,
    },
    suggestedSector: null,
    suggestedSectorSlug: null,
    instructions: null,
    commission: { amount: '8.00', currency: 'BRL' },
    publishedAt: '2026-10-20T10:00:00Z',
    myAssignment: null,
    ...overrides,
  };
}

const empty: FormState = {
  existing: [],
  added: [],
  code: '',
  displayName: '',
  vendorName: '',
  sectorSlug: '',
  consentPublish: false,
  consentShowName: false,
  consentMethod: 'VERBAL',
  problemConfirmed: '',
  comment: '',
  notes: '',
};

const photo = {
  clientUploadId: 'p1',
  blob: new Blob(['x']),
  role: 'STALL' as const,
  state: 'uploaded' as const,
  fileId: 'f1',
};

describe('RF-18 formulário de envio', () => {
  it('RF-18 aceita preço com vírgula ou ponto e recusa texto', () => {
    expect(parsePrice('12,50')).toBe('12.50');
    expect(parsePrice('R$ 1.234,5')).toBe('1234.50');
    expect(parsePrice('7.5')).toBe('7.50');
    expect(parsePrice('')).toBeNull();
    expect(parsePrice('doze')).toBeUndefined();
    expect(parsePrice('0')).toBeUndefined();
  });

  it('RN-13 RN-06 NEW_STALL exige foto da fachada, número e consentimento', () => {
    const t = task({ type: 'NEW_STALL', stall: null });
    expect(validateForm(t, empty, [])).toEqual(
      expect.arrayContaining([
        'errors.photoRequired',
        'errors.codeRequired',
        'errors.consentRequired',
        'errors.stallPhotoRequired',
      ]),
    );
    expect(validateForm(t, { ...empty, code: '12', consentPublish: true }, [photo])).toEqual([]);
  });

  it('RF-18 recusa preço inválido, produto vazio e produto repetido', () => {
    const t = task();
    const form: FormState = {
      ...empty,
      existing: [
        {
          productSlug: 'acai',
          productName: 'Açaí',
          baseVersion: 1,
          action: 'UPDATE',
          price: 'abc',
          unit: 'LITER',
          availability: 'AVAILABLE',
        },
      ],
      added: [
        {
          key: 'a',
          productSlug: 'acai',
          productName: 'Açaí',
          suggestion: null,
          price: '',
          unit: 'KG',
          availability: 'AVAILABLE',
        },
        {
          key: 'b',
          productSlug: null,
          productName: '',
          suggestion: null,
          price: '',
          unit: 'KG',
          availability: 'AVAILABLE',
        },
      ],
    };
    expect(validateForm(t, form, [photo])).toEqual(
      expect.arrayContaining(['errors.priceInvalid', 'errors.productRequired', 'errors.productDuplicated']),
    );
  });

  it('RF-28 VERIFY_REPORT pede a resposta da verificação', () => {
    expect(validateForm(task({ type: 'VERIFY_REPORT' }), empty, [photo])).toContain(
      'errors.verificationRequired',
    );
  });

  it('Anexo A monta o changeset com baseVersion e ignora o que não foi conferido', () => {
    const form: FormState = {
      ...empty,
      notes: '  conferido às 9h  ',
      existing: [
        {
          productSlug: 'acai',
          productName: 'Açaí',
          baseVersion: 4,
          action: 'UPDATE',
          price: '15,00',
          unit: 'LITER',
          availability: 'AVAILABLE',
        },
        {
          productSlug: 'jambu',
          productName: 'Jambu',
          baseVersion: 2,
          action: 'CONFIRM',
          price: '',
          unit: 'BUNCH',
          availability: 'AVAILABLE',
        },
        {
          productSlug: 'tucupi',
          productName: 'Tucupi',
          baseVersion: 1,
          action: 'REMOVE',
          price: '',
          unit: 'LITER',
          availability: 'AVAILABLE',
        },
        {
          productSlug: 'farinha',
          productName: 'Farinha',
          baseVersion: 1,
          action: 'KEEP',
          price: '',
          unit: 'KG',
          availability: 'AVAILABLE',
        },
      ],
      added: [
        {
          key: 'a',
          productSlug: 'castanha',
          productName: 'Castanha',
          suggestion: null,
          price: '40',
          unit: 'KG',
          availability: 'SEASONAL',
        },
        {
          key: 'b',
          productSlug: null,
          productName: '',
          suggestion: { namePt: 'Bacuri', categorySlug: 'frutas' },
          price: '',
          unit: 'UNIT',
          availability: 'AVAILABLE',
        },
      ],
    };
    const cs = buildChangeSet(task(), form);
    expect(cs.stall).toEqual({ publicId: 'stall-1', baseVersion: 3 });
    expect(cs.notes).toBe('conferido às 9h');
    expect(cs.offerings).toEqual([
      {
        action: 'UPDATE',
        productSlug: 'acai',
        baseVersion: 4,
        referencePrice: { amount: '15.00', currency: 'BRL' },
        priceUnit: 'LITER',
        availability: 'AVAILABLE',
      },
      { action: 'CONFIRM', productSlug: 'jambu', baseVersion: 2 },
      { action: 'REMOVE', productSlug: 'tucupi', baseVersion: 1 },
      {
        action: 'ADD',
        productSlug: 'castanha',
        referencePrice: { amount: '40.00', currency: 'BRL' },
        priceUnit: 'KG',
        availability: 'SEASONAL',
      },
      {
        action: 'ADD_SUGGESTED',
        suggestion: { namePt: 'Bacuri', categorySlug: 'frutas' },
        referencePrice: null,
        priceUnit: 'UNIT',
        availability: 'AVAILABLE',
      },
    ]);
    expect(cs.verification).toBeNull();
  });

  it('RN-13 NEW_STALL leva consentimento e setor sugerido; VERIFY_REPORT leva a verificação', () => {
    const fresh = buildChangeSet(task({ type: 'NEW_STALL', stall: null, suggestedSectorSlug: 'ervas' }), {
      ...empty,
      code: ' A-07 ',
      vendorName: 'Seu Zé',
      consentPublish: true,
      consentShowName: true,
      consentMethod: 'SIGNED_FORM',
    });
    expect(fresh.stall).toMatchObject({
      marketSlug: 'ver-o-peso',
      sectorSlug: 'ervas',
      code: 'A-07',
      vendorName: 'Seu Zé',
      consent: { publish: true, showVendorName: true, method: 'SIGNED_FORM' },
    });
    const check = buildChangeSet(task({ type: 'VERIFY_REPORT' }), {
      ...empty,
      problemConfirmed: 'yes',
      comment: 'preço mudou',
    });
    expect(check.verification).toEqual({ problemConfirmed: true, comment: 'preço mudou' });
  });
});

describe('RF-18 RNF-03 envio pela fila', () => {
  const stall: StallDetail = {
    publicId: 'stall-1',
    code: '142',
    name: 'Box da Dona Ana',
    vendorName: null,
    market: { slug: 'ver-o-peso', name: 'Ver-o-Peso' },
    sector: { slug: 'hortifruti', name: 'Hortifruti' },
    photoUrl: null,
    photoThumbUrl: null,
    lastVerifiedAt: '2026-10-01T10:00:00Z',
    outdated: false,
    version: 3,
    demo: false,
    instagram: null,
    gallery: [],
    latitude: null,
    longitude: null,
    offerings: [
      {
        publicId: 'o1',
        productSlug: 'acai',
        productName: 'Açaí',
        categorySlug: 'frutas',
        price: { amount: '15.00', currency: 'BRL' },
        priceUnit: 'LITER',
        availability: 'AVAILABLE',
        lastVerifiedAt: '2026-10-01T10:00:00Z',
        version: 4,
      },
    ],
  };

  it('RF-18 sem foto mostra o que falta; com foto, a atualização entra na fila e é enviada uma vez', async () => {
    let submissions = 0;
    server.use(
      http.get('/api/v1/stalls/stall-1', () => HttpResponse.json(stall)),
      http.post('/api/v1/uploads', () =>
        HttpResponse.json({ fileId: 'file-1', status: 'TEMP', width: 1200, height: 900, expiresAt: null }),
      ),
      http.post('/api/v1/tasks/task-1/submission', async ({ request }) => {
        submissions++;
        const body = (await request.json()) as { offerings: unknown[]; photos: { fileId: string }[] };
        expect(body.offerings).toEqual([{ action: 'CONFIRM', productSlug: 'acai', baseVersion: 4 }]);
        expect(body.photos[0]?.fileId).toBe('file-1');
        return HttpResponse.json({
          submissionId: 's1',
          taskId: 'task-1',
          status: 'PENDING',
          receivedAt: '2026-10-20T10:00:00Z',
        });
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<SubmissionForm task={task()} />);

    expect(await screen.findByText('Açaí')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Confirmar todos sem mudança' }));
    await user.click(screen.getByRole('button', { name: 'Enviar atualização' }));
    expect(await screen.findByText('Tire pelo menos uma foto.')).toBeInTheDocument();

    const file = new File(['jpeg'], 'box.jpg', { type: 'image/jpeg' });
    await user.upload(document.getElementById('photo-input') as HTMLInputElement, file);
    expect(await screen.findByText('Foto recebida')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Enviar atualização' }));
    await waitFor(async () => expect((await getItem('task-1'))?.state).toBe('SENT'));
    expect(submissions).toBe(1);
    expect(await loadDraft('task-1')).toBeUndefined();
    await removeItem('task-1');
  });

  it('RN-13 cadastro de box novo pede o consentimento antes de enviar', async () => {
    server.use(
      http.get('/api/v1/markets/ver-o-peso', () =>
        HttpResponse.json({
          slug: 'ver-o-peso',
          name: 'Ver-o-Peso',
          sectors: [{ slug: 'ervas', name: 'Ervas', stallCount: 3 }],
        }),
      ),
      http.get('/api/v1/categories', () => HttpResponse.json([])),
    );
    const user = userEvent.setup();
    renderWithProviders(
      <SubmissionForm task={task({ publicId: 'task-2', type: 'NEW_STALL', stall: null })} />,
    );

    await user.type(await screen.findByLabelText(/Número do box/), '88');
    await user.click(screen.getByRole('button', { name: 'Enviar atualização' }));
    expect(
      await screen.findByText('Marque o consentimento do feirante para o box aparecer.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Inclua uma foto da fachada do box.')).toBeInTheDocument();

    await user.click(screen.getByLabelText('O feirante aceita que o box apareça no app.'));
    expect(screen.getByLabelText('O feirante aceita que o box apareça no app.')).toBeChecked();
    // O nome só pode ser exibido depois de informado.
    expect(screen.getByLabelText('O feirante aceita mostrar o nome dele.')).toBeDisabled();
    await user.type(screen.getByLabelText(/Nome do feirante/), 'Seu Zé');
    expect(screen.getByLabelText('O feirante aceita mostrar o nome dele.')).toBeEnabled();
  });

  it('RF-31 adiciona produto do catálogo ou sugere um novo', async () => {
    server.use(
      http.get('/api/v1/stalls/stall-1', () => HttpResponse.json({ ...stall, offerings: [] })),
      http.get('/api/v1/search/suggestions', () =>
        HttpResponse.json([{ slug: 'jambu', name: 'Jambu', categorySlug: 'ervas' }]),
      ),
      http.get('/api/v1/categories', () =>
        HttpResponse.json([{ slug: 'ervas', name: 'Ervas', icon: 'leaf', productCount: 3 }]),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<SubmissionForm task={task({ publicId: 'task-3' })} />);

    await user.click(await screen.findByRole('button', { name: 'Adicionar produto' }));
    await user.type(screen.getByLabelText('Produto'), 'jam');
    await user.click(await screen.findByRole('button', { name: 'Jambu' }));
    expect(screen.getByText('Jambu')).toBeInTheDocument();

    await user.type(screen.getAllByLabelText(/Preço de referência/)[0]!, '5,5');
    expect(screen.getByText('R$ 5,50')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Adicionar produto' }));
    await user.type(screen.getByLabelText('Produto'), 'Bacuri');
    await user.click(screen.getByRole('button', { name: 'Não está na lista: sugerir "Bacuri"' }));
    expect(screen.getByLabelText('Nome do produto sugerido')).toHaveValue('Bacuri');
  });
});
