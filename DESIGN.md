---
name: NANDESK
description: Lousa de preços das feiras de Belém — em qual feira e em qual box vende o que você procura.
colors:
  acai-900: "#2b1330"
  acai-700: "#4a2152"
  acai-100: "#f3edf4"
  tucupi-400: "#f2b23a"
  tucupi-700: "#7a4f00"
  jambu-600: "#2f6b2f"
  urucum-600: "#b3381c"
  rio-600: "#1e5a73"
  cinza-50: "#f6f4f7"
  cinza-200: "#e4e0e6"
  cinza-500: "#6b6370"
  cinza-800: "#3b353e"
  branco: "#ffffff"
  on-band-muted: "#d9cddc"
typography:
  display:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "2.25rem"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "-0.01em"
    fontVariation: "'wdth' 112"
  headline:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "-0.01em"
    fontVariation: "'wdth' 112"
  title:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "1.375rem"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "-0.01em"
    fontVariation: "'wdth' 112"
  price:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 700
    lineHeight: 1.15
    fontFeature: "'tnum' 1, 'lnum' 1"
    fontVariation: "'wdth' 112"
  body:
    fontFamily: "'Atkinson Hyperlegible Next Variable', 'Atkinson Hyperlegible Next', system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  body-large:
    fontFamily: "'Atkinson Hyperlegible Next Variable', 'Atkinson Hyperlegible Next', system-ui, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "'Atkinson Hyperlegible Next Variable', 'Atkinson Hyperlegible Next', system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 600
    lineHeight: 1.5
  support:
    fontFamily: "'Atkinson Hyperlegible Next Variable', 'Atkinson Hyperlegible Next', system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
rounded:
  control: "10px"
  panel: "14px"
  pill: "999px"
spacing:
  space-1: "0.25rem"
  space-2: "0.5rem"
  space-3: "0.75rem"
  space-4: "1rem"
  space-6: "1.5rem"
  space-8: "2rem"
  space-12: "3rem"
  touch: "44px"
  content-max: "72rem"
  measure: "65ch"
components:
  button-primary:
    backgroundColor: "{colors.acai-700}"
    textColor: "{colors.branco}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 1rem"
    height: "44px"
  button-primary-hover:
    backgroundColor: "{colors.acai-900}"
    textColor: "{colors.branco}"
  button-secondary:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.acai-700}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 1rem"
    height: "44px"
  button-secondary-hover:
    backgroundColor: "{colors.acai-100}"
  button-ghost:
    textColor: "{colors.acai-700}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 1rem"
    height: "44px"
  button-ghost-hover:
    backgroundColor: "{colors.acai-100}"
  button-danger:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.urucum-600}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 1rem"
    height: "44px"
  button-on-band:
    backgroundColor: "{colors.tucupi-400}"
    textColor: "{colors.acai-900}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 1rem"
    height: "44px"
  button-on-band-quiet:
    textColor: "{colors.branco}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "0 1rem"
    height: "44px"
  button-large:
    typography: "{typography.body-large}"
    padding: "0 1.25rem"
    height: "52px"
  field:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.acai-900}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "0 0.75rem"
    height: "48px"
  search-field-band:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.acai-900}"
    typography: "{typography.body-large}"
    rounded: "{rounded.control}"
    height: "60px"
  price-row:
    textColor: "{colors.acai-900}"
    typography: "{typography.body}"
    padding: "0.75rem 0"
  chip:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.acai-900}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "0 1rem"
    height: "44px"
  chip-active:
    backgroundColor: "{colors.acai-700}"
    textColor: "{colors.branco}"
  status-pill:
    typography: "{typography.support}"
    rounded: "{rounded.pill}"
    padding: "0.125rem 0.5rem"
  header-band:
    backgroundColor: "{colors.acai-900}"
    textColor: "{colors.branco}"
    height: "64px"
  bottom-nav:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.cinza-800}"
    height: "56px"
  bottom-nav-active:
    textColor: "{colors.acai-700}"
  sheet:
    backgroundColor: "{colors.branco}"
    rounded: "{rounded.panel}"
    padding: "1rem"
    width: "min(32rem, calc(100vw - 2rem))"
  toast:
    backgroundColor: "{colors.branco}"
    textColor: "{colors.acai-900}"
    rounded: "{rounded.panel}"
    width: "28rem"
---

# Design System: NANDESK

Registro do sistema visual como ele foi entregue. Fontes de verdade: `frontend/src/shared/ui/tokens.css`, `frontend/src/index.css`, os componentes em `frontend/src/shared/ui/`, `frontend/src/features/shared/board.tsx`, `frontend/src/features/tasks/Stepper.tsx`, `frontend/src/app/AppLayout.tsx`, `frontend/src/features/admin/` e o app shell em `frontend/index.html`. Os tokens do frontmatter são normativos; o texto explica onde e por quê. Decisão registrada no ADR-0016; marca em `docs/brand.md`.

## Overview

**Creative North Star: "Lousa de preços"**

A interface é a pedra de preços da feira: cada lista do que um box vende é montada como a placa escrita à mão — produto, pontilhado, preço e unidade em colunas fixas, com números tabulares — e cada linha diz quando foi conferida ("verificado há N dias"). A hierarquia vem de tipografia, réguas e espaço, não de caixas, fotos ou cards. Não há fotografia de produto: o build é tipográfico até que colaboradores enviem fotos reais.

O mundo tem dois chãos. O chão de trabalho é branco (melhor ao sol). No topo de toda tela há uma faixa Açaí 900 com a logo invertida; na início e na busca, essa faixa carrega o campo de busca. O amarelo Tucupi aparece só sobre essa faixa (e sobre painéis Açaí 900 do mesmo material), como o toldo da logo. Títulos, nomes de feira, códigos de box e preços em Archivo semi-expandida (largura 112); todo o resto em Atkinson Hyperlegible Next. A densidade é de quadro de avisos: linhas de 48 a 64 px separadas por régua fina, sem moldura.

O movimento é escasso e sempre resposta: pressionar um botão afunda 1 px, um aviso desce 6 px ao entrar, o esqueleto pulsa enquanto carrega. Nenhuma seção anima ao aparecer.

**Key Characteristics:**
- Linha de preço com pontilhado e números tabulares como assinatura.
- Faixa Açaí 900 no topo; Tucupi só sobre fundo escuro.
- Réguas (2 px Açaí 900 sob títulos de seção, 1 px Cinza 200 entre linhas) no lugar de caixas.
- Raio por papel: 10 px em controles, 14 px em folhas e painéis, pílula só em chips, selos e barras.
- Sombra só no que flutua.
- Alvos de 44 px, campos de 16 px, foco visível de 3 px.
- Tema de alto contraste em `[data-contrast='high']`.

## Colors

Paleta sóbria de marca: um roxo-açaí profundo como tinta e faixa, um açaí médio para o que se toca, e o amarelo do tucupi como único brilho, reservado ao fundo escuro.

### Primary
- **Açaí 700 — açaí de toque** (`acai-700`): botões primários, links, chips ativos, item ativo da navegação inferior e do painel, cor do cursor de texto e do anel de foco sobre fundo claro. Branco sobre ele: cerca de 12,9:1. No build também marca o **código do box** (Archivo tabular, ex.: "Box 103"), os ícones de categoria, o preenchimento do stepper, da barra de prazo e da barra de limite em Finanças, e o ponto de "não lida" nas notificações (ver divergência abaixo).
- **Açaí 900 — tinta e faixa** (`acai-900`): texto principal (cerca de 17:1 sobre branco), régua de 2 px sob títulos de seção, fundo da faixa do cabeçalho, das faixas de busca e dos painéis de destaque (saldo em Ganhos, fila em Visão geral). Também é o hover do botão primário.
- **Açaí 100 — superfície de marca** (`acai-100`): hover de botões secundários e fantasma, item ativo do painel, opção ativa das sugestões de busca, aviso de rede e cor de seleção de texto.

### Secondary
- **Tucupi 400 — toldo** (`tucupi-400`): só sobre Açaí 900. Usos reais: sublinhado do link ativo no cabeçalho, contador do sino, borda de foco do campo de busca na faixa, anel de foco dentro de `.on-band`, o número da fila em Visão geral e o saldo em Ganhos (ambos sobre painel Açaí 900), o miolo do marcador do mapa (sobre o pino Açaí 700), o link "Pular para o conteúdo" (sobre a faixa) e o botão "Buscar" do app shell. Sobre Açaí 900 dá cerca de 9:1; **sobre branco, 1,87:1**: nunca texto, ícone ou borda com significado em fundo claro.
- **Tucupi 700 — alerta** (`tucupi-700`): texto de aviso ("pode estar desatualizado", dados salvos antigos, selo de alerta). Cerca de 7:1 sobre branco e 6,5:1 sobre Cinza 50.

### Tertiary
- **Jambu 600** (`jambu-600`): sucesso — borda do aviso de sucesso, selo "bom". Cerca de 6,4:1 sobre branco.
- **Urucum 600** (`urucum-600`): erro e perigo — texto de erro do campo, borda de campo inválido, botão de perigo, ícone do estado de erro, barra de prazo nos últimos 15 minutos. Cerca de 6:1 sobre branco.
- **Rio 600** (`rio-600`): informação — nota "de safra" nas linhas do quadro. Cerca de 7,6:1 sobre branco.

### Neutral
- **Branco** (`branco`): fundo e superfícies; texto sobre a faixa e sobre Açaí 700.
- **Cinza 50** (`cinza-50`): superfície suave — rodapé, aviso de demonstração, caixa de aviso de desatualização, hover de linhas do quadro.
- **Cinza 200** (`cinza-200`): régua fina entre linhas, trilho do stepper e da barra de prazo, borda do toast neutro. Cerca de 1,3:1: só decorativo, nunca limite de campo.
- **Cinza 500** (`cinza-500`): borda de campos e de botões secundários (cerca de 5,8:1, acima de 3:1), pontos do pontilhado, placeholder e texto silenciado.
- **Cinza 800** (`cinza-800`): texto de apoio — dicas, subtítulos, linha de apoio das linhas de preço, itens inativos da navegação inferior (cerca de 12:1).
- **Lilás da faixa** (`on-band-muted`): texto secundário e contornos sobre Açaí 900 (cerca de 11:1); usado com 50–60% de opacidade nos contornos de chips e do seletor de idioma.

Tokens derivados em `tokens.css`, sem nova cor: `--color-band-hover` (branco a 10% sobre a faixa) e `--color-overlay` (Açaí 900 a 45%, véu das folhas). O tema de alto contraste troca texto e bordas por preto, a faixa por preto e o Açaí de toque por Açaí 900.

### Named Rules
**The Tucupi-no-Escuro Rule.** Tucupi 400 só existe sobre Açaí 900 (ou preto no alto contraste). Se o fundo é claro, use Tucupi 700 para alerta ou Açaí 700 para destaque.

**The Açaí-de-Toque Rule.** Açaí 700 marca o que se pode pressionar. O contrato de direção pede exclusividade; o build a estende ao código do box, aos ícones de categoria, às barras de progresso e ao ponto de não lida. Telas novas não devem ampliar essa lista.

**The Nunca-Só-Cor Rule.** Todo estado tem texto: selos dizem "Aprovado"/"Pendente", prazo urgente muda cor e peso e diz o tempo, "Em falta" substitui o preço por palavra.

## Typography

**Display Font:** Archivo (Archivo Variable, com fallback `system-ui, sans-serif`)
**Body Font:** Atkinson Hyperlegible Next (Variable, com fallback `system-ui, sans-serif`)

**Character:** Archivo semi-expandida é a letra pintada da placa; Atkinson é a leitura sob sol, de quem não pode errar um número.

Archivo é auto-hospedada como subconjunto latino próprio (`frontend/src/assets/fonts/archivo-display-latin.woff2`, gerado por `scripts/build-display-font.py`) com largura fixa de 112% e eixo de peso 600–800, `font-display: swap`. Atkinson Hyperlegible Next vem do pacote `@fontsource-variable/atkinson-hyperlegible-next` (eixo de peso, subconjunto latino), servida pelo próprio app. Nenhuma fonte de CDN.

### Hierarchy
- **Display** (700, 36 px / `text-3xl`, entrelinha 1,15, −0,01em): título de página (H1 do painel, título da início no celular). No app shell estático o H1 usa 800 e 30 px (44 px a partir de 768 px).
- **Headline** (700, 28 px / `text-2xl`): título da faixa de busca, número grande de cartão do painel.
- **Title** (700, 22 px / `text-xl`, ou 18 px / `text-lg` para o nome da feira no quadro): títulos de seção sobre a régua de 2 px e título de folha.
- **Preço** (700, 18 px, Archivo tabular): valor na linha de preço; código do box no mesmo estilo em Açaí 700.
- **Body** (400, 16 px, entrelinha 1,5): todo o texto corrido; linhas limitadas a 65ch (`--measure`). `text-wrap: pretty` em parágrafos, `balance` em títulos.
- **Body large** (400, 18 px): texto de abertura na faixa e campo de busca grande.
- **Label** (600, 16 px): rótulos de campo, botões, links de navegação.
- **Support** (400 ou 600, 14 px): dicas, linha de apoio, data da verificação, erros de campo (600), rótulos da navegação inferior.

Todo H1/H2/H3 e todo elemento com a classe `display` recebe Archivo, 700, largura 112. Números de dinheiro, hora, contagem e paginação usam `.tabular` (`tabular-nums lining-nums`). Não há texto em caixa alta nem espaçamento de letras positivo.

### Named Rules
**The Número-de-Lousa Rule.** Todo número que se compara (preço, prazo, contagem, código de box, página) é tabular. Preços e códigos de box são Archivo; números de apoio ficam em Atkinson tabular.

**The Duas-Vozes Rule.** Só duas famílias. Archivo nunca em texto corrido; Atkinson nunca em títulos.

## Layout

Coluna única no celular, alinhada à esquerda, com margem lateral de 16 px que respeita as áreas seguras (`.safe-x`: `max(16px, safe-area-inset)`). O conteúdo para em 72rem (`--content-max`), centralizado. Pontos de quebra do Tailwind redefinidos: 40rem (sm), 48rem (md), 64rem (lg).

- **Cabeçalho:** faixa Açaí 900 de 64 px com logo à esquerda e navegação à direita; soma `safe-area-inset-top` no modo instalado.
- **Início:** a faixa continua em um bloco com H1, texto de abertura, busca grande (até 42rem) e termos rápidos; abaixo, em branco, "Feiras" e "Categorias" em duas colunas a partir de md (1,2fr / 1fr).
- **Busca:** faixa curta com H1 e busca; abaixo, filtros em coluna de 16rem à esquerda a partir de lg.
- **Painel:** a partir de lg, navegação lateral de 13rem fixa ao rolar; no celular vira faixa horizontal rolável.
- **Ritmo:** múltiplos de 4 px (`space-1` a `space-12`). Entre seções 32–40 px; dentro de uma seção 8–16 px; linhas do quadro com 12 px de respiro vertical.
- **Colaborador no celular:** navegação inferior fixa de 56 px; o conteúdo ganha 80 px de folga embaixo.
- Altura de tela em `dvh`; o `body` não faz bounce vertical.

**The Régua-Não-Caixa Rule.** Seções se separam por régua de 2 px Açaí 900 sob o título e linhas por régua de 1 px Cinza 200. Agrupar não exige moldura.

## Elevation & Depth

Sistema plano. A profundidade vem do contraste de chãos (faixa Açaí 900 contra branco, rodapé Cinza 50) e das réguas. Sombra existe só em camadas que flutuam sobre o conteúdo.

### Shadow Vocabulary
- **Folha** (`box-shadow: 0 -8px 24px rgb(43 19 48 / 0.16)`): folha que sobe de baixo no celular; também nos toasts.
- **Diálogo** (`box-shadow: 0 12px 32px rgb(43 19 48 / 0.22)`): a mesma folha centrada a partir de sm; também na lista de sugestões da busca.

O sublinhado Açaí 700 do `.fresh-mark` usa `box-shadow: inset` como sublinhado, não como elevação.

**The Só-Flutua Rule.** Sombra só em folha, diálogo, toast e lista suspensa. Linhas, painéis, chips e botões em repouso não têm sombra.

## Shapes

Cantos por papel, não por gosto: **controles** (botões, campos, botões de ícone, seletor de idioma, aviso de desatualização) com 10 px; **painéis** (folha, toast, painéis Açaí 900, cartão de tarefa ativa) com 14 px; **pílula** (999 px) só em chips de filtro, termos rápidos, selos de estado, contador do sino, círculos do stepper e barras de progresso. O anel de foco tem 4 px de raio. Bordas são de 1 px (Cinza 500 em controles, Cinza 200 entre linhas); 2 px só na régua de seção, no passo ativo do stepper e no cartão de tarefa ativa.

## Components

### Buttons
Firmes e diretos; o texto diz o que acontece ("Aceitar por R$ 4,00").
- **Shape:** cantos de controle (10 px), Atkinson 600, ícone opcional à esquerda com 8 px de vão.
- **Tamanhos:** sm e md com 44 px de altura mínima (12 px / 16 px laterais; 14 px / 16 px de texto); lg com 52 px, 20 px laterais e 18 px.
- **Primário:** Açaí 700 com texto branco; hover Açaí 900.
- **Secundário:** fundo branco, borda Cinza 500, texto Açaí 700; hover Açaí 100.
- **Fantasma:** só texto Açaí 700; hover Açaí 100.
- **Perigo:** fundo branco, borda e texto Urucum 600; hover Cinza 50.
- **Sobre a faixa:** Tucupi 400 com texto Açaí 900 (hover com brilho a 95%); versão discreta com contorno lilás a 60% e texto branco, hover branco a 10%.
- **Estados:** transição de fundo, cor e borda em 140 ms; ao pressionar desce 1 px; desabilitado com 55% de opacidade e cursor bloqueado; ocupado troca o ícone por spinner e marca `aria-busy`.

### Chips
- **Filtro de setor:** pílula de 44 px, borda Cinza 500, texto Açaí 900 e contagem tabular em Cinza 800; ativo vira Açaí 700 cheio com texto branco (`aria-pressed`).
- **Termo rápido na faixa:** pílula de 44 px com contorno lilás a 50% e texto branco; hover branco a 10%.
- **Selo de estado (painel):** pílula de 14 px, 600, só contorno e texto na cor do tom (neutro Cinza 500/800, bom Jambu, alerta Tucupi 700, ruim Urucum, marca Açaí 700). Sempre com palavra.

### Inputs / Fields
- **Style:** 48 px de altura, cantos de 10 px, borda de 1 px Cinza 500, fundo branco, 12 px laterais, texto de 16 px; textarea com 112 px; checkbox de 20 px em Açaí 700 dentro de um rótulo de 44 px.
- **Rótulo, dica e erro:** rótulo Atkinson 600 em cima; dica em 14 px Cinza 800 entre rótulo e campo; erro em 14 px 600 Urucum 600 abaixo, com `role="alert"`, dizendo o que fazer. Dica e erro ligados por `aria-describedby`.
- **Erro:** `aria-invalid` troca a borda por Urucum 600.
- **Foco:** anel global de 3 px Açaí 700 com 2 px de afastamento.
- **Busca na faixa:** campo branco sem borda visível (borda de 2 px transparente que vira Tucupi 400 no foco), lupa à esquerda e botão "Buscar" Açaí 700 embutido à direita; 60 px na início, 48 px na busca. Sugestões em lista suspensa com sombra de diálogo.

### Navigation
- **Cabeçalho (faixa):** logo invertida (104 px no celular, 124 px a partir de sm) como link para a início; links Atkinson 600 brancos com 44 px, hover branco a 10%, ativo com sublinhado Tucupi de 2 px afastado 8 px. Botões de ícone (busca, sino) de 44 × 44. Seletor de idioma: só a sigla (PT/EN/ES) e uma seta, contorno lilás a 50%, com `<select>` nativo invisível por cima. Com sessão, no celular, os links de seção saem do topo e passam para a barra inferior.
- **Navegação inferior (colaborador, celular):** quatro colunas de 56 px, borda superior Cinza 200, ícone de 22 px sobre rótulo de 14 px 600; ativo Açaí 700, inativo Cinza 800. Respeita `safe-area-inset-bottom`. Some a partir de md.
- **Painel:** lista de links de 44 px; ativo com fundo Açaí 100, texto 700 e traço Açaí 700 (embaixo no celular, 3 px à esquerda na tela larga, sobre uma régua de 2 px Açaí 900). Paginação com dois botões secundários pequenos e a posição tabular no meio.
- **Rodapé:** Cinza 50 com régua superior, logo normal de 104 px, links de 44 px.

### Linha de preço (assinatura)
O elemento que define o sistema. Grade de três colunas: rótulo (Atkinson 600), pontilhado que preenche o espaço restante (no mínimo 24 px) e valor (Archivo 700, 18 px, tabular, sem quebra). O pontilhado são pontos Cinza 500 de 1 px a cada 6 px, a 70% de opacidade, alinhados à linha de base. Abaixo, a linha de apoio em 14 px Cinza 800 com setor, nota "de safra" em Rio 600 e a data da verificação ("verificado hoje", "verificado há N dias", "ainda não verificado"). O preço leva a unidade curta em 16 px 600 Cinza 800 ("R$ 45,00 o quilo"); sem preço aparece "preço no box"; em falta, a palavra substitui o preço e a linha fica silenciada.
- **No quadro da feira:** título da feira em Archivo 18 px sobre régua de 2 px, contagem de boxes à direita; um box por linha, linha inteira clicável, hover e foco em Cinza 50. No celular só o código do box fica na linha do pontilhado; o nome desce para a linha de apoio.
- **Desatualização:** acima de 60 dias aparece uma caixa Cinza 50 de cantos de 10 px com ícone e texto Tucupi 700. Dados vindos do aparelho dizem quando foram salvos ("dados salvos em 19/10 às 15:40"), em Tucupi 700 quando antigos.
- **"Atualizado até ser visto": só na página do box.** `useFreshRows` (`src/shared/offline/seen.ts`) guarda no aparelho a versão de cada oferta vista; na visita seguinte, a linha cuja versão mudou recebe `.fresh-mark` (sublinhado de 3 px em Açaí 700, com varredura de 220 ms). Na primeira visita nada é marcado. Outras listas (busca, feira, produto) ainda não usam a marca.

### Stepper e barra de prazo
- **Stepper 1-2-3** (Aceitar, Check-in, Enviar): três colunas, cada uma com trilho de 6 px em pílula (Açaí 700 feito ou atual, Cinza 200 a fazer) e, abaixo, círculo de 24 px com o número em Archivo tabular — cheio Açaí 700 com ✓ em ícone quando feito, contorno de 2 px Açaí 700 quando atual, contorno Cinza 500 quando a fazer — e o rótulo em 14 px 600. O passo atual tem `aria-current="step"`.
- **Barra de prazo:** comprimento proporcional ao tempo que resta entre início e prazo, recalculada a cada 30 s. Rótulo à esquerda e "faltam X · até HH:MM" tabular à direita; trilho Cinza 200 de 8 px, preenchimento Açaí 700, que vira Urucum 600 com texto em negrito nos últimos 15 minutos. É um `progressbar` com valor anunciado.

### Folhas e diálogos
Radix Dialog: foco preso, Esc fecha, título anunciado. No celular sobe de baixo, ocupa a largura, até 90dvh, cantos superiores de 14 px, sombra de folha e folga para a barra de gestos. A partir de sm vira diálogo centrado de até 32rem com cantos de 14 px e sombra de diálogo. Véu Açaí 900 a 45%. Título em Archivo 22 px; botão fechar de 44 × 44 com ícone X.

### Toasts
Até três avisos no topo, centrados, com até 28rem: fundo branco, cantos de 14 px, sombra de folha, borda de 1 px que indica o tom (Jambu sucesso, Urucum erro, Cinza 200 informação), texto de 16 px e botão fechar de 44 px. Entram descendo 6 px em 220 ms. Sucesso e informação em região `polite` e somem em 4,5 s; erro como `alert`, em 7 s. Textos repetidos não se empilham.

### Estados
- **Carregando:** linhas-esqueleto de altura fixa (64 px) com régua entre elas, barras Cinza 50/200 de cantos de 6 px pulsando em 1,4 s; texto oculto anunciado em `role="status"`.
- **Vazio:** bloco alinhado à esquerda com ícone de 22 px Cinza 500, título 600 e corpo Cinza 800 até 65ch, ação opcional.
- **Erro:** mesmo bloco com ícone Urucum, `role="alert"`, título e detalhe do problema (sem código técnico) e botão secundário pequeno "Tentar de novo". Erro 404 vira "não encontrado"; erro de rede vira o estado sem conexão.
- **Sem conexão:** ícone de nuvem cortada, explicação do que fazer e "Tentar de novo". Com dados salvos, a tela mostra esses dados e o aviso de quando foram salvos. Uma faixa Açaí 100 sob o cabeçalho anuncia mudanças de rede.

### Movimento
Tokens: `--duration-fast` 140 ms (transições de botão), `--duration-base` 220 ms (entrada de toast, varredura do `.fresh-mark`), `--ease-out` `cubic-bezier(0.16, 1, 0.3, 1)`. Esqueleto com `ease-in-out` em 1,4 s, spinner em rotação contínua. Com `prefers-reduced-motion: reduce`, as duas durações vão a 0 ms, esqueleto, toast e `.fresh-mark` perdem a animação e o spinner para de girar.

### Acessibilidade
- Alvos de toque de pelo menos 44 px (`--touch`) em botões, links de navegação, chips e botões de ícone.
- Campos com fonte de no mínimo 16 px (o iPhone não dá zoom sozinho).
- Foco visível: contorno de 3 px Açaí 700 com 2 px de afastamento; dentro da faixa (`.on-band`), Tucupi 400. Link "Pular para o conteúdo" como primeiro foco. A cada troca de rota o foco vai para o H1.
- Mapa nunca é o único caminho: as feiras e boxes aparecem primeiro em lista, com endereço e "como chegar" em texto; o mapa só carrega no toque em "Ver no mapa", como região rotulada, com esqueleto de 256 px enquanto chega.
- Tema de alto contraste disponível.

### App shell e performance
- `frontend/index.html` traz CSS crítico inline que repete os tokens (Açaí 900, Tucupi 400, raio de 10 px, 72rem): enquanto o JavaScript chega, a primeira tela já mostra a faixa, a logo invertida, o H1 e uma busca de 56 px que funciona sem script (GET `/search`).
- Só a fonte de texto é pré-carregada (plugin `nandesk:preload-text-font` no `vite.config.ts`); Archivo chega pelo CSS com `swap`.
- O mapa (Leaflet/OpenStreetMap) é um chunk preguiçoso carregado sob demanda; as tiles nunca vão para o service worker.
- O toast é um componente próprio pequeno para caber no orçamento da primeira tela; telas são rotas preguiçosas.

### Marca e imagens
O logotipo é sempre o SVG oficial em curvas (`/brand/svg/nandesk-logo.svg` no claro, `-reversed.svg` na faixa), com largura mínima de 96 px; nunca é redigitado com fonte.

Proveniência de todo raster entregue:
- `frontend/public/brand/png/*.png` (ícones do PWA 192/512, maskable, monocromático, apple-touch 180, logos de 1200 px) e `frontend/public/brand/favicon.ico`: do pacote oficial da marca NANDESK, assim como os SVGs em `frontend/public/brand/svg/`.
- `frontend/public/screenshots/home-narrow.png` (780 × 1688) e `home-wide.png` (1440 × 900), usados no manifesto, e `docs/images/inicio-celular.png`, `busca-celular.png`, `box-celular.png`, `tarefa-celular.png`, `moderacao-desktop.png`: capturados do build real com Playwright (Chromium, `vite preview` em :4173, celular a 390 × 844 com escala 2) em 2026-10-21, com os dados de demonstração. O script de captura está em `frontend/scripts/capturas.mjs`.
- Sem fotos de banco e sem imagens geradas por IA. Não há fotos de produto no build.

## Do's and Don'ts

### Do:
- **Do** montar toda lista de preço ou de dados com a linha de preço: rótulo, pontilhado, valor tabular e a data da verificação.
- **Do** separar seções com a régua de 2 px Açaí 900 e linhas com 1 px Cinza 200.
- **Do** usar só os tokens de `tokens.css` para cor, fonte, espaço e raio: 10 px em controles, 14 px em painéis, pílula só em chips, selos e barras.
- **Do** manter Tucupi 400 sobre Açaí 900 e usar Tucupi 700 para alerta no claro.
- **Do** desenhar os estados carregando, vazio, erro e sem conexão em toda tela, com orientação útil.
- **Do** manter alvos de 44 px, campos de 16 px e o anel de foco de 3 px.
- **Do** mostrar o prazo como barra proporcional ao tempo que resta, com texto do tempo.
- **Do** mover só em resposta a uma ação, em 140–220 ms, com `--ease-out` e respeitando `prefers-reduced-motion`.
- **Do** alinhar à esquerda e fazer hierarquia por tamanho, peso e espaço.
- **Do** usar uma cor de destaque por tela (Açaí 700).

### Don't:
- **Don't** usar degradês decorativos (sobretudo roxo→azul), glassmorphism, desfoques ou "blobs" 3D. O único degradê permitido é o funcional do esqueleto e o padrão de pontos do pontilhado.
- **Don't** usar ilustrações ou fotos geradas por IA nem imagens de banco genéricas.
- **Don't** usar emoji como ícone ou marcador, nem ícone de "brilho/IA".
- **Don't** pôr tudo em cards idênticos com o mesmo raio e a mesma sombra, nem barra colorida na borda esquerda de cards.
- **Don't** centralizar tudo, pôr rótulo em caixa alta acima de cada título, numerar 01/02/03 sem sequência real ou pôr "→" em todo botão.
- **Don't** inventar estatísticas e depoimentos ("+10 mil usuários") nem usar lorem ipsum.
- **Don't** escrever frases de marketing vazias ("revolucione", "a melhor experiência", "solução inovadora").
- **Don't** animar a entrada de cada seção nem mover sem motivo.
- **Don't** usar Inter, Poppins, Montserrat ou Space Grotesk no lugar de Archivo e Atkinson Hyperlegible Next.
- **Don't** pôr Tucupi 400 em texto, ícone ou borda sobre fundo claro (1,87:1).
- **Don't** usar Cinza 200 como borda de campo; campos usam Cinza 500.
- **Don't** dar sombra a linhas, painéis, chips ou botões em repouso.
- **Don't** redigitar o logotipo com fonte, nem aplicar degradê, sombra, brilho ou contorno a ele.
- **Don't** tornar o mapa o único caminho para uma informação.
