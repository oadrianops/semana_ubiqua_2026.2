import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../shared/api/http';
import type {
  AcceptResult,
  AvailableTasks,
  CheckInResult,
  HistoryItem,
  TaskDetail,
  TaskType,
} from '../../shared/api/types';
import { useLang } from '../public-api';

/** Dados pessoais e de tarefas: só em memória, rede primeiro (9.13). */
export function useAvailableTasks(filters: {
  market?: string | null;
  type?: TaskType | null;
  lat?: number | null;
  lng?: number | null;
}) {
  const lng = useLang();
  const params = new URLSearchParams();
  if (filters.market) params.set('market', filters.market);
  if (filters.type) params.set('type', filters.type);
  if (filters.lat != null && filters.lng != null) {
    params.set('lat', String(filters.lat));
    params.set('lng', String(filters.lng));
  }
  return useQuery({
    queryKey: ['me', 'tasks', 'available', filters, lng],
    queryFn: () => api.get<AvailableTasks>(`/tasks/available?${params.toString()}`),
    networkMode: 'online',
    refetchInterval: 60_000,
  });
}

export function useTask(publicId: string) {
  const lng = useLang();
  return useQuery({
    queryKey: ['me', 'task', publicId, lng],
    queryFn: () => api.get<TaskDetail>(`/tasks/${publicId}`),
    networkMode: 'offlineFirst',
  });
}

export function useHistory() {
  return useQuery({
    queryKey: ['me', 'history'],
    queryFn: () => api.get<HistoryItem[]>('/me/tasks'),
    networkMode: 'online',
  });
}

export function useTaskActions(publicId: string) {
  const client = useQueryClient();
  const invalidate = () => client.invalidateQueries({ queryKey: ['me'] });
  const accept = useMutation({
    mutationFn: () => api.post<AcceptResult>(`/tasks/${publicId}/accept`, undefined, { retries: 0 }),
    onSettled: invalidate,
  });
  const checkIn = useMutation({
    mutationFn: (pos: { latitude: number; longitude: number; accuracyMeters: number }) =>
      api.post<CheckInResult>(`/tasks/${publicId}/check-in`, pos, { retries: 0, timeoutMs: 15_000 }),
    onSettled: invalidate,
  });
  const release = useMutation({
    mutationFn: () => api.post(`/tasks/${publicId}/release`, undefined, { retries: 0 }),
    onSettled: invalidate,
  });
  return { accept, checkIn, release };
}

/** Geolocalização de alta precisão com tempo-limite de 20 s (RF-17); a posição não fica guardada no aparelho. */
export function locate(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!('geolocation' in navigator)) {
      reject(Object.assign(new Error('unsupported'), { code: 0 }));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 20_000,
      maximumAge: 0,
    });
  });
}
