import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { ErrorState, LoadingRows } from '../../../shared/ui/States';
import { useCategory } from '../../public-api';
import { CategoryIcon } from '../../shared/CategoryIcon';

/** Página da categoria (RF-05): produtos com o número de boxes que os oferecem. */
export default function CategoryPage() {
  const { slug = '' } = useParams();
  const { t } = useTranslation('public');
  const category = useCategory(slug);

  if (!category.data) {
    return (
      <div className="safe-x mx-auto max-w-[var(--content-max)] py-8">
        {category.isPending ? (
          <LoadingRows rows={6} />
        ) : (
          <ErrorState error={category.error} onRetry={() => void category.refetch()} />
        )}
      </div>
    );
  }
  const { category: c, products } = category.data;
  return (
    <div className="safe-x mx-auto grid max-w-3xl gap-6 py-6">
      <header className="flex items-center gap-3">
        <CategoryIcon name={c.icon} size={32} />
        <h1 className="text-3xl">{c.name}</h1>
      </header>
      <ul className="divide-y divide-line border-t-2 border-ink">
        {products.map((p) => (
          <li key={p.slug}>
            <Link
              to={`/products/${p.slug}`}
              className="flex min-h-14 items-center justify-between gap-3 py-3 text-ink no-underline hover:bg-soft"
            >
              <span className="font-semibold">{p.name}</span>
              <span className={p.stallCount ? 'tabular text-brand' : 'text-sm text-muted'}>
                {p.stallCount ? t('category.stallsCount', { count: p.stallCount }) : t('category.noStalls')}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
