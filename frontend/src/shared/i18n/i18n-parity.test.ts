/**
 * Paridade de chaves (CA-3, RNF-08): os três idiomas têm exatamente as mesmas chaves, sem valor vazio.
 */
const files = import.meta.glob('./locales/*/*.json', { eager: true, import: 'default' }) as Record<
  string,
  Record<string, unknown>
>;

function flatten(obj: Record<string, unknown>, prefix = ''): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(obj)) {
    const key = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === 'object') Object.assign(out, flatten(v as Record<string, unknown>, key));
    else out[key] = String(v ?? '');
  }
  return out;
}

const byLang: Record<string, Record<string, Record<string, string>>> = {};
for (const [path, content] of Object.entries(files)) {
  const [, lng, file] = path.match(/locales\/([^/]+)\/([^/]+)\.json$/) ?? [];
  if (!lng || !file) continue;
  byLang[lng] ??= {};
  byLang[lng][file] = flatten(content);
}

describe('CA-3 paridade de chaves de tradução', () => {
  const namespaces = Object.keys(byLang['pt-BR'] ?? {});

  it('CA-3 existem os três idiomas e todos os namespaces', () => {
    expect(Object.keys(byLang).sort()).toEqual(['en', 'es', 'pt-BR']);
    expect(namespaces.length).toBeGreaterThan(0);
    for (const lng of ['en', 'es']) {
      expect(Object.keys(byLang[lng] ?? {}).sort()).toEqual([...namespaces].sort());
    }
  });

  it.each(['en', 'es'])('CA-3 %s tem as mesmas chaves do PT-BR', (lng) => {
    for (const ns of namespaces) {
      const base = Object.keys(byLang['pt-BR']![ns]!)
        .filter((k) => !/_(zero|one|two|few|many|other)$/.test(k))
        .sort();
      const other = Object.keys(byLang[lng]![ns] ?? {})
        .filter((k) => !/_(zero|one|two|few|many|other)$/.test(k))
        .sort();
      expect({ ns, keys: other }).toEqual({ ns, keys: base });
    }
  });

  it.each(['pt-BR', 'en', 'es'])('CA-3 nenhum valor vazio em %s', (lng) => {
    for (const ns of namespaces) {
      const empty = Object.entries(byLang[lng]![ns] ?? {}).filter(([, v]) => !v.trim());
      expect({ ns, empty }).toEqual({ ns, empty: [] });
    }
  });

  it('CA-3 plurais existem nos três idiomas', () => {
    for (const ns of namespaces) {
      const plurals = (lng: string) =>
        Object.keys(byLang[lng]![ns] ?? {})
          .filter((k) => /_(one|other)$/.test(k))
          .map((k) => k.replace(/_(one|other)$/, ''))
          .sort();
      expect(new Set(plurals('en'))).toEqual(new Set(plurals('pt-BR')));
      expect(new Set(plurals('es'))).toEqual(new Set(plurals('pt-BR')));
    }
  });
});
