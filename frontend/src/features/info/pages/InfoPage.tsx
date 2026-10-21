import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { appConfig } from '../../../shared/config';

type Page = 'about' | 'vendors' | 'privacy' | 'terms';

/**
 * Páginas institucionais (14.2): Sobre, Sou feirante, Privacidade e Termos. Os textos jurídicos são rascunhos
 * (docs/legal) e precisam de revisão antes de um lançamento público (13).
 */
export default function InfoPage({ page }: { page: Page }) {
  const { t } = useTranslation('public');
  const sections = t(`info.${page}.sections`, { returnObjects: true, email: appConfig().contactEmail }) as {
    title: string;
    body: string;
  }[];
  return (
    <article className="safe-x mx-auto grid max-w-3xl gap-6 py-8">
      <header className="grid gap-2">
        <h1 className="text-3xl">{t(`info.${page}.title`)}</h1>
        <p className="max-w-[var(--measure)] text-lg text-support">
          {t(`info.${page}.lead`, { email: appConfig().contactEmail })}
        </p>
      </header>
      {Array.isArray(sections) &&
        sections.map((s) => (
          <section key={s.title} className="grid gap-2 border-t border-line pt-4">
            <h2 className="text-xl">{s.title}</h2>
            <p className="max-w-[var(--measure)] whitespace-pre-line text-ink">{s.body}</p>
          </section>
        ))}
      {page === 'about' && (
        <p>
          <Link to="/signup" className="font-semibold">
            {t('info.about.cta')}
          </Link>
        </p>
      )}
      {page === 'vendors' && (
        <p>
          <a href={`mailto:${appConfig().contactEmail}`} className="font-semibold">
            {t('info.vendors.cta', { email: appConfig().contactEmail })}
          </a>
        </p>
      )}
    </article>
  );
}
