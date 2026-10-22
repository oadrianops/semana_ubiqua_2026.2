import { screen } from '@testing-library/react';
import { renderWithProviders, setupI18n } from '../../test/render';
import { RouteErrorBoundary } from './RouteErrorBoundary';

beforeAll(async () => {
  await setupI18n();
});

function Broken(): never {
  throw new TypeError("Cannot read properties of undefined (reading 'map')");
}

describe('RNF-03 erro inesperado numa tela', () => {
  it('RNF-03 mostra o estado de erro com "Tentar de novo" em vez da tela branca', () => {
    const quiet = vi.spyOn(console, 'error').mockImplementation(() => {});
    renderWithProviders(
      <RouteErrorBoundary>
        <Broken />
      </RouteErrorBoundary>,
    );
    expect(screen.getByRole('button', { name: /Tentar de novo/ })).toBeInTheDocument();
    quiet.mockRestore();
  });
});
