import { useTranslation } from 'react-i18next';
import { useAuth } from '../shared/auth/AuthProvider';
import { api } from '../shared/api/http';
import { changeLanguage, LANGUAGE_NAMES, LANGUAGES, toLanguage, type Language } from '../shared/i18n/config';

const SHORT: Record<Language, string> = { 'pt-BR': 'PT', en: 'EN', es: 'ES' };

/** Seletor de idioma visível em todas as páginas (RF-06); salvo no aparelho ou no perfil. */
export function LanguageSwitch() {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const current = toLanguage(i18n.language);

  async function select(lng: Language) {
    await changeLanguage(lng);
    if (user) void api.patch('/me', { locale: lng }).catch(() => undefined);
  }

  return (
    <label className="relative inline-flex min-h-11 items-center">
      <span className="sr-only">{t('nav.language')}</span>
      <select
        value={current}
        onChange={(e) => void select(e.target.value as Language)}
        className="min-h-11 cursor-pointer appearance-none rounded-[var(--radius-control)] border border-on-band-muted/50 bg-transparent px-3 text-base font-semibold text-on-band"
        aria-label={t('nav.language')}
      >
        {LANGUAGES.map((lng) => (
          <option key={lng} value={lng} lang={lng} className="text-ink">
            {SHORT[lng]} · {LANGUAGE_NAMES[lng]}
          </option>
        ))}
      </select>
    </label>
  );
}
