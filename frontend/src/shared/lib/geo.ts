/** "-1.444643, -48.467934" (como sai do mapa) → [latitude, longitude]; nulo se não for uma posição válida. */
export function parsePosition(text: string): [number, number] | null {
  const match = text.trim().match(/^(-?\d{1,2}(?:\.\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:\.\d+)?)$/);
  if (!match) return null;
  const lat = Number(match[1]);
  const lng = Number(match[2]);
  return Math.abs(lat) <= 90 && Math.abs(lng) <= 180 ? [lat, lng] : null;
}
