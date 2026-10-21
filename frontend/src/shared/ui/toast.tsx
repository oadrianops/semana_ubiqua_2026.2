import { X } from 'lucide-react';
import { useSyncExternalStore } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '../lib/cn';

/**
 * Avisos curtos ("Tarefa aceita.", "Sem sinal agora…"), anunciados ao leitor de tela: sucesso e informação em
 * região polida, erro como alerta. Componente próprio de ~1 KB para caber no orçamento da primeira tela (9.14).
 */
type Tone = 'success' | 'error' | 'info';

interface Item {
  id: number;
  tone: Tone;
  text: string;
}

const MAX = 3;
let items: Item[] = [];
let seq = 0;
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((l) => l());
}

function dismiss(id: number) {
  items = items.filter((i) => i.id !== id);
  emit();
}

function push(tone: Tone, text: string) {
  const id = ++seq;
  items = [...items.filter((i) => i.text !== text), { id, tone, text }].slice(-MAX);
  emit();
  setTimeout(() => dismiss(id), tone === 'error' ? 7000 : 4500);
  return id;
}

export const toast = {
  success: (text: string) => push('success', text),
  error: (text: string) => push('error', text),
  message: (text: string) => push('info', text),
  dismiss,
};

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

const getItems = () => items;

export function Toaster() {
  const { t } = useTranslation();
  const list = useSyncExternalStore(subscribe, getItems, getItems);
  return (
    <div className="pointer-events-none fixed inset-x-0 top-[max(0.75rem,env(safe-area-inset-top))] z-[60] grid justify-items-center gap-2 px-4">
      <div role="status" aria-live="polite" className="contents">
        {list
          .filter((i) => i.tone !== 'error')
          .map((i) => (
            <ToastCard key={i.id} item={i} closeLabel={t('action.close')} />
          ))}
      </div>
      <div role="alert" className="contents">
        {list
          .filter((i) => i.tone === 'error')
          .map((i) => (
            <ToastCard key={i.id} item={i} closeLabel={t('action.close')} />
          ))}
      </div>
    </div>
  );
}

function ToastCard({ item, closeLabel }: { item: Item; closeLabel: string }) {
  return (
    <div
      className={cn(
        'toast-in pointer-events-auto flex w-full max-w-md items-start gap-3 rounded-[var(--radius-panel)] border bg-surface py-2 pr-1 pl-4 text-ink shadow-[var(--shadow-sheet)]',
        item.tone === 'error' ? 'border-danger' : item.tone === 'success' ? 'border-success' : 'border-line',
      )}
    >
      <p className="flex-1 py-2 text-base">{item.text}</p>
      <button
        type="button"
        onClick={() => dismiss(item.id)}
        aria-label={closeLabel}
        className="inline-flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-support hover:bg-soft"
      >
        <X size={18} aria-hidden="true" />
      </button>
    </div>
  );
}
