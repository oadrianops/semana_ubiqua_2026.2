import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from '../../../shared/ui/toast';
import { api } from '../../../shared/api/http';
import type { Page, Role, UserStatus } from '../../../shared/api/types';
import { formatDate } from '../../../shared/lib/format';
import { TextField } from '../../../shared/ui/Field';
import { EmptyState, ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useDebounced } from '../../public-api';
import type { AdminUser } from '../types';
import { AdminHeader, errorMessage, Pager, selectClass } from '../ui';

const ROLES: Role[] = ['COLLABORATOR', 'MODERATOR', 'ADMIN'];
const STATUSES: UserStatus[] = ['ACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'];

/** Usuários (RF-12, só ADMIN): papel e bloqueio. E-mails aparecem mascarados (RN-22); cada troca vai para a auditoria. */
export default function UsersPage() {
  const { t, i18n } = useTranslation('admin');
  const client = useQueryClient();
  const [q, setQ] = useState('');
  const [role, setRole] = useState<Role | ''>('');
  const [page, setPage] = useState(0);
  const debounced = useDebounced(q);
  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (debounced.trim()) params.set('q', debounced.trim());
  if (role) params.set('role', role);
  const query = useQuery({
    queryKey: ['admin', 'users', debounced, role, page],
    queryFn: () => api.get<Page<AdminUser>>(`/admin/users?${params.toString()}`),
    networkMode: 'online',
  });
  const update = useMutation({
    mutationFn: ({ id, body }: { id: string; body: { role?: Role; status?: UserStatus } }) =>
      api.patch(`/admin/users/${id}`, body, { retries: 0 }),
    onSuccess: () => {
      toast.success(t('users.saved'));
      void client.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (e) => toast.error(errorMessage(e, t('users.error'))),
  });

  return (
    <div className="grid gap-6">
      <AdminHeader title={t('users.title')} lead={t('users.lead')} />
      <div className="flex flex-wrap items-end gap-3">
        <TextField
          label={t('users.search')}
          value={q}
          className="min-w-[14rem]"
          onChange={(e) => (setQ(e.target.value), setPage(0))}
        />
        <label className="grid gap-1">
          <span className="text-sm font-semibold text-support">{t('users.role')}</span>
          <select
            className={selectClass}
            value={role}
            onChange={(e) => (setRole(e.target.value as Role | ''), setPage(0))}
          >
            <option value="">{t('filters.all')}</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {t(`users.roles.${r}`)}
              </option>
            ))}
          </select>
        </label>
      </div>
      {query.isPending && <LoadingRows rows={6} />}
      {query.isError && <ErrorState error={query.error} onRetry={() => void query.refetch()} />}
      {query.data?.items.length === 0 && (
        <EmptyState title={t('users.emptyTitle')} body={t('users.emptyBody')} />
      )}
      <ul className="divide-y divide-line border-t-2 border-ink">
        {query.data?.items.map((u) => (
          <li key={u.publicId} className="grid gap-2 py-3 md:grid-cols-[1fr_auto] md:items-center">
            <span className="grid gap-0.5">
              <span className="font-semibold">{u.name}</span>
              <span className="text-sm text-support">
                {u.emailMasked} · {t('users.reputation', { score: u.reputation })} ·{' '}
                {t('users.since', { date: formatDate(u.createdAt, i18n.language) })}
              </span>
            </span>
            <span className="flex flex-wrap gap-2">
              <select
                aria-label={t('users.roleOf', { name: u.name })}
                className={selectClass}
                value={u.role}
                onChange={(e) => update.mutate({ id: u.publicId, body: { role: e.target.value as Role } })}
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {t(`users.roles.${r}`)}
                  </option>
                ))}
              </select>
              <select
                aria-label={t('users.statusOf', { name: u.name })}
                className={selectClass}
                value={u.status}
                onChange={(e) =>
                  update.mutate({ id: u.publicId, body: { status: e.target.value as UserStatus } })
                }
              >
                {[...new Set([u.status, ...STATUSES])].map((s) => (
                  <option key={s} value={s}>
                    {t(`users.statuses.${s}`)}
                  </option>
                ))}
              </select>
            </span>
          </li>
        ))}
      </ul>
      {query.data && <Pager page={query.data.page} totalPages={query.data.totalPages} onPage={setPage} />}
    </div>
  );
}
