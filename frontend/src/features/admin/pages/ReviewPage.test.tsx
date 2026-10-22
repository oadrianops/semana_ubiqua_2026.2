import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { Route, Routes } from 'react-router';
import { renderWithProviders, setupI18n } from '../../../test/render';
import type { SubmissionDetail } from '../types';
import ReviewPage from './ReviewPage';

const server = setupServer();
beforeAll(async () => {
  await setupI18n();
  server.listen({ onUnhandledRequest: 'error' });
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const base: SubmissionDetail = {
  publicId: 'sub1',
  taskId: 't1',
  type: 'UPDATE_STALL',
  status: 'PENDING',
  marketName: 'Feira da 25',
  marketSlug: 'feira-da-25',
  stallId: 's7',
  stallCode: '7',
  collaboratorName: 'Beto',
  collaboratorReputation: 80,
  ownSubmission: false,
  commission: { amount: '4.00', currency: 'BRL' },
  checkIn: { distanceMeters: 0, accuracyMeters: 12, flag: null, checkedInAt: '2026-10-22T17:20:00Z' },
  stallDiff: [],
  offerings: [
    {
      productSlug: 'jambu',
      productName: 'Jambu',
      action: 'UPDATE',
      before: { price: { amount: '2.50', currency: 'BRL' }, priceUnit: 'BUNCH', availability: 'AVAILABLE' },
      after: { price: { amount: '3.00', currency: 'BRL' }, priceUnit: 'BUNCH', availability: 'AVAILABLE' },
      conflict: true,
    },
  ],
  photos: [],
  conflicts: ['stall', 'offering:jambu'],
  notes: null,
  changeSet: { taskType: 'UPDATE_STALL', stall: null, offerings: [], photos: [] },
  createdAt: '2026-10-22T17:21:00Z',
  rejectionReason: null,
  reviewComment: null,
};

function renderReview(detail: SubmissionDetail) {
  server.use(http.get('/api/v1/moderation/submissions/sub1', () => HttpResponse.json(detail)));
  return renderWithProviders(
    <Routes>
      <Route path="/admin/moderation/:publicId" element={<ReviewPage />} />
    </Routes>,
    { route: '/admin/moderation/sub1' },
  );
}

describe('RF-22 RN-26 conflitos na revisão de um envio', () => {
  it('RN-26 lista os conflitos pelo nome do item, sem as chaves internas', async () => {
    renderReview(base);
    const box = await screen.findByRole('region', { name: 'Os dados mudaram depois deste envio' });
    expect(box).toHaveTextContent('Dados do box');
    expect(box).toHaveTextContent('Jambu');
    expect(box).not.toHaveTextContent('offering:jambu');
    expect(box).not.toHaveTextContent(/\bstall\b/);
  });

  it('RN-26 envio já aprovado não mostra conflito com ele mesmo', async () => {
    renderReview({ ...base, status: 'APPROVED' });
    expect(await screen.findByText('Aprovado: comissão creditada')).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Os dados mudaram depois deste envio' })).not.toBeInTheDocument();
  });
});
