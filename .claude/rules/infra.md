---
paths:
  - "infra/**"
  - "docker-compose*.yml"
  - "**/Dockerfile"
  - ".github/**"
  - "frontend/nginx/**"
---
# Infraestrutura
- Imagens não-root, multi-stage, sem segredos embutidos; tags de imagem fixadas (ADR-0002).
- Configuração só por variáveis de ambiente; atualizar o .env.example ao criar uma variável.
- Nginx: incluir security-headers.conf em cada location com add_header; SSE sem buffering.
- Workflows com permissões mínimas; deploy de produção só com aprovação no Environment.
- Produção no Coolify com `docker-compose.prod.yml` (TLS e domínio pelo proxy do Coolify).
