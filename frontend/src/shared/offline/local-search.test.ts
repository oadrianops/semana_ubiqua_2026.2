import type { MarketSnapshot } from '../api/types';
import { searchSnapshots } from './local-search';
import { isSnapshotStale } from './snapshots';

function snapshot(): MarketSnapshot {
  return {
    market: {
      slug: 'ver-o-peso',
      name: 'Ver-o-Peso',
      description: null,
      howToGetThere: null,
      address: '',
      latitude: -1.45,
      longitude: -48.5,
      geofenceRadiusMeters: 250,
      openingHours: [],
      publishedStalls: 2,
      coverUrl: null,
      dataVersion: 3,
      sectors: [],
    },
    dataVersion: 3,
    locale: 'pt-BR',
    generatedAt: '2026-10-19T18:40:00Z',
    categories: [{ slug: 'temperos-e-molhos', name: 'Temperos e molhos' }],
    products: [
      {
        slug: 'jambu',
        name: 'Jambu',
        categorySlug: 'temperos-e-molhos',
        imageUrl: null,
        terms: ['jambu', 'paracress'],
      },
      {
        slug: 'tucupi',
        name: 'Tucupi',
        categorySlug: 'temperos-e-molhos',
        imageUrl: null,
        terms: ['tucupi', 'tucupi cassava broth'],
      },
    ],
    stalls: [
      {
        publicId: 'a',
        code: '18',
        name: 'Farinhas do Guamá',
        vendorName: null,
        sectorSlug: 'castanhas-e-farinhas',
        sectorName: 'Castanhas e farinhas',
        photoThumbUrl: null,
        lastVerifiedAt: '2026-10-01T10:00:00Z',
        outdated: false,
        offerings: [
          {
            productSlug: 'jambu',
            price: { amount: '3.00', currency: 'BRL' },
            priceUnit: 'BUNCH',
            availability: 'AVAILABLE',
            lastVerifiedAt: '2026-10-01T10:00:00Z',
          },
          {
            productSlug: 'tucupi',
            price: null,
            priceUnit: 'BOTTLE',
            availability: 'OUT_OF_STOCK',
            lastVerifiedAt: '2026-10-01T10:00:00Z',
          },
        ],
      },
      {
        publicId: 'b',
        code: '7',
        name: 'Hortifrúti',
        vendorName: null,
        sectorSlug: null,
        sectorName: null,
        photoThumbUrl: null,
        lastVerifiedAt: '2026-10-15T10:00:00Z',
        outdated: false,
        offerings: [
          {
            productSlug: 'jambu',
            price: { amount: '2.50', currency: 'BRL' },
            priceUnit: 'BUNCH',
            availability: 'AVAILABLE',
            lastVerifiedAt: '2026-10-15T10:00:00Z',
          },
        ],
      },
    ],
  };
}

describe('RF-36 busca sem internet nas feiras salvas', () => {
  it('RF-36 busca "jambu" e mostra os boxes do Ver-o-Peso, mais recentes primeiro', () => {
    const result = searchSnapshots('jambu', [{ data: snapshot(), savedAt: '2026-10-19T18:40:00Z' }]);
    expect(result.items).toHaveLength(1);
    expect(result.items[0]?.markets[0]?.market.name).toBe('Ver-o-Peso');
    expect(result.items[0]?.markets[0]?.stalls.map((s) => s.code)).toEqual(['7', '18']);
    expect(result.oldestSavedAt).toBe('2026-10-19T18:40:00Z');
  });

  it('RF-36 encontra por sinônimo em outro idioma e ignora acentos', () => {
    expect(searchSnapshots('PARACRESS', [{ data: snapshot(), savedAt: 'x' }]).items[0]?.product.slug).toBe(
      'jambu',
    );
    expect(searchSnapshots('tucupí', [{ data: snapshot(), savedAt: 'x' }]).items[0]?.product.slug).toBe(
      'tucupi',
    );
  });

  it('RF-02 filtro "somente disponíveis"', () => {
    expect(
      searchSnapshots('tucupi', [{ data: snapshot(), savedAt: 'x' }], { availableOnly: true }).items,
    ).toHaveLength(0);
  });

  it('RF-36 sem feiras salvas, sem resultados', () => {
    expect(searchSnapshots('jambu', []).items).toEqual([]);
  });

  it('RN-28 dados salvos há mais de 7 dias estão possivelmente desatualizados', () => {
    const now = new Date('2026-10-20T12:00:00Z');
    expect(isSnapshotStale('2026-10-12T11:00:00Z', now)).toBe(true);
    expect(isSnapshotStale('2026-10-15T12:00:00Z', now)).toBe(false);
  });
});
