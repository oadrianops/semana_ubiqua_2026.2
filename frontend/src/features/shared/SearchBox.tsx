import { Search } from 'lucide-react';
import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { cn } from '../../shared/lib/cn';
import { useNetworkState } from '../../shared/offline/network';
import { useDebounced, useSuggestions } from '../public-api';

/**
 * Busca em destaque sobre a faixa Açaí 900 (RF-02): sugestões a partir de 2 caracteres com 250 ms entre teclas,
 * navegáveis por teclado (combobox ARIA). Sem rede, a busca vai para os dados salvos.
 */
export function SearchBox({
  initial = '',
  size = 'lg',
  focusOnMount = false,
}: {
  initial?: string;
  size?: 'lg' | 'md';
  focusOnMount?: boolean;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  // Na página de busca vazia, o cursor já começa no campo (foco programático, sem o atributo autofocus).
  useEffect(() => {
    if (focusOnMount) inputRef.current?.focus();
  }, [focusOnMount]);
  const { t } = useTranslation();
  const navigate = useNavigate();
  const network = useNetworkState();
  const [text, setText] = useState(initial);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const debounced = useDebounced(text, 250);
  const suggestions = useSuggestions(network === 'offline' ? '' : debounced);
  const items = open ? (suggestions.data ?? []) : [];
  const listId = useId();

  function submit(q: string) {
    const value = q.trim();
    if (!value) return;
    setOpen(false);
    navigate(`/search?q=${encodeURIComponent(value)}`);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const chosen = active >= 0 ? items[active] : undefined;
    if (chosen) navigate(`/products/${chosen.slug}`);
    else submit(text);
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (!items.length) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => (a + 1) % items.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => (a <= 0 ? items.length - 1 : a - 1));
    } else if (e.key === 'Escape') {
      setOpen(false);
      setActive(-1);
    }
  }

  return (
    <form role="search" onSubmit={onSubmit} className="relative">
      <label className="sr-only" htmlFor={`${listId}-input`}>
        {t('search.label')}
      </label>
      <div className="relative">
        <Search
          size={22}
          aria-hidden="true"
          className="pointer-events-none absolute top-1/2 left-4 -translate-y-1/2 text-muted"
        />
        <input
          id={`${listId}-input`}
          type="search"
          inputMode="search"
          enterKeyHint="search"
          autoComplete="off"
          ref={inputRef}
          value={text}
          onChange={(e) => {
            setText(e.target.value);
            setOpen(true);
            setActive(-1);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          onKeyDown={onKeyDown}
          placeholder={t('search.placeholder')}
          role="combobox"
          aria-expanded={items.length > 0}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={active >= 0 ? `${listId}-opt-${active}` : undefined}
          className={cn(
            'w-full rounded-[var(--radius-control)] border-2 border-transparent bg-surface pr-28 pl-12 text-ink placeholder:text-muted focus:border-[var(--tucupi-400)] focus:outline-none',
            size === 'lg' ? 'min-h-15 text-lg' : 'min-h-12 text-base',
          )}
        />
        <button
          type="submit"
          className={cn(
            'absolute top-1/2 right-0.5 -translate-y-1/2 rounded-[8px] bg-brand px-4 font-semibold text-on-brand hover:bg-brand-strong',
            size === 'lg' ? 'min-h-12' : 'min-h-11',
          )}
        >
          {t('search.submit')}
        </button>
      </div>
      {items.length > 0 && (
        <ul
          id={listId}
          role="listbox"
          aria-label={t('search.suggestions')}
          className="absolute inset-x-0 top-full z-20 mt-1 overflow-hidden rounded-[var(--radius-control)] border border-line bg-surface text-ink shadow-[var(--shadow-dialog)]"
        >
          {items.map((s, i) => (
            <li
              key={s.slug}
              id={`${listId}-opt-${i}`}
              role="option"
              aria-selected={i === active}
              onMouseDown={(e) => {
                e.preventDefault();
                navigate(`/products/${s.slug}`);
              }}
              className={cn(
                'flex min-h-11 cursor-pointer items-center px-4 py-2',
                i === active && 'bg-brand-soft',
              )}
            >
              {s.name}
            </li>
          ))}
        </ul>
      )}
    </form>
  );
}
