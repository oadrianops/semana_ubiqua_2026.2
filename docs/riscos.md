# Registro de riscos

Probabilidade (P) e impacto (I): A = alto, M = médio, B = baixo. Revisar a cada sprint (seção 17).

| ID | Risco | P | I | Mitigação | Situação em 2026-10-21 |
|---|---|---|---|---|---|
| R1 | Poucos colaboradores ativos | M | A | Começar com a equipe e a turma; reputação visível; divulgação | Aberto: sem colaboradores reais ainda |
| R2 | Fraude em envios | M | A | Cerca da feira, fotos com hash e aviso de foto repetida, moderação, reputação, limite diário | Mitigado no código (IT de idempotência e duplicidade) |
| R3 | GPS impreciso dentro das feiras | A | M | Cerca por feira com tolerância; alerta de baixa precisão para a moderação | Mitigado; falta medir em campo |
| R4 | Sinal instável nas feiras | A | A | Snapshot offline, fila de envios, fotos uma a uma, app shell, perfis de rede no E2E | Mitigado (J6, J7, J8 verdes) |
| R5 | Falta de verba para comissões | M | A | Modo voluntário, orçamento com teto, indicadores | Mitigado no código |
| R6 | Feirantes não consentirem | M | A | Abordagem presencial, termo de uma página, remoção fácil | Aberto: validação com feirantes pendente (CA-5) |
| R7 | Não conformidade com a LGPD | B | A | Inventário, retenção automática, exportação e exclusão | Mitigado; textos legais sem revisão jurídica |
| R8 | Bloqueio ou limite dos tiles do OSM | B | M | URL configurável, sem cache de tiles | Monitorar com tráfego real |
| R9 | Prazo apertado | A | M | Entregas demonstráveis; cortar Could antes de Should | Entregue até a data |
| R10 | Incompatibilidade de versões | M | M | ADR-0002, Dependabot, ITs com banco real | Ocorreu e foi tratado: Jackson 3, Boot 4, TypeScript 5.9 |
| R11 | Traduções incorretas em EN/ES | M | B | Paridade de chaves no CI, completude no catálogo, fallback PT-BR | Aberto: falta revisão por falantes |
| R12 | Aparência genérica de IA | M | M | Direção "Lousa de preços", skill impeccable, checklist de design | Mitigado (ADR-0016) |
| R13 | Performance piora a cada funcionalidade | A | A | Orçamentos no CI, J8, Web Vitals de campo | Mitigado (142,7 KB de JS inicial) |
| R14 | Dados fictícios confundidos com reais | M | A | Faixa "Dados de demonstração", ADR-0017, README | Mitigado; desligar ao entrar com dados reais |
| R15 | Contas de teste públicas usadas para vandalizar a demonstração | M | M | Senha separada para a administração; limites de taxa; auditoria | Mitigado; trocar senhas após o hackathon |
