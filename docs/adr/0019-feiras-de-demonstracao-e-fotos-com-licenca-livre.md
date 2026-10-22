# ADR-0019: Mais feiras na demonstração e fotos de capa só com licença livre

- Situação: aceito
- Data: 2026-10-21
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
A equipe pediu sete feiras além do Ver-o-Peso e da Feira da 25 (Cremação, Pedreira, Bandeira Branca, Icoaraci,
Benguí, Mercado de São Brás e Praça da República), com boxes e produtos fictícios e foto de capa tirada da internet.
A v1 previa duas feiras (seção 2.1). Fotos de sites de notícia ou de buscadores têm dono: usá-las sem licença expõe o
projeto a pedido de remoção e contradiz o princípio de usar fotos reais com autorização (14.6).

## Opções consideradas
1. Copiar fotos de sites e buscadores. Contras: direito autoral; nenhuma garantia de que a foto é da feira certa.
2. Só fotos com licença livre (Wikimedia Commons), com crédito do autor e da licença visível no app; feira sem foto
   livre que mostre a própria feira fica sem capa até a equipe fotografar.

## Decisão
Opção 2.
- Ver-o-Peso e Mercado de São Brás ganham capa do Wikimedia Commons (CC BY-SA 3.0), reduzida para 1600 px, com o
  crédito abaixo da foto e link para a página do arquivo (`markets.cover_credit` e `cover_credit_url`, V6). Lista
  em `docs/creditos-imagens.md`.
- Icoaraci, Cremação e Praça da República só têm no Commons fotos do porto, de prédios do bairro ou da praça sem a
  feira; Pedreira, Bandeira Branca e Benguí não têm nenhuma. Ficam sem capa.
- As sete feiras entram pelo seed com endereço, posição (OpenStreetMap) e horário de fontes públicas, que às vezes
  divergem; os boxes e preços são fictícios e marcados como exemplo (ADR-0018).
- O painel aceita o crédito ao enviar uma capa e a posição de cada box ("Como chegar ao box").

## Consequências
O mapa passa a mostrar nove feiras, sete delas só com dados de exemplo. Conferir em campo endereço, posição e horário
antes de tratar essas feiras como reais, e trocar as capas por fotos próprias quando a equipe visitar.

## Requisitos relacionados
RF-01 · RF-29 · RF-34 (posição do box) · 14.6 · ADR-0017 · ADR-0018
