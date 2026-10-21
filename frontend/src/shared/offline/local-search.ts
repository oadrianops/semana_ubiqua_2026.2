import type { SearchItem, MarketSnapshot } from '../api/types';
import { normalize, score, tokens } from './search-normalizer';

/**
 * Busca local nas feiras salvas (RF-36): mesma normalização e pontuação do servidor; resultado no mesmo
 * formato da busca online (produto → feira → boxes), com boxes verificados mais recentemente primeiro.
 */
export interface LocalSearchResult {
  items: SearchItem[];
  oldestSavedAt: string | null;
}

export function searchSnapshots(
  query: string,
  snapshots: { data: MarketSnapshot; savedAt: string }[],
  options: { availableOnly?: boolean; marketSlug?: string | null; categorySlug?: string | null } = {},
): LocalSearchResult {
  const q = normalize(query);
  if (!q || snapshots.length === 0) return { items: [], oldestSavedAt: null };
  const longest = tokens(q).sort((a, b) => b.length - a.length)[0] ?? q;
  const bySlug = new Map<string, { item: SearchItem; score: number; available: number }>();

  for (const { data } of snapshots) {
    if (options.marketSlug && data.market.slug !== options.marketSlug) continue;
    const products = new Map(data.products.map((p) => [p.slug, p]));
    const scores = new Map<string, number>();
    for (const p of data.products) {
      if (options.categorySlug && p.categorySlug !== options.categorySlug) continue;
      const best = Math.max(
        0,
        ...p.terms.filter((t) => t.includes(longest) || t.startsWith(q)).map((t) => score(t, q)),
      );
      if (best > 0) scores.set(p.slug, best);
    }
    for (const stall of data.stalls) {
      for (const o of stall.offerings) {
        const s = scores.get(o.productSlug);
        if (!s) continue;
        if (options.availableOnly && o.availability === 'OUT_OF_STOCK') continue;
        const product = products.get(o.productSlug);
        if (!product) continue;
        const category = data.categories.find((c) => c.slug === product.categorySlug);
        let entry = bySlug.get(o.productSlug);
        if (!entry) {
          entry = {
            item: {
              product: {
                slug: product.slug,
                name: product.name,
                category: { slug: product.categorySlug, name: category?.name ?? product.categorySlug },
                imageUrl: product.imageUrl,
              },
              markets: [],
            },
            score: s,
            available: 0,
          };
          bySlug.set(o.productSlug, entry);
        }
        let market = entry.item.markets.find((m) => m.market.slug === data.market.slug);
        if (!market) {
          market = { market: { slug: data.market.slug, name: data.market.name }, stalls: [] };
          entry.item.markets.push(market);
        }
        market.stalls.push({
          publicId: stall.publicId,
          code: stall.code,
          name: stall.name,
          sector: stall.sectorName,
          price: o.price,
          priceUnit: o.priceUnit,
          availability: o.availability,
          lastVerifiedAt: o.lastVerifiedAt,
        });
        if (o.availability === 'AVAILABLE') entry.available++;
      }
    }
  }

  const items = [...bySlug.values()]
    .sort(
      (a, b) =>
        b.score - a.score ||
        b.available - a.available ||
        normalize(a.item.product.name).localeCompare(normalize(b.item.product.name)),
    )
    .map((e) => {
      e.item.markets.forEach((m) =>
        m.stalls.sort((x, y) => y.lastVerifiedAt.localeCompare(x.lastVerifiedAt)),
      );
      e.item.markets.sort((x, y) => x.market.name.localeCompare(y.market.name));
      return e.item;
    });
  const oldestSavedAt = snapshots.map((s) => s.savedAt).sort()[0] ?? null;
  return { items, oldestSavedAt };
}
