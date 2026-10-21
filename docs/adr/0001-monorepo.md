# ADR-0001: Monorepo com backend, frontend, infra e docs

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O projeto tem duas aplicações (API e PWA), infraestrutura como código e uma especificação viva. A equipe é pequena e as mudanças costumam atravessar as camadas: um endpoint novo mexe no backend, nos tipos do frontend, na documentação e nos testes E2E.

## Opções consideradas
1. Monorepo único — um PR atômico por mudança, CI por caminho, rastreabilidade simples; contras: workflows precisam filtrar por pasta.
2. Repositórios separados (api, web, infra) — permissões e versões independentes; contras: PRs coordenados, contrato da API fora de sincronia, mais trabalho de release.

## Decisão
Monorepo com `backend/`, `frontend/`, `infra/`, `contracts/`, `perf/` e `docs/`. Cada workflow do GitHub Actions roda só quando a sua pasta muda.

## Consequências
Um PR leva código, contrato OpenAPI, tipos gerados, testes e documentação juntos. O histórico único facilita a matriz de rastreabilidade (`infra/scripts/rastreabilidade.py`). O tamanho do clone cresce com o tempo; os artefatos de build ficam fora do Git.

## Requisitos relacionados
RNF-11 · seção 16.1
