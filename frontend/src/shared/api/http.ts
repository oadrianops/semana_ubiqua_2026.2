import { appConfig } from '../config';

/**
 * Cliente HTTP da API (9.4, 9.14, 12.1):
 * - token de acesso só em memória; em 401, UMA renovação em andamento por vez e a requisição repete uma vez;
 * - Accept-Language do idioma atual;
 * - tempo-limite de 8 s em GET (30 s para fotos) e novas tentativas com espera exponencial e jitter,
 *   no máximo 3, só em operações idempotentes (GET ou com Idempotency-Key).
 */

export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code?: string;
  requestId?: string;
  errors?: { field: string; message: string }[];
  [key: string]: unknown;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? problem.code ?? `HTTP ${problem.status}`);
    this.name = 'ApiError';
    this.status = problem.status;
    this.code = problem.code ?? (problem.status === 0 ? 'NETWORK' : 'HTTP_' + problem.status);
    this.problem = problem;
  }

  get isNetwork(): boolean {
    return this.status === 0;
  }
}

type TokenListener = (token: string | null) => void;

let accessToken: string | null = null;
let refreshInFlight: Promise<string | null> | null = null;
let refreshHandler: (() => Promise<string | null>) | null = null;
let currentLanguage = 'pt-BR';
const listeners = new Set<TokenListener>();

export const tokenStore = {
  get: () => accessToken,
  set(token: string | null) {
    accessToken = token;
    listeners.forEach((l) => l(token));
  },
  subscribe(listener: TokenListener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

export function setRefreshHandler(handler: () => Promise<string | null>) {
  refreshHandler = handler;
}

export function setApiLanguage(lng: string) {
  currentLanguage = lng;
}

/** Uma única renovação simultânea: chamadas em paralelo esperam a mesma promessa (15.3). */
export function refreshOnce(): Promise<string | null> {
  if (!refreshHandler) return Promise.resolve(null);
  if (!refreshInFlight) {
    refreshInFlight = refreshHandler().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  form?: FormData;
  headers?: Record<string, string>;
  idempotencyKey?: string;
  timeoutMs?: number;
  retries?: number;
  signal?: AbortSignal;
  auth?: boolean;
  /** Repassado ao fetch para revalidar com If-None-Match quando o navegador tiver o ETag. */
  cache?: RequestCache;
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export function backoff(attempt: number, base = 400): number {
  const exp = base * 2 ** attempt;
  return Math.round(exp / 2 + Math.random() * exp);
}

function url(path: string): string {
  const base = appConfig().apiBaseUrl.replace(/\/$/, '');
  const relative = `${base}/v1${path}`;
  return typeof window === 'undefined' ? relative : new URL(relative, window.location.origin).toString();
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET';
  const idempotent = method === 'GET' || Boolean(options.idempotencyKey);
  const maxRetries = options.retries ?? (idempotent ? 3 : 0);
  const timeout = options.timeoutMs ?? (options.form ? 30_000 : method === 'GET' ? 8_000 : 15_000);

  let attempt = 0;
  let refreshed = false;
  for (;;) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(new DOMException('timeout', 'TimeoutError')), timeout);
    options.signal?.addEventListener('abort', () => controller.abort(options.signal?.reason), { once: true });
    const headers: Record<string, string> = {
      Accept: 'application/json',
      'Accept-Language': currentLanguage,
      ...options.headers,
    };
    if (options.auth !== false && accessToken) headers.Authorization = `Bearer ${accessToken}`;
    if (options.idempotencyKey) headers['Idempotency-Key'] = options.idempotencyKey;
    let body: BodyInit | undefined;
    if (options.form) {
      body = options.form;
    } else if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(options.body);
    }

    let response: Response;
    try {
      response = await fetch(url(path), {
        method,
        headers,
        body,
        credentials: 'include',
        signal: controller.signal,
        cache: options.cache,
      });
    } catch (error) {
      clearTimeout(timer);
      if (options.signal?.aborted) throw error;
      if (attempt < maxRetries) {
        await sleep(backoff(attempt++));
        continue;
      }
      throw new ApiError({ status: 0, code: 'NETWORK', detail: (error as Error)?.message });
    }
    clearTimeout(timer);

    if (response.status === 401 && options.auth !== false && !refreshed) {
      refreshed = true;
      const token = await refreshOnce();
      if (token) continue;
    }
    if (response.status >= 500 && attempt < maxRetries) {
      await sleep(backoff(attempt++));
      continue;
    }
    if (response.status === 429 && attempt < maxRetries && idempotent) {
      const retryAfter = Number(response.headers.get('Retry-After') ?? '2');
      await sleep(Math.min(10, retryAfter) * 1000);
      attempt++;
      continue;
    }
    if (response.status === 204) {
      return undefined as T;
    }
    const text = await response.text();
    const data = text ? (JSON.parse(text) as unknown) : undefined;
    if (!response.ok) {
      throw new ApiError({ status: response.status, ...(data as object) } as ProblemDetail);
    }
    return data as T;
  }
}

export const api = {
  get: <T>(path: string, opts?: RequestOptions) => request<T>(path, { ...opts, method: 'GET' }),
  post: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(path, { ...opts, method: 'POST', body }),
  put: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(path, { ...opts, method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(path, { ...opts, method: 'PATCH', body }),
  delete: <T>(path: string, opts?: RequestOptions) => request<T>(path, { ...opts, method: 'DELETE' }),
};

/** Busca autenticada de imagem restrita (evidências de envio) como object URL para o <img>. */
export async function fetchImageObjectUrl(apiUrl: string): Promise<string> {
  const headers: Record<string, string> = {};
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const response = await fetch(apiUrl, { headers, credentials: 'include' });
  if (!response.ok) throw new ApiError({ status: response.status });
  return URL.createObjectURL(await response.blob());
}
