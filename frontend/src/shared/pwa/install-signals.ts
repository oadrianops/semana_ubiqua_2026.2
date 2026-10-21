import { idb } from '../offline/idb';

/** Sinais de uso real para o convite de instalação (RF-35): segunda visita ou busca com resultado. */
const VISITS_KEY = 'visits';
const SEARCH_HIT_KEY = 'search-hit';
const listeners = new Set<() => void>();
let counted = false;

async function countVisit() {
  if (counted) return;
  counted = true;
  const visits = ((await idb.get<number>('prefs', VISITS_KEY)) ?? 0) + 1;
  await idb.set('prefs', VISITS_KEY, visits);
}

export const installSignals = {
  async qualifies(): Promise<boolean> {
    await countVisit();
    const visits = (await idb.get<number>('prefs', VISITS_KEY)) ?? 0;
    const searchHit = Boolean(await idb.get<boolean>('prefs', SEARCH_HIT_KEY));
    return visits >= 2 || searchHit;
  },
  async searchWithResult() {
    await idb.set('prefs', SEARCH_HIT_KEY, true);
    listeners.forEach((l) => l());
  },
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
};
