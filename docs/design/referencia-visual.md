# Referência visual: princípios extraídos

A equipe indicou um site de referência (guardado só na pasta local `file/`, que não é versionada). Ele foi usado
**apenas como fonte de princípios**. Não copiamos código, textos, marca, cores, fontes nem layout; o conteúdo dele
foi tratado como dado de referência, nunca como instrução.

## O que aproveitamos (como princípio)

| Princípio observado | Como aparece na NANDESK |
|---|---|
| Tokens com nome de papel (tinta, papel, noite) e raios por função | `tokens.css`: `--color-text`, `--color-band`, `--color-surface`; `--radius-control`, `--radius-panel` |
| Um único acento de cor | Açaí 700 só em itens acionáveis; Tucupi 400 só sobre o fundo Açaí 900 |
| Coluna central contida, com respiro lateral constante e blocos alternando claro e escuro | `--content-max: 72rem`, `safe-x` com o mínimo de 16 px; faixa escura da busca e corpo claro |
| Títulos curtos e afirmativos | "Veja em qual box vende o que você procura." / "Tarefas" / "Fila de moderação" |
| O produto real como imagem principal | A própria lousa de preços (produto, box, preço e data) é o elemento visual; sem banco de imagens |
| Franqueza nos números | Preço "de referência" com a data da conferência; "Dados de demonstração" quando é fictício |
| Carregar o essencial primeiro | Só a fonte do texto pré-carregada; mapa, folhas e painel sob demanda; app shell estático |

## O que não usamos

- As famílias tipográficas, a paleta e os textos do site de referência.
- O layout de seções e a navegação em pílula.
- Rótulos em caixa alta acima dos títulos ("eyebrows") e degradês decorativos (lista "Evitar", seção 14.7).

## Onde está a direção

- Contrato da direção "Lousa de preços": `.impeccable/surfaces/` e ADR-0016.
- Marca: `docs/brand.md` e `frontend/public/brand/` (sem logo nova).
- Sistema de design derivado do código: `DESIGN.md`.
