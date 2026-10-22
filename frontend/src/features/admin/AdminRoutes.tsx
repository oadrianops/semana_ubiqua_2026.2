import { lazy, Suspense, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { NavLink, Route, Routes } from 'react-router';
import { useAuth } from '../../shared/auth/AuthProvider';
import { useNamespaces } from '../../shared/i18n/useNamespaces';
import { cn } from '../../shared/lib/cn';
import { EmptyState, LoadingRows } from '../../shared/ui/States';

const OverviewPage = lazy(() => import('./pages/OverviewPage'));
const QueuePage = lazy(() => import('./pages/QueuePage'));
const ReviewPage = lazy(() => import('./pages/ReviewPage'));
const ReportsPage = lazy(() => import('./pages/ReportsPage'));
const SuggestionsPage = lazy(() => import('./pages/SuggestionsPage'));
const TasksAdminPage = lazy(() => import('./pages/TasksAdminPage'));
const MarketsAdminPage = lazy(() => import('./pages/MarketsAdminPage'));
const StallsAdminPage = lazy(() => import('./pages/StallsAdminPage'));
const StallEditorPage = lazy(() => import('./pages/StallEditorPage'));
const CatalogAdminPage = lazy(() => import('./pages/CatalogAdminPage'));
const FinancePage = lazy(() => import('./pages/FinancePage'));
const UsersPage = lazy(() => import('./pages/UsersPage'));
const AuditPage = lazy(() => import('./pages/AuditPage'));

/**
 * Painel (RF-21 a RF-33): moderação para MODERATOR; finanças, usuários e auditoria só para ADMIN (11.2). No
 * celular, a navegação vira uma faixa rolável; na tela larga, uma coluna à esquerda.
 */
export default function AdminRoutes() {
  useNamespaces(['admin', 'collab']);
  const { t } = useTranslation('admin');
  const { hasRole } = useAuth();
  const admin = hasRole('ADMIN');

  const links: { to: string; label: string; end?: boolean; adminOnly?: boolean }[] = [
    { to: '/admin', label: t('nav.overview'), end: true },
    { to: '/admin/moderation', label: t('nav.moderation') },
    { to: '/admin/reports', label: t('nav.reports') },
    { to: '/admin/suggestions', label: t('nav.suggestions') },
    { to: '/admin/tasks', label: t('nav.tasks') },
    { to: '/admin/markets', label: t('nav.markets') },
    { to: '/admin/stalls', label: t('nav.stalls') },
    { to: '/admin/catalog', label: t('nav.catalog') },
    { to: '/admin/finance', label: t('nav.finance'), adminOnly: true },
    { to: '/admin/users', label: t('nav.users'), adminOnly: true },
    { to: '/admin/audit', label: t('nav.audit'), adminOnly: true },
  ];

  const only = (node: ReactNode) =>
    admin ? (
      node
    ) : (
      <EmptyState title={t('common:state.forbiddenTitle')} body={t('common:state.forbiddenBody')} />
    );

  return (
    <div className="safe-x mx-auto grid max-w-[var(--content-max)] gap-6 py-6 lg:grid-cols-[13rem_1fr] lg:gap-10">
      <nav
        aria-label={t('nav.label')}
        className="-mx-4 overflow-x-auto px-4 lg:sticky lg:top-4 lg:mx-0 lg:self-start lg:px-0"
      >
        <ul className="flex gap-1 border-b border-line lg:grid lg:border-b-0 lg:border-l-2 lg:border-ink">
          {links
            .filter((l) => admin || !l.adminOnly)
            .map((l) => (
              <li key={l.to}>
                <NavLink
                  to={l.to}
                  end={l.end}
                  className={({ isActive }) =>
                    cn(
                      'flex min-h-11 items-center px-3 font-semibold whitespace-nowrap no-underline lg:-ml-0.5 lg:border-l-2',
                      isActive
                        ? 'border-b-2 border-brand bg-brand-soft font-bold text-ink lg:border-b-0 lg:border-l-[3px] lg:border-brand'
                        : 'border-transparent text-support hover:text-ink',
                    )
                  }
                >
                  {l.label}
                </NavLink>
              </li>
            ))}
        </ul>
      </nav>
      <div className="min-w-0">
        <Suspense fallback={<LoadingRows rows={6} />}>
          <Routes>
            <Route index element={<OverviewPage />} />
            <Route path="moderation" element={<QueuePage />} />
            <Route path="moderation/:publicId" element={<ReviewPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="suggestions" element={<SuggestionsPage />} />
            <Route path="tasks" element={<TasksAdminPage />} />
            <Route path="markets" element={<MarketsAdminPage />} />
            <Route path="stalls" element={<StallsAdminPage />} />
            <Route path="stalls/:publicId" element={<StallEditorPage />} />
            <Route path="catalog" element={<CatalogAdminPage />} />
            <Route path="finance" element={only(<FinancePage />)} />
            <Route path="users" element={only(<UsersPage />)} />
            <Route path="audit" element={only(<AuditPage />)} />
          </Routes>
        </Suspense>
      </div>
    </div>
  );
}
