#!/usr/bin/env node
// Importa um levantamento de campo pela API do painel (RF-29, RN-13, ADR-0018): atualiza a feira, cria os setores
// que faltam, envia a capa e cadastra os boxes com o termo assinado, as ofertas e as fotos.
//
// Os dados reais (nomes, fotos) ficam fora do repositório: o manifesto e as fotos moram em file/ (ignorado pelo
// Git) e vão direto para o volume de produção, de onde a revogação do consentimento consegue apagá-los.
//
// Uso (Node 24, sem dependências):
//   NANDESK_ADMIN_PASSWORD='...' node infra/scripts/importar-campo.mjs file/campo/feira-da-25.json \
//     [--api https://nandesk.com.br] [--email admin@nandesk.local] [--dry-run]
//
// Pode rodar de novo: o box que já existe não é recriado, as ofertas são regravadas e as fotos só sobem quando
// o box ainda não tem nenhuma.
import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { parseArgs } from 'node:util';

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    api: { type: 'string', default: 'http://localhost:8080' },
    email: { type: 'string', default: 'admin@nandesk.local' },
    'dry-run': { type: 'boolean', default: false },
  },
});

const manifestPath = positionals[0];
const password = process.env.NANDESK_ADMIN_PASSWORD;
if (!manifestPath || !password) {
  console.error('Uso: NANDESK_ADMIN_PASSWORD=... node infra/scripts/importar-campo.mjs <manifesto.json> [--api URL]');
  process.exit(2);
}
const base = values.api.replace(/\/$/, '') + '/api/v1';
const dry = values['dry-run'];
const root = dirname(resolve(manifestPath));
const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));

let token = '';

async function call(method, path, body) {
  const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
  let payload;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(base + path, { method, headers, body: payload });
  const text = await res.text();
  const json = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const detail = json?.detail ?? json?.code ?? text;
    throw new Error(`${method} ${path} → ${res.status} ${detail}`);
  }
  return json;
}

async function photoForm(relative) {
  const bytes = await readFile(resolve(root, relative));
  const form = new FormData();
  form.append('file', new Blob([bytes], { type: 'image/jpeg' }), relative.split('/').pop());
  return form;
}

function step(message) {
  console.log((dry ? '[simulação] ' : '') + message);
}

// 1. Sessão
const login = await fetch(`${base}/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  body: JSON.stringify({ email: values.email, password }),
});
if (!login.ok) {
  console.error(`Login falhou (${login.status}). Confira o e-mail, a senha e a URL da API.`);
  process.exit(1);
}
token = (await login.json()).accessToken;
step(`Sessão aberta em ${values.api} como ${values.email}`);

// 2. Feira, setores e capa
const m = manifest.market;
let market = await call('GET', `/admin/markets/${m.slug}`);
if (m.update) {
  step(`Feira ${market.name}: endereço, posição, horários e textos`);
  if (!dry) {
    market = await call('PUT', `/admin/markets/${m.slug}`, {
      name: market.name,
      active: market.active,
      ...m.update,
      translations: { ...market.translations, ...m.update.translations },
      expectedVersion: market.version,
    });
  }
}
for (const sector of m.sectors ?? []) {
  if (market.sectors.some((s) => s.slug === sector.slug)) continue;
  step(`Setor novo: ${sector.names['pt-BR']}`);
  if (!dry) {
    market = await call('PUT', `/admin/markets/${m.slug}/sectors/${sector.slug}`, {
      sortOrder: sector.sortOrder,
      active: true,
      names: sector.names,
    });
  }
}
if (m.cover && !market.coverThumbUrl) {
  step(`Capa da feira: ${m.cover}`);
  if (!dry) await call('POST', `/admin/markets/${m.slug}/cover`, await photoForm(m.cover));
} else if (m.cover) {
  step('Capa da feira: já existe, mantida');
}

// 3. Boxes
const summary = [];
for (const s of manifest.stalls) {
  const found = await call('GET', `/admin/stalls?market=${m.slug}&q=${encodeURIComponent(s.code)}&size=100`);
  let stall = found.items.find((x) => x.code === s.code);
  if (!stall) {
    step(`Box ${s.code}: cadastro com termo assinado`);
    if (!dry) {
      stall = (
        await call('POST', '/admin/stalls', {
          marketSlug: m.slug,
          sectorSlug: s.sector ?? null,
          code: s.code,
          displayName: s.displayName ?? null,
          vendorName: s.vendorName ?? null,
          showVendorName: Boolean(s.showVendorName),
          consentMethod: s.consentMethod ?? 'SIGNED_FORM',
          instagram: s.instagram ?? null,
        })
      ).stall;
    }
  } else {
    step(`Box ${s.code}: já existe, mantido`);
  }
  if (dry) continue;

  for (const o of s.offerings ?? []) {
    await call('PUT', `/admin/stalls/${stall.publicId}/offerings/${o.product}`, {
      price: o.price,
      priceUnit: o.unit,
      availability: o.availability ?? 'AVAILABLE',
    });
  }
  step(`Box ${s.code}: ${s.offerings?.length ?? 0} ofertas gravadas`);

  const detail = await call('GET', `/admin/stalls/${stall.publicId}`);
  if (detail.photos.length > 0) {
    step(`Box ${s.code}: já tem ${detail.photos.length} fotos, mantidas`);
  } else if (s.photos) {
    // Uma foto por vez (RN-27): numa falha, rodar de novo retoma pelo box que ficou sem fotos.
    if (s.photos.main) {
      await call('POST', `/admin/stalls/${stall.publicId}/photos?main=true`, await photoForm(s.photos.main));
    }
    for (const g of s.photos.gallery ?? []) {
      await call('POST', `/admin/stalls/${stall.publicId}/photos`, await photoForm(g));
    }
    step(`Box ${s.code}: ${(s.photos.main ? 1 : 0) + (s.photos.gallery?.length ?? 0)} fotos enviadas`);
  }
  summary.push(`${values.api.replace(/\/$/, '')}/stalls/${stall.publicId}`);
}

console.log('\nPronto. Páginas públicas:');
console.log(`  ${values.api.replace(/\/$/, '')}/markets/${m.slug}`);
for (const url of summary) console.log(`  ${url}`);
