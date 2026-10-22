import { HandCoins, History, LogOut, UserRound } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../shared/auth/AuthProvider';

/**
 * Menu da conta no topo (RF-09, RF-11): perfil, histórico dos envios, ganhos e "Sair", no celular e na tela larga.
 * Fecha com Esc, com clique fora ou ao trocar de página; o foco volta ao botão.
 */
export function AccountMenu() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const menuId = useId();
  const root = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);

  useEffect(() => setOpen(false), [location.pathname]);

  useEffect(() => {
    if (!open) return;
    const onPointer = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        button.current?.focus();
      }
    };
    document.addEventListener('pointerdown', onPointer);
    document.addEventListener('keydown', onKey);
    root.current?.querySelector<HTMLElement>('a, button[data-item]')?.focus();
    return () => {
      document.removeEventListener('pointerdown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!user) return null;
  const item =
    'flex min-h-11 w-full items-center gap-3 rounded-[var(--radius-control)] px-3 text-left font-semibold text-ink no-underline hover:bg-soft';

  return (
    <div ref={root} className="relative">
      <button
        ref={button}
        type="button"
        aria-expanded={open}
        aria-controls={menuId}
        aria-label={t('nav.accountOf', { name: user.name })}
        onClick={() => setOpen((o) => !o)}
        className="inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-on-band hover:bg-band-hover"
      >
        <UserRound size={20} aria-hidden="true" />
      </button>
      {open && (
        <div
          id={menuId}
          className="absolute top-full right-0 z-40 mt-2 grid w-64 gap-1 rounded-[var(--radius-panel)] border border-line bg-surface p-2 text-ink shadow-[var(--shadow-dialog)]"
        >
          <p className="grid px-3 pt-1 pb-2">
            <span className="truncate font-semibold">{user.name}</span>
            <span className="truncate text-sm text-support">{user.email}</span>
          </p>
          <Link to="/profile" className={item}>
            <UserRound size={18} aria-hidden="true" />
            {t('nav.profile')}
          </Link>
          <Link to="/collaborate/history" className={item}>
            <History size={18} aria-hidden="true" />
            {t('nav.history')}
          </Link>
          <Link to="/collaborate/earnings" className={item}>
            <HandCoins size={18} aria-hidden="true" />
            {t('nav.earnings')}
          </Link>
          <button
            type="button"
            data-item
            className={`${item} border-t border-line text-danger`}
            onClick={() => void logout().then(() => navigate('/'))}
          >
            <LogOut size={18} aria-hidden="true" />
            {t('nav.logout')}
          </button>
        </div>
      )}
    </div>
  );
}
