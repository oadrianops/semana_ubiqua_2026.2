/**
 * Compressão no aparelho antes do envio (RN-06): JPEG, lado maior ≤ 1600 px, alvo ≤ 300 KB. Reduz a qualidade
 * em passos e, se preciso, a dimensão, para não depender de uma conexão longa e estável.
 */
export const MAX_DIMENSION = 1600;
export const TARGET_BYTES = 300 * 1024;

export function fitWithin(
  width: number,
  height: number,
  max = MAX_DIMENSION,
): { width: number; height: number } {
  const longest = Math.max(width, height);
  if (longest <= max) return { width, height };
  const ratio = max / longest;
  return { width: Math.round(width * ratio), height: Math.round(height * ratio) };
}

export interface Encoder {
  (bitmap: ImageBitmap | HTMLImageElement, width: number, height: number, quality: number): Promise<Blob>;
}

async function canvasEncode(
  source: ImageBitmap | HTMLImageElement,
  width: number,
  height: number,
  quality: number,
): Promise<Blob> {
  if (typeof OffscreenCanvas !== 'undefined') {
    const canvas = new OffscreenCanvas(width, height);
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('canvas');
    ctx.drawImage(source, 0, 0, width, height);
    return canvas.convertToBlob({ type: 'image/jpeg', quality });
  }
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) throw new Error('canvas');
  ctx.drawImage(source, 0, 0, width, height);
  return new Promise((resolve, reject) =>
    canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('toBlob'))), 'image/jpeg', quality),
  );
}

async function decode(file: Blob): Promise<ImageBitmap | HTMLImageElement> {
  if (typeof createImageBitmap === 'function') {
    return createImageBitmap(file, { imageOrientation: 'from-image' } as ImageBitmapOptions);
  }
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.decoding = 'async';
    img.src = url;
    await img.decode();
    return img;
  } finally {
    URL.revokeObjectURL(url);
  }
}

export async function compressImage(file: Blob, encode: Encoder = canvasEncode): Promise<Blob> {
  const source = await decode(file);
  const naturalWidth = 'naturalWidth' in source ? source.naturalWidth : source.width;
  const naturalHeight = 'naturalHeight' in source ? source.naturalHeight : source.height;
  let { width, height } = fitWithin(naturalWidth, naturalHeight);
  let quality = 0.82;
  let blob = await encode(source, width, height, quality);
  let attempts = 0;
  while (blob.size > TARGET_BYTES && attempts < 6) {
    attempts++;
    if (quality > 0.55) {
      quality -= 0.1;
    } else {
      width = Math.round(width * 0.85);
      height = Math.round(height * 0.85);
    }
    blob = await encode(source, width, height, quality);
  }
  if ('close' in source && typeof source.close === 'function') source.close();
  return blob;
}
