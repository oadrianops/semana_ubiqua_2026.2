# NANDESK

Plataforma colaborativa das feiras de Belém (Ver-o-Peso e Feira da 25). App web instalável (PWA), feito para o celular e para rede instável.
Especificação completa: `docs/ESPECIFICACAO.md` — leia só as seções indicadas abaixo conforme a tarefa (não importe o arquivo inteiro: ele tem mais de 3.000 linhas).

## Estrutura
- `backend/` — Spring Boot 4.1 (Java 21, Maven Wrapper), pacote `br.com.nandesk`, módulos por funcionalidade com camadas `api/application/domain/infra`
- `frontend/` — React 19 + TypeScript + Vite (PWA com `injectManifest`), Tailwind 4, TanStack Query, i18next
- `infra/` — scripts (chaves JWT/VAPID, backup, rastreabilidade), observabilidade
- `contracts/` — casos de teste compartilhados entre backend e frontend (normalização da busca)
- `docs/` — especificação, ADRs, design, rastreabilidade, validação, runbooks, legal
- `perf/` — scripts k6

## Comandos
- Infra local: `docker compose up -d` (MySQL + Mailpit)
- Backend: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- Verificar backend: `cd backend && ./mvnw verify` (precisa do Docker: Testcontainers)
- Frontend: `cd frontend && npm run dev` (proxy de `/api` para :8080)
- Verificar frontend: `cd frontend && npm run ci` (lint + typecheck + testes + i18n + build + size)
- Stack completo: `docker compose --profile app up -d --build` → http://localhost:8081
- E2E: `cd frontend && npm run e2e`
- Tipos da API: `cd frontend && npm run api:generate` (lê `docs/api/openapi.json`)
- Detector de design: `npx impeccable@4.7.0 detect frontend/src`
- Windows: use Git Bash para os scripts `.sh`; `mvnw.cmd` no lugar de `./mvnw` no PowerShell.

## Sempre
- Citar IDs de requisitos (RF-xx, RN-xx, RNF-xx, CA-x) em commits, PRs e testes (`@Tag("RF-02")`, `@DisplayName("RN-05: ...")`, `test("RF-02 ...")`).
- Commits: Conventional Commits em PT-BR com os IDs no fim — `feat(search): agrupa resultados por feira e box (RF-02)`.
- Rodar `./mvnw verify` e `npm run ci` antes de concluir uma tarefa.
- Schema só via Flyway (`backend/src/main/resources/db/migration`), com `ddl-auto=validate`.
- Dinheiro em BigDecimal/DECIMAL(10,2) (HALF_EVEN; string na API); datas como Instant em UTC, exibidas em America/Belem; usar o bean `Clock`.
- Textos de interface só via i18n (pt-BR, en, es); código em inglês; docs e commits em PT-BR.
- Registrar decisões técnicas relevantes em `docs/adr/` (modelo no Anexo D da especificação).
- Em toda tarefa de interface: carregar a skill `impeccable`, seguir a seção 14.7 e rodar `audit` + `critique` antes de concluir a tela; registrar o resumo no PR.
- Respeitar os orçamentos de performance (seção 9.14, `npm run size`) e testar no perfil de rede "Feira lotada".
- Toda tela com estados carregando, vazio, erro e sem conexão.
- Envios do colaborador só pela fila (outbox); fotos uma a uma com `Idempotency-Key` (RN-27).

## Nunca
- Implementar pagamento online, carteira ou gateway (decisão de produto P2).
- Editar uma migração já aplicada na `main`.
- Usar H2 nos testes (use Testcontainers com MySQL).
- Guardar token em localStorage/sessionStorage ou segredos no repositório.
- Fazer cache de tiles do OpenStreetMap no service worker.
- Usar padrões da lista "Evitar" da seção 14.7 (degradês decorativos, emoji como ícone, cards iguais para tudo, texto de marketing vazio, Inter/Poppins).
- Usar cor, fonte ou espaçamento fora dos tokens de `frontend/src/shared/ui/tokens.css`.
- Versionar a pasta `file/` (material de terceiros e dados pessoais da equipe).

## Onde ler na especificação
| Tarefa | Seções |
|---|---|
| Regra de negócio | 6 (RN) e o RF correspondente em 5 |
| Endpoint novo ou alterado | 11 (convenções, endpoints, erros 11.4) e 12.2 |
| Tabela ou migração | 10 |
| Arquitetura, módulos, fluxos | 9.1–9.12 |
| Offline, PWA, fila de envios | 9.13 e RF-35/RF-36 |
| Performance e rede | 9.14 e RNF-01/02/03 |
| Segurança | 12 · LGPD: 13 |
| Interface, marca, texto | 14 (14.6 marca, 14.7 direção, 14.8 texto) |
| Testes e gates | 15 |
| Docker, CI, deploy | 16 (deploy no Coolify: `docs/runbooks/deploy.md`) |
| Fases e critérios de saída | 18 |
