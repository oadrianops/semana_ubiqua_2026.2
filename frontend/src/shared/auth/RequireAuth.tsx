import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, Outlet, useLocation } from 'react-router';
import type { Role } from '../api/types';
import { ensureNamespaces } from '../i18n/config';
import { EmptyState, LoadingRows } from '../ui/States';
import { useAuth } from './AuthProvider';

/** Rota protegida por papel (9.4): sem sessão, vai para o login com returnTo; sem papel, explica. */
export function RequireAuth({ minRole: role }: { minRole: Role }) {
  const { user, ready, hasRole } = useAuth();
  const location = useLocation();
  const { t, i18n } = useTranslation();
  const [nsReady, setNsReady] = useState(false);

  useEffect(() => {
    void ensureNamespaces(
      role === 'COLLABORATOR' ? ['account', 'collab'] : ['account', 'collab', 'admin'],
      i18n.language,
    ).then(() => setNsReady(true));
  }, [role, i18n.language]);

  if (!ready || !nsReady) {
    return (
      <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
        <LoadingRows />
      </div>
    );
  }
  if (!user) {
    return (
      <Navigate to={`/login?returnTo=${encodeURIComponent(location.pathname + location.search)}`} replace />
    );
  }
  if (!hasRole(role)) {
    return (
      <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
        <EmptyState title={t('state.forbiddenTitle')} body={t('state.forbiddenBody')} />
      </div>
    );
  }
  return <Outlet />;
}
