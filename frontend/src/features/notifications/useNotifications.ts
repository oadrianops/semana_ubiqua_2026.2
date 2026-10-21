import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { api } from '../../shared/api/http';
import type { NotificationPage } from '../../shared/api/types';
import { useAuth } from '../../shared/auth/AuthProvider';
import { appConfig } from '../../shared/config';

/**
 * Sino com contador (RF-14): tempo real por SSE com ticket de uso único enquanto o app está aberto; sem SSE,
 * consulta a cada 60 s (9.8).
 */
export function useNotificationsQuery(page = 0) {
  const { user } = useAuth();
  return useQuery({
    queryKey: ['me', 'notifications', page],
    queryFn: () => api.get<NotificationPage>(`/me/notifications?page=${page}&size=30`),
    enabled: Boolean(user),
    refetchInterval: 60_000,
    staleTime: 15_000,
    networkMode: 'online',
  });
}

export function useUnreadCount(): number {
  const { user } = useAuth();
  const client = useQueryClient();
  const query = useNotificationsQuery(0);

  useEffect(() => {
    if (!user || typeof EventSource === 'undefined') return;
    let source: EventSource | null = null;
    let closed = false;
    let retry = 0;
    const connect = async () => {
      try {
        const { ticket } = await api.post<{ ticket: string }>('/me/notifications/stream-ticket', undefined, {
          retries: 0,
        });
        if (closed) return;
        source = new EventSource(
          `${appConfig().apiBaseUrl}/v1/me/notifications/stream?ticket=${encodeURIComponent(ticket)}`,
        );
        source.addEventListener('notification', () => {
          void client.invalidateQueries({ queryKey: ['me', 'notifications'] });
        });
        source.onerror = () => {
          source?.close();
          if (!closed) setTimeout(() => void connect(), Math.min(60_000, 2_000 * 2 ** retry++));
        };
        source.onopen = () => {
          retry = 0;
        };
      } catch {
        if (!closed) setTimeout(() => void connect(), Math.min(60_000, 5_000 * 2 ** retry++));
      }
    };
    void connect();
    return () => {
      closed = true;
      source?.close();
    };
  }, [user, client]);

  return query.data?.unread ?? 0;
}
