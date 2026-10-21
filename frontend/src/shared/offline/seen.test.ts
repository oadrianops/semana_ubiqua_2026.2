import { renderHook, waitFor } from '@testing-library/react';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { idb } from './idb';
import { useFreshRows } from './seen';

describe('direção "Lousa de preços": marca "atualizado"', () => {
  it('RF-03 na primeira visita nada é marcado; na seguinte, só a linha cuja versão mudou', async () => {
    await idb.del('prefs', 'seen:stall:s1');
    const first = renderHook(({ rows }) => useFreshRows('stall:s1', rows), {
      initialProps: {
        rows: [
          { id: 'a', version: 1 },
          { id: 'b', version: 1 },
        ],
      },
    });
    await waitFor(async () => expect(await idb.get('prefs', 'seen:stall:s1')).toEqual({ a: 1, b: 1 }));
    expect(first.result.current.size).toBe(0);
    first.unmount();

    const second = renderHook(() =>
      useFreshRows('stall:s1', [
        { id: 'a', version: 2 },
        { id: 'b', version: 1 },
      ]),
    );
    await waitFor(() => expect([...second.result.current]).toEqual(['a']));
  });

  it('WCAG 2.4.2 o título da aba acompanha a tela e volta ao sair', () => {
    document.title = 'NANDESK: feiras de Belém';
    const { unmount } = renderHook(() => useDocumentTitle('Jambu'));
    expect(document.title).toBe('Jambu · NANDESK');
    unmount();
    expect(document.title).toBe('NANDESK: feiras de Belém');
  });
});
