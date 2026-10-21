# Plano de medição (GQM)

Objetivo → perguntas → métricas, com a fonte de cada número (seção 17). Valores medidos em 2026-10-21, no fim da
implementação para o hackathon.

## M1. O app ajuda a achar o produto na feira?

| Pergunta | Métrica | Fonte | Valor |
|---|---|---|---|
| A busca encontra o que existe? | Produtos do seed encontrados em PT e pelos sinônimos EN/ES | `PublicCatalogIT` (CA-1) | 26 de 26 |
| Que buscas ficam sem resposta? | Top termos sem resultado (30 dias) | Painel → Visão geral | Coletado em produção |
| Quanto se busca, e em que idioma? | Buscas em 7 dias, por idioma; taxa de buscas com resultado | Painel / métrica `nandesk.search.requests` | Coletado em produção |

## M2. Funciona na rede da feira?

| Pergunta | Métrica | Fonte | Valor |
|---|---|---|---|
| Quanto JavaScript chega antes da primeira tela? | JS inicial (gzip) | `npm run size` | 143,3 KB (limite 150) |
| A primeira tela aparece rápido com rede ruim? | Tempo até a busca utilizável, perfil "Feira lotada" | E2E J8 | 1,4 s na 1ª visita (meta 3 s), 0,9 s na 2ª (meta 1 s) |
| Envios se perdem ou duplicam com quedas? | Uploads e envios por tarefa com a rede caindo | E2E J6 | 1 upload e 1 envio |
| Como é a experiência real? | p75 de LCP, INP, CLS e TTFB por rota | `/telemetry/web-vitals` → Grafana | Coletado em produção (amostra de 10%) |

## M3. A colaboração se sustenta?

| Pergunta | Métrica | Fonte |
|---|---|---|
| Quanto tempo a moderação leva? | Horas médias da fila até a decisão | Painel / `nandesk.moderation.duration` |
| Quantos envios são aprovados? | Taxa de aprovação | Painel |
| O acervo está fresco? | % de boxes verificados nos últimos 30 dias; % com foto | Painel |
| A verba dá conta? | Orçamento do mês: reservado, realizado e disponível | Painel → Finanças |

## M4. Qualidade do produto

| Métrica | Fonte | Valor |
|---|---|---|
| Testes unitários do backend | `./mvnw verify` | 128, todos verdes |
| Testes de integração com MySQL 8.4 | `*IT` | 71, todos verdes |
| Cobertura de linhas do backend (total / `domain`+`application`) | JaCoCo | 91% / todos os pacotes ≥ 80% |
| Testes do frontend / cobertura de linhas | `npm run ci` | 87 / 78,7% |
| Jornadas E2E | Playwright | J1 a J8 verdes no Chromium |
| Violações de acessibilidade "serious"/"critical" | axe nos E2E | 0 |
| Chaves de tradução faltando | Teste de paridade | 0 |
