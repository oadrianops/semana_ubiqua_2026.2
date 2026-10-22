/// <reference types="node" />
import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { appConfig, directionsUrl } from './config';

function hasShell(): boolean {
  try {
    execFileSync('sh', ['-c', 'true']);
    return true;
  } catch {
    return false;
  }
}

/** Roda o nginx/30-config-js.sh de verdade e devolve o objeto que ele grava em window.__APP_CONFIG__. */
function runConfigScript(env: Record<string, string> = {}) {
  const dir = mkdtempSync(join(tmpdir(), 'nandesk-config-'));
  try {
    const target = join(dir, 'config.js');
    const clean = Object.fromEntries(Object.entries(process.env).filter(([k]) => !k.startsWith('WEB_')));
    execFileSync('sh', ['nginx/30-config-js.sh'], { env: { ...clean, ...env, CONFIG_JS_TARGET: target } });
    const holder: { __APP_CONFIG__?: Record<string, unknown> } = {};
    new Function('window', readFileSync(target, 'utf8'))(holder);
    return holder.__APP_CONFIG__ ?? {};
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

describe('RF-01 configuração do mapa em tempo de execução', () => {
  afterEach(() => {
    delete window.__APP_CONFIG__;
  });

  it.runIf(hasShell())('RF-01 o config.js do contêiner traz as URLs padrão inteiras', () => {
    const config = runConfigScript();
    expect(config.mapTileUrl).toBe('https://tile.openstreetmap.org/{z}/{x}/{y}.png');
    expect(config.directionsUrlTemplate).toBe(
      'https://www.google.com/maps/dir/?api=1&destination={lat},{lng}',
    );
  });

  it.runIf(hasShell())('RF-01 o config.js respeita a URL de tiles definida por variável', () => {
    const config = runConfigScript({ WEB_MAP_TILE_URL: 'https://tiles.example/{z}/{x}/{y}.png' });
    expect(config.mapTileUrl).toBe('https://tiles.example/{z}/{x}/{y}.png');
  });

  it('RF-01 "Como chegar" monta o destino com latitude e longitude', () => {
    expect(directionsUrl(-1.445, -48.4682)).toBe(
      'https://www.google.com/maps/dir/?api=1&destination=-1.445,-48.4682',
    );
  });

  it('RF-01 configuração sem os marcadores volta para o padrão', () => {
    window.__APP_CONFIG__ = {
      mapTileUrl: 'https://tile.openstreetmap.org/{z/{x}/{y}.png}',
      directionsUrlTemplate: 'https://www.google.com/maps/dir/?api=1&destination={lat,{lng}}',
    };
    expect(appConfig().mapTileUrl).toBe('https://tile.openstreetmap.org/{z}/{x}/{y}.png');
    expect(directionsUrl(-1.445, -48.4682)).toContain('destination=-1.445,-48.4682');
  });
});
