# ADR-0014: Upload de fotos uma a uma, idempotente, antes do envio

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Com rede intermitente, um único multipart de 1,5 MB falha no meio e recomeça do zero.

## Opções consideradas
1. Um multipart com tudo — simples; contras: qualquer queda perde tudo.
2. Cada foto sobe assim que é tirada, com `Idempotency-Key` gerada no aparelho; o envio leva só os ids.

## Decisão
O app comprime a foto (1600 px, ~300 KB), sobe em `/uploads` com uma chave por foto e guarda o `fileId`; a fila (outbox) envia o changeset com a sua própria chave. Repetir com a mesma chave devolve o mesmo resultado; outra chave com outro conteúdo é recusada (`IDEMPOTENCY_KEY_REUSED`). Fotos temporárias vencem em 24 h (RN-27) e o app sobe de novo se preciso.

## Consequências
Uma queda só obriga a reenviar a foto que faltou; nada duplica (verificado na jornada E2E J6).

## Requisitos relacionados
RF-18 · RN-27 · RNF-03
