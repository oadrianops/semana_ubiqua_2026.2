# Runbook: restauração de backup

Metas (RNF-06): RPO ≤ 24 h (backup diário) e RTO ≤ 4 h. Teste a restauração uma vez por mês num ambiente separado
e anote o resultado no fim deste arquivo.

## Restaurar em produção

1. Escolha os arquivos em `backups/daily/` (ou `weekly/`): `db-AAAAMMDDTHHMMSSZ.sql.gz` e o `uploads-*.tar.gz` do mesmo horário.
2. Na pasta do projeto no VPS:

   ```bash
   COMPOSE="docker compose -p <projeto-no-coolify> -f docker-compose.prod.yml" \
     infra/scripts/restore.sh backups/daily/db-20261021T070000Z.sql.gz backups/daily/uploads-20261021T070000Z.tar.gz
   ```

   O script pede a palavra `RESTAURAR`, para `api` e `web`, carrega o banco, restaura as fotos e sobe tudo de novo.
3. Smoke test do `deploy.md` (seção 5).

## Testar a restauração (mensal)

1. Num computador com Docker: `docker compose up -d db`.
2. `gunzip -c db-....sql.gz | docker compose exec -T db sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" nandesk'`.
3. `docker compose --profile app up -d api web` e conferir a busca e um box.
4. Anotar data, arquivo usado, tempo total e problemas.

## Registro dos testes

| Data | Backup usado | Tempo | Resultado |
|---|---|---|---|
| — | — | — | Ainda não executado em produção |
