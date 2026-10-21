import { useEffect } from 'react';

/** Título da aba por página e no idioma atual (WCAG 2.4.2): "Jambu · NANDESK". */
export function useDocumentTitle(title: string | null | undefined) {
  useEffect(() => {
    if (!title) return;
    const previous = document.title;
    document.title = `${title} · NANDESK`;
    return () => {
      document.title = previous;
    };
  }, [title]);
}
