import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, setRefreshHandler, tokenStore } from '../api/http';
import type { Profile, Role, SessionResponse } from '../api/types';

/**
 * Sessão (12.1, RF-09): token de acesso só em memória; ao abrir o app, tenta renovar pelo cookie HttpOnly para
 * restaurar a sessão; renovação silenciosa em 401 (uma por vez).
 */
interface AuthContextValue {
  user: Profile | null;
  ready: boolean;
  login: (email: string, password: string) => Promise<Profile>;
  logout: () => Promise<void>;
  setUser: (user: Profile | null) => void;
  hasRole: (role: Role) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const RANK: Record<Role, number> = { COLLABORATOR: 1, MODERATOR: 2, ADMIN: 3 };

export function roleAtLeast(user: Profile | null, role: Role): boolean {
  return Boolean(user && RANK[user.role] >= RANK[role]);
}

async function refreshSession(): Promise<SessionResponse | null> {
  try {
    return await api.post<SessionResponse>('/auth/refresh', undefined, {
      auth: false,
      retries: 1,
      timeoutMs: 8000,
    });
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Profile | null>(null);
  const [ready, setReady] = useState(false);
  const queryClient = useQueryClient();

  useEffect(() => {
    setRefreshHandler(async () => {
      const session = await refreshSession();
      tokenStore.set(session?.accessToken ?? null);
      setUser(session?.user ?? null);
      return session?.accessToken ?? null;
    });
    void (async () => {
      const session = await refreshSession();
      if (session) {
        tokenStore.set(session.accessToken);
        setUser(session.user);
      }
      setReady(true);
    })();
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const session = await api.post<SessionResponse>(
      '/auth/login',
      { email, password },
      { auth: false, retries: 0 },
    );
    tokenStore.set(session.accessToken);
    setUser(session.user);
    return session.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout', undefined, { auth: false, retries: 0 });
    } finally {
      tokenStore.set(null);
      setUser(null);
      queryClient.removeQueries({ predicate: (q) => q.queryKey[0] === 'me' });
    }
  }, [queryClient]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, ready, login, logout, setUser, hasRole: (role) => roleAtLeast(user, role) }),
    [user, ready, login, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth fora do AuthProvider');
  return ctx;
}
