import { Bell, ClipboardList, HandCoins, History, Search, Store, UserRound } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, Outlet, useLocation } from 'react-router';
import { RouteErrorBoundary } from '../shared/ui/RouteErrorBoundary';
import { useAuth } from '../shared/auth/AuthProvider';
import { useUnreadCount } from '../features/notifications/useNotifications';
import { cn } from '../shared/lib/cn';
import { startOutbox } from '../shared/offline/outbox';
import { refreshSavedMarkets } from '../shared/offline/snapshots';
import { PwaPrompts } from '../shared/pwa/PwaPrompts';
import { Logo } from '../shared/ui/Logo';
import { AccountMenu } from './AccountMenu';
import { LanguageSwitch } from './LanguageSwitch';
import { NetworkBanner } from './NetworkBanner';
import { appConfig } from '../shared/config';

/** Estrutura das telas: faixa Açaí 900, aviso de rede, conteúdo, rodapé e navegação inferior do colaborador. */
export function AppLayout() {
  const { t, i18n } = useTranslation();
  const { user, hasRole } = useAuth();
  const location = useLocation();
  const mainRef = useRef<HTMLElement>(null);
  const first = useRef(true);

  useEffect(() => {
    startOutbox();
  }, []);

  useEffect(() => {
    void refreshSavedMarkets(i18n.language);
  }, [i18n.language]);

  // Foco gerenciado a cada troca de rota (14.5) e rolagem para o topo.
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    window.scrollTo({ top: 0 });
    const heading = mainRef.current?.querySelector('h1');
    if (heading) {
      heading.setAttribute('tabindex', '-1');
      heading.focus({ preventScroll: true });
    } else {
      mainRef.current?.focus({ preventScroll: true });
    }
  }, [location.pathname]);

  const isCollaborator = Boolean(user);
  const isHome = location.pathname === '/';

  return (
    <div className="flex min-h-dvh flex-col">
      <a
        href="#conteudo"
        className="sr-only z-50 rounded-[var(--radius-control)] bg-highlight px-4 py-3 font-semibold text-on-highlight focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
      >
        {t('a11y.skipToContent')}
      </a>
      <header className="on-band bg-band pt-[var(--safe-top)] text-on-band">
        <div className="safe-x mx-auto flex min-h-16 max-w-[var(--content-max)] items-center gap-3">
          <Link to="/" className="inline-flex min-h-11 shrink-0 items-center" aria-label={t('nav.home')}>
            <Logo reversed width={124} />
          </Link>
          <nav aria-label={t('nav.main')} className="ml-auto flex min-w-0 items-center gap-0.5 sm:gap-1">
            {/* Com sessão, no celular, a barra inferior já leva às seções: o topo fica com busca, sino e idioma. */}
            <HeaderLink to="/markets" label={t('nav.markets')} desktopOnly={Boolean(user)} />
            {!isHome && (
              <Link
                to="/search"
                className="inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-on-band hover:bg-band-hover"
                aria-label={t('nav.search')}
              >
                <Search size={20} aria-hidden="true" />
              </Link>
            )}
            {user ? (
              <>
                <HeaderLink to="/collaborate" label={t('nav.collaborate')} desktopOnly />
                {hasRole('MODERATOR') && <HeaderLink to="/admin" label={t('nav.admin')} desktopOnly />}
                <BellLink />
                <AccountMenu />
              </>
            ) : (
              <HeaderLink to="/login" label={t('nav.login')} />
            )}
            <LanguageSwitch />
          </nav>
        </div>
      </header>
      <NetworkBanner />
      {appConfig().demoNotice && (
        <p className="safe-x border-b border-line bg-soft py-1.5 text-center text-sm text-support">
          {t('demoNotice')}
        </p>
      )}
      <main
        id="conteudo"
        ref={mainRef}
        tabIndex={-1}
        className={cn('flex-1 outline-none', isCollaborator && 'pb-20 md:pb-0')}
      >
        <RouteErrorBoundary key={location.pathname}>
          <Outlet />
        </RouteErrorBoundary>
      </main>
      <PwaPrompts />
      <footer className="border-t border-line bg-soft">
        <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-6 py-8 text-sm text-support md:grid-cols-[1fr_auto]">
          <div className="grid gap-2">
            <Logo width={104} />
            <p className="max-w-[var(--measure)]">{t('footer.tagline')}</p>
            <p>
              {t('footer.contact')}{' '}
              <a href={`mailto:${appConfig().contactEmail}`}>{appConfig().contactEmail}</a>
            </p>
          </div>
          <nav
            aria-label={t('footer.navLabel')}
            className="grid content-start gap-1 sm:grid-cols-2 sm:gap-x-8"
          >
            <Link className="inline-flex min-h-11 items-center" to="/about">
              {t('footer.about')}
            </Link>
            <Link className="inline-flex min-h-11 items-center" to="/for-vendors">
              {t('footer.vendors')}
            </Link>
            <Link className="inline-flex min-h-11 items-center" to="/privacy">
              {t('footer.privacy')}
            </Link>
            <Link className="inline-flex min-h-11 items-center" to="/terms">
              {t('footer.terms')}
            </Link>
            {!user && (
              <Link className="inline-flex min-h-11 items-center" to="/signup">
                {t('footer.becomeCollaborator')}
              </Link>
            )}
          </nav>
        </div>
      </footer>
      {isCollaborator && <BottomNav />}
    </div>
  );
}

/** Link do cabeçalho; `desktopOnly` some no celular, onde a barra inferior já leva ao mesmo lugar. */
function HeaderLink({
  to,
  label,
  desktopOnly = false,
}: {
  to: string;
  label: string;
  desktopOnly?: boolean;
}) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          'min-h-11 items-center rounded-[var(--radius-control)] px-2.5 font-semibold whitespace-nowrap text-on-band no-underline hover:bg-band-hover sm:px-3',
          desktopOnly ? 'hidden md:inline-flex' : 'inline-flex',
          isActive && 'underline decoration-[var(--tucupi-400)] decoration-2 underline-offset-8',
        )
      }
    >
      {label}
    </NavLink>
  );
}

function BellLink() {
  const { t } = useTranslation();
  const unread = useUnreadCount();
  return (
    <Link
      to="/notifications"
      className="relative inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-on-band hover:bg-band-hover"
      aria-label={unread ? t('nav.notificationsUnread', { count: unread }) : t('nav.notifications')}
    >
      <Bell size={20} aria-hidden="true" />
      {unread > 0 && (
        <span className="tabular absolute top-0.5 right-0 min-w-5 rounded-full bg-highlight px-1 text-center text-xs leading-5 font-bold text-on-highlight">
          {unread > 9 ? '9+' : unread}
        </span>
      )}
    </Link>
  );
}

/** Navegação inferior do colaborador no celular, respeitando a barra de gestos (RF-35). */
function BottomNav() {
  const { t } = useTranslation();
  const { hasRole } = useAuth();
  const items = [
    {
      to: '/collaborate',
      label: t('nav.tasks'),
      icon: <ClipboardList size={22} aria-hidden="true" />,
      end: true,
    },
    { to: '/collaborate/history', label: t('nav.history'), icon: <History size={22} aria-hidden="true" /> },
    {
      to: '/collaborate/earnings',
      label: t('nav.earnings'),
      icon: <HandCoins size={22} aria-hidden="true" />,
    },
    hasRole('MODERATOR')
      ? { to: '/admin', label: t('nav.admin'), icon: <Store size={22} aria-hidden="true" /> }
      : { to: '/profile', label: t('nav.profile'), icon: <UserRound size={22} aria-hidden="true" /> },
  ];
  return (
    <nav
      aria-label={t('nav.collaborator')}
      className="fixed inset-x-0 bottom-0 z-30 border-t border-line bg-surface pb-[var(--safe-bottom)] md:hidden"
    >
      <ul className="grid grid-cols-4">
        {items.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  'flex min-h-14 flex-col items-center justify-center gap-0.5 text-xs font-semibold no-underline',
                  isActive ? 'text-brand' : 'text-support',
                )
              }
            >
              {item.icon}
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
