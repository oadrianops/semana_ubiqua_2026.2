#!/usr/bin/env sh
# Backup diário (seção 16.8, RNF-06): dump consistente do MySQL + arquivo do volume de uploads, com retenção de
# 7 diários e 4 semanais (domingo). Rodar no servidor, por cron, na pasta do projeto:
#   0 4 * * * cd /caminho/do/projeto && infra/scripts/backup.sh >> backups/backup.log 2>&1
set -eu

COMPOSE="${COMPOSE:-docker compose -f docker-compose.prod.yml}"
DEST="${BACKUP_DIR:-./backups}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$DEST/daily" "$DEST/weekly"

echo "[$stamp] dump do banco"
$COMPOSE exec -T db sh -c 'exec mysqldump --single-transaction --routines --triggers --no-tablespaces \
  -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' | gzip -9 > "$DEST/daily/db-$stamp.sql.gz"

echo "[$stamp] arquivo dos uploads"
$COMPOSE exec -T api sh -c 'tar -C /data -czf - uploads' > "$DEST/daily/uploads-$stamp.tar.gz"

if [ "$(date -u +%u)" = "7" ]; then
  cp "$DEST/daily/db-$stamp.sql.gz" "$DEST/daily/uploads-$stamp.tar.gz" "$DEST/weekly/"
fi

# Retenção: 7 diários e 4 semanais de cada tipo.
for kind in db uploads; do
  ls -1t "$DEST/daily/$kind-"* 2>/dev/null | tail -n +8 | xargs -r rm -f
  ls -1t "$DEST/weekly/$kind-"* 2>/dev/null | tail -n +5 | xargs -r rm -f
done

echo "[$stamp] ok — copie a pasta $DEST para fora do servidor (Should, seção 16.8)"
