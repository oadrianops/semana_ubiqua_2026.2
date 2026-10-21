import { ChevronDown } from 'lucide-react';
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

  // No cabeçalho aparece só a sigla (cabe em 360 px); o select nativo, invisível por cima, mantém os nomes
  // completos, o teclado e o leitor de tela.
  return (
    <label className="relative inline-flex min-h-11 min-w-11 shrink-0 items-center justify-center gap-1 rounded-[var(--radius-control)] border border-on-band-muted/50 px-2.5 font-semibold text-on-band focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-[var(--color-focus-on-band)] hover:bg-band-hover">
      <span aria-hidden="true">{SHORT[current]}</span>
      <ChevronDown size={16} aria-hidden="true" />
      <select
        value={current}
        onChange={(e) => void select(e.target.value as Language)}
        className="absolute inset-0 cursor-pointer appearance-none opacity-0"
        aria-label={t('nav.language')}
      >
        {LANGUAGES.map((lng) => (
          <option key={lng} value={lng} lang={lng}>
            {SHORT[lng]} · {LANGUAGE_NAMES[lng]}
          </option>
        ))}
      </select>
    </label>
  );
}
