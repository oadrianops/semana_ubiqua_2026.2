# Como contribuir

## Fluxo

- **GitHub Flow:** a `main` está sempre pronta para deploy. Trabalhe em branches curtas: `feat/RF-02-busca`,
  `fix/RN-05-checkin`, `docs/...`, `chore/...`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/pt-br/) em PT-BR, com os IDs no fim:
  `feat(search): agrupa resultados por feira e box (RF-02)`.
- **PRs:** use o modelo (`.github/pull_request_template.md`), com o resumo da revisão de design (impeccable
  `audit` + `critique`) quando mudar interface. Pelo menos 1 aprovação e todos os checks verdes; squash merge.
- **Mudança de requisito:** abra a issue "Mudança de requisito" antes de mudar o comportamento descrito na
  especificação.

## Rodando localmente

| O quê | Comando |
|---|---|
| MySQL + Mailpit | `docker compose up -d` |
| API (perfil dev, com seed) | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |
| Web (Vite, proxy de `/api`) | `cd frontend && npm ci && npm run dev` → http://localhost:5173 |
| Stack completo como em produção | `docker compose --profile app up -d --build` → http://localhost:8081 |

No Windows, use o Git Bash para os scripts `.sh` e `mvnw.cmd` no PowerShell.

## Verificação (Definition of Done)

```bash
cd backend && ./mvnw verify      # unitários, *IT com MySQL (Testcontainers), Spotless, JaCoCo, ArchUnit, OpenAPI
cd frontend && npm run ci        # lint, tipos, testes com cobertura, build e orçamentos de tamanho
cd frontend && npm run e2e       # jornadas J1 a J8 (precisa da API em :8080)
```

**Sem Docker:** os `*IT` aceitam um MySQL 8.4 vazio em vez do Testcontainers:

```bash
export IT_JDBC_URL=jdbc:mysql://127.0.0.1:3306/nandesk_it IT_JDBC_USER=nandesk_app IT_JDBC_PASSWORD=troque-me
cd backend && ./mvnw verify
```

`./mvnw verify -Pno-docker` pula os `*IT` (só para iterar rápido; o CI sempre roda tudo).

Se a API mudou, rode os `*IT` localmente (eles regravam `docs/api/openapi.json`) e depois
`cd frontend && npm run api:generate`. Atualize a rastreabilidade com `python infra/scripts/rastreabilidade.py`.

## Regras do projeto

Estão em `CLAUDE.md` e `.claude/rules/`. As principais: sem pagamento online; esquema só por Flyway (nunca editar
uma migração já aplicada); textos só via i18n nos 3 idiomas; cores, fontes e espaçamentos só dos tokens; envios do
colaborador só pela fila; nenhum segredo no repositório; a pasta `file/` nunca é versionada.
