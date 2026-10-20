---
paths:
  - "frontend/**"
---
# Frontend
- Dados do servidor só via TanStack Query; cliente gerado do OpenAPI (`npm run api:generate`) em `src/shared/api`.
- Nenhum texto fixo: tudo em `src/shared/i18n/locales/{pt-BR,en,es}`; manter a paridade de chaves (teste `i18n-parity`).
- Acessibilidade WCAG 2.2 AA; todo mapa com alternativa em lista; alvos ≥ 44 px; campos com fonte ≥ 16 px.
- Token de acesso só em memória; uma única renovação simultânea em 401.
- Mapa: URL de tiles da configuração em tempo de execução (`window.__APP_CONFIG__`), atribuição visível, sem cache de tiles no service worker.
- Só tokens de `src/shared/ui/tokens.css`; fontes Archivo (títulos) e Atkinson Hyperlegible Next (texto) via Fontsource.
- Skill `impeccable` e checklist da especificação 14.7 em toda tela; nada da lista "Evitar".
- Dados do snapshot sempre com a data da última atualização; envios só pela fila (outbox), nunca direto.
- Antes de concluir: `npm run size` dentro dos orçamentos e jornada no perfil de rede "Feira lotada".
