# NANDESK — marca

O toldo de feira cobre o primeiro N: a banca vira a letra. O mesmo N com toldo é o símbolo e o ícone do app.

## Arquivos

| Arquivo | Uso |
|---|---|
| `svg/nandesk-logo.svg` | Logotipo principal, fundos claros |
| `svg/nandesk-logo-reversed.svg` | Fundos escuros (letras brancas, toldo tucupi) |
| `svg/nandesk-logo-mono.svg` / `-mono-white.svg` | Uma cor só (carimbo, impressão em uma cor, gravação) |
| `svg/nandesk-symbol.svg` / `-symbol-mono.svg` | Símbolo (N com toldo) quando o espaço é pequeno |
| `svg/app-icon.svg`, `svg/app-icon-maskable.svg`, `svg/app-icon-monochrome.svg` | Fontes dos ícones do PWA |
| `svg/favicon.svg`, `favicon.ico` | Aba do navegador |
| `png/pwa-192x192.png`, `png/pwa-512x512.png` | Ícones do manifesto (`any`) |
| `png/maskable-icon-512x512.png` | Ícone adaptável do Android (`maskable`, dentro da zona segura) |
| `png/monochrome-icon-512x512.png` | Ícone monocromático (`monochrome`) |
| `png/apple-touch-icon-180x180.png` | Tela de início do iPhone |
| `png/nandesk-logo-1200.png`, `png/nandesk-logo-reversed-1200.png` | README, documentos, slides |

O logotipo está em curvas (não depende de fonte instalada). Desenho tipográfico: Archivo (SIL Open Font License), peso 800, largura 112.

## Cores

| Nome | Hex | Uso |
|---|---|---|
| Açaí 900 | `#2B1330` | Texto principal; versão de uma cor |
| Açaí 700 | `#4A2152` | Cor da marca; botões primários; links |
| Açaí 100 | `#F3EDF4` | Superfícies suaves |
| Tucupi 400 | `#F2B23A` | Toldo da logo; destaques sobre fundo escuro; fundo de selos com texto Açaí 900 |
| Tucupi 700 | `#7A4F00` | Texto de alerta |
| Jambu 600 | `#2F6B2F` | Sucesso |
| Urucum 600 | `#B3381C` | Erro |
| Rio 600 | `#1E5A73` | Informação |
| Cinza 50 / 200 / 500 / 800 | `#F6F4F7` / `#E4E0E6` / `#6B6370` / `#3B353E` | Superfícies, divisórias, texto secundário, texto |

Contraste (WCAG): todas as cores de texto acima passam de 4,5:1 sobre branco. **Tucupi 400 sobre branco tem 1,87:1** — nunca usar em texto ou ícone com significado sobre fundo claro. Bordas de campos usam Cinza 500 (≥ 3:1); Cinza 200 só em divisórias decorativas.

## Tipografia da interface

- **Archivo** (semi-expandida, 600–800): títulos e números de destaque.
- **Atkinson Hyperlegible Next** (400/600/700): texto, formulários e interface — escolhida pela legibilidade em tela pequena e sob sol forte.
- Ambas com licença SIL OFL, servidas pelo próprio app via Fontsource (`@fontsource-variable/archivo`, `@fontsource/atkinson-hyperlegible-next`).

## Regras de uso

- **Área de proteção:** em volta da logo, deixe livre pelo menos a altura do toldo (faixa + babado).
- **Tamanho mínimo:** logotipo com 96 px de largura na tela (25 mm impresso); símbolo com 16 px.
- **Não:** aplicar degradê, sombra, brilho ou contorno; distorcer, girar ou inclinar; trocar as cores do toldo ou das letras fora das versões acima; redesenhar o logotipo digitando com uma fonte; colocar sobre foto sem uma área de fundo liso com contraste.

## Manifesto do PWA (trecho)

```json
"icons": [
  { "src": "/brand/png/pwa-192x192.png", "sizes": "192x192", "type": "image/png" },
  { "src": "/brand/png/pwa-512x512.png", "sizes": "512x512", "type": "image/png" },
  { "src": "/brand/png/maskable-icon-512x512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable" },
  { "src": "/brand/png/monochrome-icon-512x512.png", "sizes": "512x512", "type": "image/png", "purpose": "monochrome" }
],
"theme_color": "#4A2152",
"background_color": "#FFFFFF"
```
