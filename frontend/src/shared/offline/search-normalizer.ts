/**
 * Normalização da busca local (9.13), idêntica à do servidor (SearchNormalizer.java): trim → minúsculas →
 * sem acentos (NFD sem marcas) → hífens e pontuação viram espaço → espaços repetidos viram um.
 * Os casos de teste ficam em contracts/search-normalization-cases.json, compartilhado com o backend.
 */
export function normalize(input: string | null | undefined): string {
  if (!input) return '';
  return input
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

export function tokens(input: string): string[] {
  const n = normalize(input);
  return n ? n.split(' ') : [];
}

/** Pontuação igual à do servidor (SearchRanking.java): 100, 80, 60, 40 e +10 pelo idioma. */
export function score(normalizedTerm: string, normalizedQuery: string, sameLocale = false): number {
  if (!normalizedQuery) return 0;
  const base = baseScore(normalizedTerm, normalizedQuery);
  return base === 0 ? 0 : base + (sameLocale ? 10 : 0);
}

function baseScore(term: string, query: string): number {
  if (term === query) return 100;
  if (term.startsWith(query)) return 80;
  if (term.split(' ').some((w) => w.startsWith(query))) return 60;
  if (term.includes(query)) return 40;
  const parts = query.split(' ');
  if (parts.length > 1 && parts.every((p) => term.includes(p))) return 40;
  return 0;
}
