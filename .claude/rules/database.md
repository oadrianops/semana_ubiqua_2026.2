---
paths:
  - "backend/src/main/resources/db/**"
---
# Banco de dados
- Nova mudança = nova migração `V<n>__<descricao>.sql`; nunca editar uma já aplicada.
- utf8mb4 / utf8mb4_0900_ai_ci; nomes fk_/idx_/uk_; enums como VARCHAR; dinheiro DECIMAL(10,2); datas DATETIME(6) em UTC.
- Mudanças compatíveis com a versão anterior (expandir → migrar → contrair).
- Sem ON DELETE CASCADE em dados de negócio; desativação por `status`/`active`.
