// Carga da busca e do snapshot (RNF-01): p95 da busca ≤ 300 ms e do snapshot ≤ 200 ms, 100 usuários
// simultâneos sustentados e pico de 300 por 1 min; erro < 0,5%.
// Uso: k6 run perf/search.js -e BASE_URL=http://localhost:8081
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8081';
const TERMS = ['jambu', 'acai', 'castanha', 'tucupi', 'farinha', 'banho de cheiro', 'pirarucu', 'cupuacu', 'macaxeira', 'priprioca'];
const LANGS = ['pt-BR', 'en', 'es'];

export const options = {
  scenarios: {
    busca: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '3m', target: 100 },
        { duration: '15s', target: 300 },
        { duration: '1m', target: 300 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{name:search}': ['p(95)<300'],
    'http_req_duration{name:snapshot}': ['p(95)<200'],
    http_req_failed: ['rate<0.005'],
  },
};

export default function () {
  const q = TERMS[Math.floor(Math.random() * TERMS.length)];
  const lang = LANGS[Math.floor(Math.random() * LANGS.length)];
  const search = http.get(`${BASE}/api/v1/search?q=${encodeURIComponent(q)}`, {
    headers: { 'Accept-Language': lang },
    tags: { name: 'search' },
  });
  check(search, { 'busca 200': (r) => r.status === 200 });

  if (Math.random() < 0.2) {
    const snap = http.get(`${BASE}/api/v1/markets/ver-o-peso/snapshot`, {
      headers: { 'Accept-Language': lang },
      tags: { name: 'snapshot' },
    });
    check(snap, { 'snapshot 200': (r) => r.status === 200 });
  }
  sleep(1 + Math.random() * 2);
}
