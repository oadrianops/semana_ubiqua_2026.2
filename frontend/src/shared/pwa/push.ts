import { api } from '../api/http';
import { appConfig } from '../config';

/**
 * Inscrição de Web Push (9.8, RF-14). No iPhone só funciona com o app instalado na tela inicial (iOS 16.4+);
 * a interface explica isso.
 */
export type PushSupport = 'supported' | 'needs-install' | 'unsupported';

export function pushSupport(): PushSupport {
  if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) {
    const ios = /iphone|ipad|ipod/i.test(navigator.userAgent);
    return ios ? 'needs-install' : 'unsupported';
  }
  return 'supported';
}

function urlBase64ToUint8Array(base64: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4);
  const raw = atob((base64 + padding).replace(/-/g, '+').replace(/_/g, '/'));
  const out = new Uint8Array(new ArrayBuffer(raw.length));
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}

export async function subscribePush(): Promise<'subscribed' | 'denied' | 'disabled'> {
  const server = await api.get<{ enabled: boolean; publicKey: string }>('/me/push-subscriptions/public-key');
  const key = server.publicKey || appConfig().vapidPublicKey;
  if (!server.enabled || !key) return 'disabled';
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return 'denied';
  const registration = await navigator.serviceWorker.ready;
  const subscription =
    (await registration.pushManager.getSubscription()) ??
    (await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(key),
    }));
  const json = subscription.toJSON() as { endpoint: string; keys: { p256dh: string; auth: string } };
  await api.post('/me/push-subscriptions', { endpoint: json.endpoint, keys: json.keys });
  return 'subscribed';
}

export async function isPushSubscribed(): Promise<boolean> {
  if (pushSupport() !== 'supported') return false;
  const registration = await navigator.serviceWorker.getRegistration();
  return Boolean(await registration?.pushManager.getSubscription());
}
