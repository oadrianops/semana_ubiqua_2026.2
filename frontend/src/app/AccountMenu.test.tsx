import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, setupI18n } from '../test/render';
import { AccountMenu } from './AccountMenu';

const logout = vi.fn(() => Promise.resolve());
vi.mock('../shared/auth/AuthProvider', () => ({
  useAuth: () => ({ user: { name: 'Ana', email: 'ana@nandesk.local' }, logout }),
}));

beforeAll(async () => {
  await setupI18n();
});

describe('RF-09 menu da conta', () => {
  it('RF-09 abre com perfil, histórico, ganhos e "Sair", e fecha com Esc', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AccountMenu />);
    const button = screen.getByRole('button', { name: 'Conta de Ana' });
    await user.click(button);
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('ana@nandesk.local')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Perfil' })).toHaveAttribute('href', '/profile');
    expect(screen.getByRole('link', { name: 'Histórico' })).toHaveAttribute('href', '/collaborate/history');
    expect(screen.getByRole('link', { name: 'Ganhos' })).toHaveAttribute('href', '/collaborate/earnings');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('button', { name: 'Sair' })).not.toBeInTheDocument();
    expect(button).toHaveFocus();
  });

  it('RF-09 "Sair" encerra a sessão', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AccountMenu />);
    await user.click(screen.getByRole('button', { name: 'Conta de Ana' }));
    await user.click(screen.getByRole('button', { name: 'Sair' }));
    expect(logout).toHaveBeenCalledTimes(1);
  });
});
