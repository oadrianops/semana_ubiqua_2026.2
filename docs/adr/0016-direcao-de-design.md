# ADR-0016: Direção de design "Lousa de preços"

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O visual precisa ser profissional, parecer das feiras de Belém e não ter "cara de IA" (14.7). A marca já existe (`nandesk-brand/`).

## Opções consideradas
1. Vitrine de cards com fotos de banco — comum; contras: genérico, e não temos fotos reais.
2. Lousa de preços: tipografia, réguas finas e números tabulares, como a placa de preço da feira.

## Decisão
Direção escolhida com a skill impeccable (contrato em `.impeccable/`): faixa Açaí 900 com a busca, pontilhado entre produto e preço, números tabulares e a data da verificação em cada linha; Tucupi só sobre fundo escuro; Açaí 700 só em itens acionáveis; réguas no lugar de caixas; prazos como barra proporcional ao tempo restante; dado novo marcado como "atualizado" até ser visto. Títulos em Archivo (largura 112) e texto em Atkinson Hyperlegible Next, auto-hospedadas. Princípios da referência visual em `docs/design/referencia-visual.md`, sem copiar código, texto ou marca.

## Consequências
Interface tipográfica, legível sob sol e sem depender de fotos que ainda não temos.

## Requisitos relacionados
seção 14 · RNF-07
