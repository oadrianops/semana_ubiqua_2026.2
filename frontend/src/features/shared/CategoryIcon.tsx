import { Cherry, Droplet, Fish, Leaf, Nut, Shapes, Soup, Tag, UtensilsCrossed, Wheat } from 'lucide-react';

/** Ícone da categoria (lucide, traço 2 px), sempre acompanhado do nome (14.1); sem caixa em volta. */
const ICONS = {
  leaf: Leaf,
  nut: Nut,
  cherry: Cherry,
  wheat: Wheat,
  soup: Soup,
  droplet: Droplet,
  utensils: UtensilsCrossed,
  fish: Fish,
  shapes: Shapes,
} as const;

export function CategoryIcon({ name, size = 22 }: { name: string; size?: number }) {
  const Icon = ICONS[name as keyof typeof ICONS] ?? Tag;
  return <Icon size={size} aria-hidden="true" strokeWidth={2} className="shrink-0 text-brand" />;
}
