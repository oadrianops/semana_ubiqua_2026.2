#!/usr/bin/env sh
# Restauração (docs/runbooks/restore.md): carrega um dump do banco e, opcionalmente, o arquivo dos uploads.
# Uso: infra/scripts/restore.sh backups/daily/db-AAAAMMDDTHHMMSSZ.sql.gz [backups/daily/uploads-....tar.gz]
set -eu

[ $# -ge 1 ] || { echo "uso: $0 <db.sql.gz> [uploads.tar.gz]" >&2; exit 2; }
COMPOSE="${COMPOSE:-docker compose -f docker-compose.prod.yml}"

echo "Isto substitui os dados atuais. Digite RESTAURAR para continuar:"
read -r answer
[ "$answer" = "RESTAURAR" ] || { echo "cancelado"; exit 1; }

echo "Parando a API para não gravar durante a restauração"
$COMPOSE stop api web

gunzip -c "$1" | $COMPOSE exec -T db sh -c 'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
echo "Banco restaurado de $1"

if [ $# -ge 2 ]; then
  $COMPOSE run --rm --no-deps -T --entrypoint sh api -c 'rm -rf /data/uploads/* && tar -C /data -xzf -' < "$2"
  echo "Uploads restaurados de $2"
fi

$COMPOSE up -d api web
echo "Pronto. Confira: curl -fsS https://nandesk.com.br/actuator/health"
