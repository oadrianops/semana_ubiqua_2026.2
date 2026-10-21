import { use } from 'react';
import { useTranslation } from 'react-i18next';
import { ensureNamespaces, toLanguage } from './config';

const pending = new Map<string, Promise<void>>();

/** Suspende a tela até os namespaces do idioma atual estarem carregados (sem texto cru na tela — RN-19). */
export function useNamespaces(ns: string[]) {
  const { i18n } = useTranslation();
  const lng = toLanguage(i18n.language);
  if (ns.every((n) => i18n.hasResourceBundle(lng, n))) return;
  const key = `${lng}:${ns.join(',')}`;
  let promise = pending.get(key);
  if (!promise) {
    promise = ensureNamespaces(ns, lng);
    pending.set(key, promise);
  }
  use(promise);
}
