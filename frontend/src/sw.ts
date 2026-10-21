/// <reference lib="webworker" />
import { clientsClaim } from 'workbox-core';
import { ExpirationPlugin } from 'workbox-expiration';
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';
import { CacheFirst } from 'workbox-strategies';

/**
 * Service worker (9.13, RF-35):
 * - precache do app shell (HTML, JS, CSS, fontes, ícones); navegação do SPA servida do cache;
 * - imagens da API em cache (até 300 itens, 30 dias), só o que a pessoa já viu;
 * - NUNCA toca em /api/v1/me/**, nem nas tiles do OpenStreetMap (9.6);
 * - push e notificationclick; Background Sync avisa as abas abertas para a fila andar.
 * A versão nova só assume depois do toque em "Atualizar" (SKIP_WAITING).
 */
declare const self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: (string | { url: string; revision: string | null })[];
};

cleanupOutdatedCaches();
precacheAndRoute(self.__WB_MANIFEST);

registerRoute(
  new NavigationRoute(createHandlerBoundToURL('/index.html'), {
    denylist: [/^\/api\//, /^\/actuator\//, /^\/config\.js$/],
  }),
);

registerRoute(
  ({ url, request }) =>
    url.origin === self.location.origin &&
    url.pathname.startsWith('/api/v1/files/') &&
    request.destination === 'image',
  new CacheFirst({
    cacheName: 'nd-images',
    plugins: [
      new ExpirationPlugin({ maxEntries: 300, maxAgeSeconds: 30 * 24 * 60 * 60, purgeOnQuotaError: true }),
    ],
  }),
);

self.addEventListener('message', (event) => {
  if ((event.data as { type?: string })?.type === 'SKIP_WAITING') {
    void self.skipWaiting();
  }
});

self.addEventListener('activate', () => {
  clientsClaim();
});

interface PushPayload {
  title?: string;
  body?: string;
  url?: string;
}

self.addEventListener('push', (event) => {
  let payload: PushPayload = {};
  try {
    payload = (event.data?.json() as PushPayload) ?? {};
  } catch {
    payload = { title: 'NANDESK', body: event.data?.text() };
  }
  event.waitUntil(
    self.registration.showNotification(payload.title ?? 'NANDESK', {
      body: payload.body ?? '',
      icon: '/brand/png/pwa-192x192.png',
      badge: '/brand/png/monochrome-icon-512x512.png',
      data: { url: payload.url ?? '/notifications' },
      lang: 'pt-BR',
    }),
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const target = (event.notification.data as { url?: string })?.url ?? '/notifications';
  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      for (const client of windows) {
        if ('focus' in client) {
          await client.focus();
          if ('navigate' in client) await (client as WindowClient).navigate(target);
          return;
        }
      }
      await self.clients.openWindow(target);
    })(),
  );
});

interface SyncEvent extends ExtendableEvent {
  tag: string;
}

self.addEventListener('sync', (event) => {
  const sync = event as SyncEvent;
  if (sync.tag !== 'nandesk-outbox') return;
  sync.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      windows.forEach((client) => client.postMessage({ type: 'OUTBOX_SYNC' }));
    })(),
  );
});
