#!/usr/bin/env sh
# Gera o par de chaves RSA 3072 do JWT (ADR-0006) em ./secrets e imprime as variáveis para o .env / Coolify.
# Uso: infra/scripts/gen-jwt-keys.sh [pasta]
set -eu

dir="${1:-secrets}"
mkdir -p "$dir"
umask 077
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out "$dir/jwt-private.pem" 2>/dev/null
openssl pkey -in "$dir/jwt-private.pem" -pubout -out "$dir/jwt-public.pem"

echo "Chaves geradas em $dir/ (a pasta secrets/ está no .gitignore)."
echo
echo "Opção 1 — por arquivo (Docker secrets / volume):"
echo "  APP_JWT_PRIVATE_KEY_PATH=/run/secrets/jwt-private.pem"
echo "  APP_JWT_PUBLIC_KEY_PATH=/run/secrets/jwt-public.pem"
echo
echo "Opção 2 — por variável multilinha (Coolify): cole o conteúdo de cada arquivo em"
echo "  APP_JWT_PRIVATE_KEY  ← $dir/jwt-private.pem"
echo "  APP_JWT_PUBLIC_KEY   ← $dir/jwt-public.pem"
