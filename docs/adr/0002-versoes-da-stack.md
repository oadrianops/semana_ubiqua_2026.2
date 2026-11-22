# ADR-0002: Versões da stack

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
A especificação fixa linhas (Java 21, Spring Boot 4.1, MySQL 8.4, React 19, Node 24) e pede versões exatas. Em outubro de 2026 várias dependências têm versões novas com incompatibilidades entre si.

## Opções consideradas
1. Sempre a última de cada pacote — mais recente; contras: combinações sem suporte (ex.: TypeScript 7 com typescript-eslint 8).
2. Últimas versões compatíveis entre si, fixadas sem faixa (`^`/`~`) — builds reproduzíveis; contras: exige revisão periódica (Dependabot).

## Decisão
Versões exatas compatíveis entre si, fixadas no `pom.xml`, no `package.json` (sem `^`) e nas imagens Docker:

| Área | Versão |
|---|---|
| Java / Maven | Temurin 21.0.13 · Maven 3.10.1 (wrapper 3.3.4) |
| Spring Boot | 4.1.2 (Framework 7.0.10, Security 7.1.2, Hibernate 7.4.13, Jackson 3.1.7, Flyway 12.4.0) |
| Banco | MySQL 8.4.9 · Connector/J 9.7.0 |
| Testes backend | JUnit 6.0.3, Mockito 5.23.0, AssertJ 3.27.7, Testcontainers 2.0.5, ArchUnit 1.5.1, Awaitility 4.3.0 |
| Outras backend | springdoc 3.2.0, MapStruct 1.6.3, Lombok 1.18.48, Bucket4j 8.22.0, Thumbnailator 0.4.21, Caffeine 3.2.4 |
| Node / build | Node 24.22 (imagem) · Vite 8.3.8 · TypeScript 5.9.3 |
| Frontend | React 19.3.1, React Router 7.18.5, TanStack Query 5.107.1, Tailwind 4.3.5, i18next 26.5.3, Leaflet 1.9.4, vite-plugin-pwa 2.0.0, Workbox 7.4.1 |
| Testes frontend | Vitest 5.1.0, Testing Library 16.3.3, MSW 2.15.0, Playwright 1.65.0, axe-core 4.14.0 |
| Imagens | `eclipse-temurin:21.0.13_9-jre-noble`, `node:24.22.3-alpine3.24`, `nginxinc/nginx-unprivileged:1.31.7-alpine3.24`, `axllent/mailpit:v1.31.6` |

Onde não usamos a última versão: TypeScript 5.9 (o typescript-eslint e o openapi-typescript ainda não aceitam a 7); ESLint 9 (o jsx-a11y não suporta a 10); React Router 7 e MSW 2 (linhas fixadas pela especificação); @hookform/resolvers 5.9 (a 6 conflita com um peer opcional); MapStruct 1.6 (a 1.7 é beta).

## Consequências
Builds reproduzíveis e iguais no CI, no Docker e na máquina de cada pessoa. O Dependabot propõe as atualizações semanalmente, agrupadas, e o CI decide. Rever este ADR a cada fase.

**Revisão de 23/10/2026 (PRs do Dependabot):** entraram as ações do GitHub (checkout 7, setup-java 6, setup-node 7, upload-artifact 7, dependency-review 5 e docker/* da geração seguinte) e o Nginx 1.31.7, com todo o CI verde. Ficaram de fora, e o `dependabot.yml` passa a ignorar os majors: ESLint e @eslint/js 10 (o jsx-a11y não instala com o ESLint 10), MSW 3 e React Router 8 (linhas fixadas pela especificação), @types/node acima do runtime (Node 24), a imagem `node` 26 e o `eclipse-temurin` acima do 21 (o enforcer recusa outro JDK).

**Revisão de 21/11/2026 (PRs do Dependabot de novembro):** entraram, num único PR, o Spring Boot 4.1.2, springdoc 3.2.0,
Bucket4j 8.22.0, JaCoCo 0.8.16, Spotless 3.11.0, Enforcer 3.7.0 e Maven 3.10.1 (#33), o Temurin 21.0.13 (#29), o Node
24.22.3 (#32) e 24 atualizações menores do frontend (#34), com `./mvnw verify` e `npm run ci` verdes. O lock do npm foi
regenerado (o do Dependabot não passava no `npm ci` com o npm 11.4) e o `mvnw.cmd` ficou com o fim de linha do
`.gitattributes`. Ficaram de fora, e o `dependabot.yml` passa a ignorar os majors: TypeScript 7 (#19: o
openapi-typescript só aceita o 5 e o `npm ci` falha) e @hookform/resolvers 6 (#20: só acrescenta o Effect 4 e declara
peers opcionais em conflito, que quebram o `npm install`).

## Requisitos relacionados
RNF-11 · seção 8
