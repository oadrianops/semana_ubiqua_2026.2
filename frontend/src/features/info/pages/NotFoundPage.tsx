import { useTranslation } from 'react-i18next';
import { ButtonLink } from '../../../shared/ui/Button';

export default function NotFoundPage() {
  const { t } = useTranslation('public');
  return (
    <div className="safe-x mx-auto grid max-w-3xl gap-4 py-12">
      <h1 className="text-3xl">{t('notFound.title')}</h1>
      <p className="text-support">{t('notFound.body')}</p>
      <div className="flex flex-wrap gap-2">
        <ButtonLink to="/">{t('notFound.home')}</ButtonLink>
        <ButtonLink to="/search" variant="secondary">
          {t('notFound.search')}
        </ButtonLink>
      </div>
    </div>
  );
}
