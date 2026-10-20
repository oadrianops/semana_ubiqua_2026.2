---
paths:
  - "backend/**"
---
# Backend
- Módulos por funcionalidade com camadas api/application/domain/infra (especificação 9.3); respeitar as regras ArchUnit (`ArchitectureTest`).
- DTOs são records; controllers não devolvem entidades; injeção por construtor (campos `final`).
- @Transactional só em serviços de application (`readOnly = true` em consultas); efeitos colaterais via @TransactionalEventListener(AFTER_COMMIT).
- Erros como ProblemDetail com `code` do catálogo 11.4 (`ErrorCode`) e mensagens em `messages*.properties` nos 3 idiomas.
- Usar o bean Clock; nunca Instant.now() direto.
- Regras de negócio em políticas puras de `domain` com teste `@DisplayName("RN-xx: ...")` escrito antes da implementação.
- Spring Boot 4: starters modulares (`spring-boot-starter-webmvc`), Jackson 3 (`tools.jackson.*`), Security 7 só com DSL de lambdas, @MockitoBean (especificação 8.5).
- Logs sem tokens, senhas, chave PIX ou e-mail completo (`Masking.email`).
