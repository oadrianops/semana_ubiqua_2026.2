# ADR-0015: Orçamentos de performance e perfil "Feira lotada" como critério de aceite no CI

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Performance em rede ruim é requisito (RNF-03, CA-7), não ajuste final.

## Opções consideradas
1. Medir no fim — barato agora; contras: regressões só aparecem tarde.
2. Orçamentos no CI (size-limit + script próprio) e jornada E2E com emulação de rede.

## Decisão
O CI falha se: o JS inicial (entrada + tudo que o `index.html` pré-carrega) passar de 150 KB gzip, um chunk sob demanda passar de 80 KB, o CSS passar de 30 KB, o mapa passar de 80 KB ou as fontes pré-carregadas passarem de 120 KB. A jornada J8 emula 400 ms de latência e 400/150 kbps e exige conteúdo útil em até 3 s na 1ª visita e 1 s na 2ª. Medido: 142,7 KB de JS inicial; 1,3 s e 0,9 s na J8.

## Consequências
Para caber, trocamos o `sonner` por um aviso próprio, carregamos o Radix Dialog só quando a folha abre e o mapa só quando aparece, e pusemos um app shell no `index.html`.

## Requisitos relacionados
RNF-02 · RNF-03 · CA-7 · seção 9.14
