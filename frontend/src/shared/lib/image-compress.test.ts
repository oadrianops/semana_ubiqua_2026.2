import { compressImage, fitWithin, TARGET_BYTES } from './image-compress';

describe('RN-06 compressão de fotos no aparelho', () => {
  it('RN-06 reduz o lado maior para 1600 px sem distorcer', () => {
    expect(fitWithin(4000, 3000)).toEqual({ width: 1600, height: 1200 });
    expect(fitWithin(1200, 1600)).toEqual({ width: 1200, height: 1600 });
    expect(fitWithin(800, 600)).toEqual({ width: 800, height: 600 });
  });

  it('RN-06 baixa qualidade e tamanho até ficar abaixo de 300 KB', async () => {
    const calls: { w: number; q: number }[] = [];
    const encoder = vi.fn(async (_s: unknown, w: number, _h: number, q: number) => {
      calls.push({ w, q });
      const size = Math.round(w * 400 * q); // simula: tamanho cai com a qualidade e a largura
      return new Blob([new Uint8Array(size)], { type: 'image/jpeg' });
    });
    globalThis.createImageBitmap = vi.fn(
      async () => ({ width: 3000, height: 2000, close: vi.fn() }) as unknown as ImageBitmap,
    );
    const out = await compressImage(new Blob(['x']), encoder);
    expect(out.size).toBeLessThanOrEqual(TARGET_BYTES);
    expect(calls[0]).toEqual({ w: 1600, q: 0.82 });
    expect(calls.length).toBeGreaterThan(1);
  });
});
