# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

PWA instalável (Android pelo Chrome e iPhone pelo Safari, "Adicionar à Tela de Início"). Apps nativos estão fora da v1.

## Users

- **Turista** (nacional ou estrangeiro) em Belém, no celular, muitas vezes dentro da própria feira, querendo saber em qual feira e em qual box encontrar uma iguaria (castanha-do-pará, jambu, banho de cheiro) e o que ela é, no próprio idioma (PT-BR, EN ou ES).
- **Morador de Belém** que precisa saber onde comprar um produto específico sem andar a feira inteira.
- **Colaborador** da comunidade que recebe uma tarefa ("corrida"), vai até a feira, faz check-in, fotografa e envia a atualização de um box em troca de comissão. Usa o app em campo, sob sol forte, com uma mão ocupada e sinal fraco.
- **Moderador e administrador** do programa, que revisam envios, mantêm o catálogo, registram repasses feitos por fora e acompanham os indicadores.
- O **feirante** não opera a plataforma (princípio P1): aparece nela com consentimento registrado por colaboradores e moderadores.

## Product Purpose

Dar visibilidade digital aos feirantes do Ver-o-Peso e da Feira da 25 e responder, em segundos, "em qual feira e em qual box eu encontro isto?". Os dados são mantidos atualizados por uma rede de colaboradores remunerados por tarefa aprovada, sem tirar tempo do feirante. Sucesso: boxes publicados com consentimento, buscas com resultado, boxes verificados nos últimos 30 dias, comissões repassadas e uso em EN/ES (OBJ-1 a OBJ-5 da especificação).

## Positioning

Um guia das feiras de Belém mantido pela comunidade, com cada dado moderado e datado ("verificado há N dias"), que funciona dentro da feira mesmo sem sinal. A atualização é feita por colaboradores pagos por tarefa, no modelo de "corrida", e não pelo feirante. Não há pagamento online, por decisão de produto (P2).

## Operating Context

- Feiras ao ar livre ou cobertas, lotadas, com muita gente disputando a mesma antena: a rede oscila entre 4G, 3G e nada. O app abre do cache e nunca trava esperando a rede.
- Uso em pé, ao sol, no celular a partir de 360 px, com fonte de pelo menos 16 px e alvos de toque de 44 px ou mais.
- Fluxo do colaborador em 3 passos: Aceitar → Fazer check-in (geolocalização dentro do raio da feira) → Enviar (fotos uma a uma, fila de envios).
- Repasses das comissões são feitos fora da plataforma (PIX ou dinheiro) e apenas registrados no extrato.
- Projeto acadêmico da UNAMA, inscrito no Hackathon Semana Ubíqua 2026.2 (prazo de entrega: 23/10/2026). Deploy no Coolify da equipe em `nandesk.com.br`.

## Capabilities and Constraints

- Especificação completa e fonte da verdade: `docs/ESPECIFICACAO.md`; requisitos RF-01 a RF-36, regras RN-01 a RN-28 e RNF-01 a RNF-16.
- Stack definida pela equipe: Spring Boot 4.1 (Java 21), React 19 + TypeScript (Vite, PWA), MySQL 8.4, OpenStreetMap com Leaflet, Docker.
- Idiomas: PT-BR, EN e ES em toda a interface, com fallback para PT-BR.
- Orçamentos de performance como critério de aceite (seção 9.14): JS inicial ≤ 150 KB gzip, CSS ≤ 30 KB, primeira visita ≤ 400 KB, visitas seguintes ≤ 30 KB pela rede.
- Fora do escopo: pagamento online, delivery ou reserva, outras cidades, fidelidade, gestão de vendas para o feirante, apps nativos, download de mapas para uso offline.
- Indefinido: validação com feirante real (CA-5) ainda não aconteceu; parceiros financiadores das comissões ainda não existem (os valores de comissão são ilustrativos).

## Brand Commitments

- Nome: **NANDESK**. Logo e ícones prontos em `frontend/public/brand/` (guia em `docs/brand.md`): o toldo de feira cobre o primeiro N. Não criar outra logo nem redigitar o logotipo com fonte.
- Cores da marca (Açaí, Tucupi, Jambu, Urucum, Rio e cinzas) e fontes (Archivo para títulos; Atkinson Hyperlegible Next para texto) definidas na seção 14.6 da especificação e obrigatórias.
- Voz: direta, local e respeitosa; frases curtas; botões dizem o que acontece; erros dizem o que houve e o que fazer, sem pedir desculpas (seção 14.8).
- Sem aparência de interface gerada por IA (seção 14.7): nada de degradês decorativos, emoji como ícone, cards idênticos para tudo, texto de marketing vazio ou números inventados.
- Referência de qualidade externa: plumeform.com, só como princípio (registrado em `docs/design/referencia-visual.md`), nunca como cópia.

## Evidence on Hand

- Dados de demonstração em `backend/src/main/resources/seed/dev-seed.json`: 2 feiras reais (Ver-o-Peso e Feira da 25), 9 categorias, 26 produtos típicos com descrições curtas nos três idiomas e 12 boxes **fictícios** (nomes e preços ilustrativos, marcados para revisão). A interface pública mostra o aviso "Dados de demonstração".
- **Não há fotos reais** das feiras, dos boxes nem dos produtos. Não usar banco de imagens nem imagens geradas por IA; as fotos chegam depois pelo fluxo dos colaboradores, com consentimento (RN-13, RN-22).
- **Não há depoimentos, clientes, parceiros nem números de uso.** Nada disso pode ser inventado.
- A coordenada da Feira da 25 é aproximada e deve ser confirmada no painel.

## Product Principles

1. O feirante não opera a plataforma: toda manutenção de dados passa pela rede de colaboradores e pela moderação.
2. Confiança acima de volume: todo dado público mostra quando foi verificado; preço é sempre "de referência".
3. Feito para o celular e para a rede da feira: o que já foi visto continua disponível sem sinal, e nenhum envio se perde.
4. Multilíngue e local: termos regionais preservados (tucupi, jambu, maniçoba), explicados nos outros idiomas, sem alegações de saúde.
5. Privacidade por padrão: coletar o mínimo, localização só no check-in, consentimento explícito do feirante.

## Accessibility & Inclusion

WCAG 2.2 AA: contraste de pelo menos 4,5:1 no texto, foco visível, navegação por teclado, alternativa em lista para todo mapa, `prefers-reduced-motion` respeitado e zoom do navegador permitido. Legibilidade ao sol foi o motivo da escolha da Atkinson Hyperlegible Next. Há um tema de alto contraste além do tema claro padrão.
