import { appConfig } from '../config';
import { connectionType } from '../offline/network';

/**
 * Medição de campo (9.14, RNF-02): LCP, INP, CLS e TTFB de uma amostra das sessões, sem dados pessoais, enviados em
 * lote para /telemetry/web-vitals. Carregado depois da primeira pintura para não pesar no caminho crítico.
 */
const ROUTE_PATTERNS: [RegExp, string][] = [
  [/^\/$/, '/'],
  [/^\/search/, '/search'],
  [/^\/markets\/[^/]+/, '/markets/:slug'],
  [/^\/markets$/, '/markets'],
  [/^\/stalls\//, '/stalls/:id'],
  [/^\/products\//, '/products/:slug'],
  [/^\/categories\//, '/categories/:slug'],
  [/^\/collaborate\/tasks\//, '/collaborate/tasks/:id'],
  [/^\/collaborate/, '/collaborate'],
  [/^\/admin/, '/admin'],
];

function routePattern(path: string): string {
  return ROUTE_PATTERNS.find(([re]) => re.test(path))?.[1] ?? 'other';
}

export function startVitals() {
  const rate = appConfig().webVitalsSampleRate;
  if (Math.random() >= rate) return;
  const run = () =>
    void import('web-vitals').then(({ onLCP, onINP, onCLS, onTTFB }) => {
      const batch: { name: string; value: number; route: string; connection: string }[] = [];
      const push = (m: { name: string; value: number }) => {
        batch.push({
          name: m.name,
          value: m.value,
          route: routePattern(location.pathname),
          connection: connectionType(),
        });
      };
      onLCP(push);
      onINP(push);
      onCLS(push);
      onTTFB(push);
      const flush = () => {
        if (!batch.length) return;
        const body = JSON.stringify({ metrics: batch.splice(0, 10) });
        void fetch(`${appConfig().apiBaseUrl}/v1/telemetry/web-vitals`, {
          method: 'POST',
          body,
          headers: { 'Content-Type': 'application/json' },
          keepalive: true,
        }).catch(() => undefined);
      };
      addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden') flush();
      });
    });
  if ('requestIdleCallback' in window) requestIdleCallback(run, { timeout: 5000 });
  else setTimeout(run, 3000);
}
