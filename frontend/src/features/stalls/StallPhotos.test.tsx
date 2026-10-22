import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, setupI18n } from '../../test/render';
import { StallPhotos } from './StallPhotos';

beforeAll(async () => {
  await setupI18n();
});

const photo = (id: string) => ({ url: `/api/v1/files/${id}`, thumbUrl: `/api/v1/files/${id}?variant=thumb` });

describe('RF-03 fotos do box', () => {
  it('RF-03 sem fotos não ocupa espaço', () => {
    const { container } = renderWithProviders(<StallPhotos photos={[]} code="16" market="Feira da 25" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('RF-03 uma foto só: sem miniaturas, com texto alternativo do box', () => {
    renderWithProviders(<StallPhotos photos={[photo('a')]} code="16" market="Feira da 25" />);
    expect(screen.getByRole('img', { name: 'Foto do box 16 — Feira da 25' })).toHaveAttribute(
      'srcset',
      '/api/v1/files/a?variant=thumb 400w, /api/v1/files/a 1600w',
    );
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('RF-03 várias fotos: a miniatura troca a foto grande e marca a escolhida', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <StallPhotos photos={[photo('a'), photo('b'), photo('c')]} code="67" market="Feira da 25" />,
    );
    expect(screen.getByRole('img', { name: 'Foto 1 de 3 do box 67 — Feira da 25' })).toBeInTheDocument();
    const third = screen.getByRole('button', { name: 'Ver foto 3 de 3' });
    await user.click(third);
    expect(third).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('img', { name: 'Foto 3 de 3 do box 67 — Feira da 25' })).toHaveAttribute(
      'src',
      '/api/v1/files/c',
    );
  });
});
