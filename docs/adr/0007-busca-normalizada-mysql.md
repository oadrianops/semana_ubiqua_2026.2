# ADR-0007: Busca com termos normalizados no MySQL (sem motor externo na v1)

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O catálogo tem dezenas a poucos milhares de produtos. A busca precisa tolerar acento, maiúscula, plural e sinônimos regionais em três idiomas, e funcionar igual sem conexão no aparelho.

## Opções consideradas
1. Elasticsearch/OpenSearch — poderoso; contras: mais um serviço e memória no VPS.
2. FULLTEXT do MySQL — sem serviço extra; contras: ruim com termos curtos e sinônimos.
3. Tabela de termos normalizados + ranking próprio, com a mesma normalização no frontend.

## Decisão
`SearchNormalizer` (minúsculas, sem acento, sem pontuação, plural simples) gera `product_search_terms` a partir de nomes e sinônimos nos 3 idiomas; a busca faz prefixo e igualdade nesses termos, ordena por `SearchRanking` e sugere "você quis dizer" por distância de edição. O arquivo `contracts/search-normalization-cases.json` é usado pelos testes do backend e do frontend.

## Consequências
Zero infraestrutura extra e resultado idêntico online e offline. Se o catálogo passar de ~50 mil termos ou exigir relevância semântica, reavaliar um motor dedicado.

## Requisitos relacionados
RF-02 · CA-1
