// Orçamentos de performance da seção 9.14 que o size-limit não mede sozinho:
// - JS inicial = o script de entrada + tudo que o index.html pré-carrega (≤ 150 KB gzip);
// - cada chunk sob demanda (≤ 80 KB gzip);
// - transferência da primeira visita à página inicial: HTML + JS + CSS + fontes pré-carregadas (≤ 400 KB).
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';

const KB = 1024;
const dir = 'dist/assets';
const gz = (file) => gzipSync(readFileSync(file)).length;
const fmt = (n) => `${(n / KB).toFixed(1)} KB`;
let failed = false;

function check(label, size, limit) {
  const ok = size <= limit;
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${label}: ${fmt(size)} (limite ${fmt(limit)})`);
  if (!ok) failed = true;
}

const html = readFileSync('dist/index.html', 'utf8');
const initialJs = [...html.matchAll(/(?:href|src)="\/assets\/([^"]+\.js)"/g)].map((m) => m[1]);
const initialCss = [...html.matchAll(/href="\/assets\/([^"]+\.css)"/g)].map((m) => m[1]);
const preloadedFonts = [...html.matchAll(/href="\/assets\/([^"]+\.woff2)"/g)].map((m) => m[1]);

const jsTotal = initialJs.reduce((sum, f) => sum + gz(join(dir, f)), 0);
check(`JS inicial (${initialJs.length} arquivos, gzip)`, jsTotal, 150 * KB);

const lazy = readdirSync(dir).filter((f) => f.endsWith('.js') && !initialJs.includes(f) && !/^(MapView|sw|workbox-window\.prod\.es5)-/.test(f));
const biggest = lazy.map((f) => ({ f, size: gz(join(dir, f)) })).sort((a, b) => b.size - a.size);
for (const { f, size } of biggest) if (size > 80 * KB) check(`chunk ${f}`, size, 80 * KB);
console.log(`ok   ${lazy.length} chunks sob demanda ≤ 80 KB gzip (maior: ${biggest[0]?.f} ${fmt(biggest[0]?.size ?? 0)})`);

const cssTotal = initialCss.reduce((sum, f) => sum + gz(join(dir, f)), 0);
const fontsTotal = preloadedFonts.reduce((sum, f) => sum + readFileSync(join(dir, f)).length, 0);
check('Primeira visita (HTML + JS + CSS + fontes pré-carregadas)', gzipSync(html).length + jsTotal + cssTotal + fontsTotal, 400 * KB);

process.exit(failed ? 1 : 0);
