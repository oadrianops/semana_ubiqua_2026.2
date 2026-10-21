import type { ReactNode } from 'react';
import { cn } from '../lib/cn';

/**
 * Linha da lousa de preços (assinatura da direção): rótulo, pontilhado e valor em colunas fixas, com números
 * tabulares. O pontilhado preenche o espaço em qualquer largura.
 */
export function PriceRow({
  label,
  value,
  sub,
  fresh,
  className,
  muted,
}: {
  label: ReactNode;
  value: ReactNode;
  sub?: ReactNode;
  fresh?: boolean;
  muted?: boolean;
  className?: string;
}) {
  return (
    <div className={cn('py-3', className)}>
      <div className="price-row">
        <span className={cn('min-w-0 font-semibold text-ink', muted && 'text-support')}>{label}</span>
        <span className="price-row__leader" aria-hidden="true" />
        <span
          className={cn(
            'display tabular text-lg whitespace-nowrap',
            muted ? 'text-muted' : 'text-ink',
            fresh && 'fresh-mark',
          )}
        >
          {value}
        </span>
      </div>
      {sub && <div className="mt-0.5 text-sm text-support">{sub}</div>}
    </div>
  );
}
