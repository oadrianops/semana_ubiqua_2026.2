#!/usr/bin/env sh
# Gera as chaves VAPID (P-256) do Web Push (ADR-0010) no formato que a API espera: Base64 URL sem padding,
# chave pública não comprimida (65 bytes) e privada como o escalar d (32 bytes).
set -eu

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
openssl ecparam -name prime256v1 -genkey -noout -out "$tmp" 2>/dev/null

b64url() { base64 | tr -d '\n=' | tr '+/' '-_'; }

# DER da chave privada EC: 30 77 02 01 01 04 20 <d: 32 bytes> ...
private="$(openssl ec -in "$tmp" -outform DER 2>/dev/null | tail -c +8 | head -c 32 | b64url)"
# DER da chave pública: os últimos 65 bytes são o ponto não comprimido 04||X||Y.
public="$(openssl ec -in "$tmp" -pubout -outform DER 2>/dev/null | tail -c 65 | b64url)"

echo "APP_PUSH_VAPID_PUBLIC_KEY=$public"
echo "APP_PUSH_VAPID_PRIVATE_KEY=$private"
echo "APP_PUSH_VAPID_SUBJECT=mailto:contato@nandesk.com.br"
