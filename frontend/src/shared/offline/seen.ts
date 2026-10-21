import { useEffect, useState } from 'react';
import { idb } from './idb';

/**
 * Marca "atualizado" (direção "Lousa de preços", painel de embarque): uma linha cuja versão mudou desde a última
 * vez que a pessoa abriu este box fica marcada até ser vista. Na primeira visita nada é marcado.
 */
export function useFreshRows(scope: string | null, rows: { id: string; version: number }[] | undefined) {
  const [fresh, setFresh] = useState<Set<string>>(new Set());
  const signature = rows?.map((r) => `${r.id}:${r.version}`).join('|') ?? '';

  useEffect(() => {
    if (!scope || !rows?.length) return;
    let alive = true;
    const key = `seen:${scope}`;
    void (async () => {
      const seen = await idb.get<Record<string, number>>('prefs', key);
      if (!alive) return;
      if (seen) {
        setFresh(
          new Set(rows.filter((r) => seen[r.id] !== undefined && seen[r.id] !== r.version).map((r) => r.id)),
        );
      }
      await idb.set('prefs', key, Object.fromEntries(rows.map((r) => [r.id, r.version])));
    })();
    return () => {
      alive = false;
    };
    // A assinatura já resume as linhas e as versões.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [scope, signature]);

  return fresh;
}
