import { X } from 'lucide-react';
import { Dialog } from 'radix-ui';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

/**
 * Folha que sobe de baixo no celular e vira diálogo centrado na tela larga (Radix Dialog: foco preso, Esc
 * fecha, título anunciado). Única superfície com sombra (14.6).
 */
export function Sheet({
  open,
  onOpenChange,
  title,
  description,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  children: ReactNode;
}) {
  const { t } = useTranslation();
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-40 bg-overlay" />
        <Dialog.Content className="fixed inset-x-0 bottom-0 z-50 max-h-[90dvh] overflow-y-auto rounded-t-[var(--radius-panel)] bg-surface p-4 pb-[max(1rem,var(--safe-bottom))] shadow-[var(--shadow-sheet)] sm:inset-x-auto sm:top-1/2 sm:bottom-auto sm:left-1/2 sm:w-[min(32rem,calc(100vw-2rem))] sm:-translate-x-1/2 sm:-translate-y-1/2 sm:rounded-[var(--radius-panel)] sm:shadow-[var(--shadow-dialog)]">
          <div className="mb-3 flex items-start justify-between gap-3">
            <div>
              <Dialog.Title className="text-xl">{title}</Dialog.Title>
              {description ? (
                <Dialog.Description className="mt-1 text-support">{description}</Dialog.Description>
              ) : (
                <Dialog.Description className="sr-only">{title}</Dialog.Description>
              )}
            </div>
            <Dialog.Close
              className="inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-support hover:bg-soft"
              aria-label={t('action.close')}
            >
              <X size={20} aria-hidden="true" />
            </Dialog.Close>
          </div>
          {children}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
