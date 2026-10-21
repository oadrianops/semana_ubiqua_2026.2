import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import { setApiLanguage } from '../api/http';
import commonPt from './locales/pt-BR/common.json';
import publicPt from './locales/pt-BR/public.json';

/**
 * i18n (RF-06, RNF-08): PT-BR, EN e ES com fallback PT-BR (RN-19). O PT-BR da vitrine vai no pacote inicial; os
 * demais idiomas e namespaces carregam sob demanda (9.14). O idioma escolhido fica salvo no aparelho.
 */
export const LANGUAGES = ['pt-BR', 'en', 'es'] as const;
export type Language = (typeof LANGUAGES)[number];
/** Nome de cada idioma nele mesmo (não se traduz). */
export const LANGUAGE_NAMES: Record<Language, string> = {
  'pt-BR': 'Português',
  en: 'English',
  es: 'Español',
};
export const NAMESPACES = ['common', 'public', 'account', 'collab', 'admin'] as const;

const loaders = import.meta.glob('./locales/*/*.json', { import: 'default' }) as Record<
  string,
  () => Promise<Record<string, unknown>>
>;

export function toLanguage(lng: string | undefined): Language {
  if (!lng) return 'pt-BR';
  const l = lng.toLowerCase();
  if (l.startsWith('en')) return 'en';
  if (l.startsWith('es')) return 'es';
  return 'pt-BR';
}

async function loadBundle(lng: Language, ns: string) {
  if (i18n.hasResourceBundle(lng, ns)) return;
  const loader = loaders[`./locales/${lng}/${ns}.json`];
  if (!loader) return;
  i18n.addResourceBundle(lng, ns, await loader(), true, true);
}

export async function ensureNamespaces(ns: readonly string[], lng: string = i18n.language) {
  const language = toLanguage(lng);
  await Promise.all(
    ns.flatMap((n) => [loadBundle(language, n), language === 'pt-BR' ? null : loadBundle('pt-BR', n)]),
  );
}

function applyDocumentLanguage(lng: string) {
  const language = toLanguage(lng);
  document.documentElement.lang = language;
  setApiLanguage(language);
}

export async function initI18n() {
  await i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      resources: { 'pt-BR': { common: commonPt, public: publicPt } },
      fallbackLng: 'pt-BR',
      supportedLngs: [...LANGUAGES],
      nonExplicitSupportedLngs: true,
      load: 'currentOnly',
      ns: ['common', 'public'],
      defaultNS: 'common',
      fallbackNS: 'common',
      partialBundledLanguages: true,
      interpolation: { escapeValue: false },
      returnNull: false,
      detection: {
        order: ['localStorage', 'navigator', 'htmlTag'],
        lookupLocalStorage: 'nd.lang',
        caches: ['localStorage'],
        convertDetectedLanguage: (lng: string) => toLanguage(lng),
      },
      react: { useSuspense: true },
    });
  const language = toLanguage(i18n.language);
  await ensureNamespaces(['common', 'public'], language);
  if (i18n.language !== language) await i18n.changeLanguage(language);
  applyDocumentLanguage(language);
  i18n.on('languageChanged', (lng) => {
    applyDocumentLanguage(lng);
    void ensureNamespaces(NAMESPACES, lng);
  });
  return i18n;
}

export async function changeLanguage(lng: Language) {
  await ensureNamespaces(
    NAMESPACES.filter((n) => i18n.hasResourceBundle('pt-BR', n)),
    lng,
  );
  await i18n.changeLanguage(lng);
}

export default i18n;
