# ADR-0013: Offline-first: snapshot por feira com ETag e cache persistido no aparelho

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Dentro do Ver-o-Peso o sinal cai com frequência. Turistas e colaboradores precisam consultar boxes e preços sem rede.

## Opções consideradas
1. Só cache HTTP do service worker — simples; contras: busca offline impossível.
2. Snapshot compacto por feira e idioma, com ETag por `dataVersion`, guardado no IndexedDB, e busca local com a mesma normalização do servidor.

## Decisão
Endpoint `/markets/{slug}/snapshot` (≤ 150 KB gzip), revalidado com `If-None-Match` (304 quando nada mudou). "Salvar para usar sem internet" mantém a feira atualizada sempre que houver sinal; sem rede, a feira e a busca local funcionam e mostram a data dos dados ("salvos em 19/10 às 15:40"), com aviso depois de 7 dias (RN-28). As consultas públicas do TanStack Query também persistem no aparelho; dados pessoais, não.

## Consequências
A primeira tela também tem um app shell estático no `index.html` com a busca funcionando sem JavaScript. Custo: dados podem ficar velhos, por isso a data aparece sempre.

## Requisitos relacionados
RF-36 · RNF-03 · RN-28 · CA-6
