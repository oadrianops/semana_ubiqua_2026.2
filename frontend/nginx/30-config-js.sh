#!/bin/sh
# Gera /config.js a partir das variáveis WEB_* a cada inicialização (seção 16.3): a mesma imagem serve
# desenvolvimento, homologação e produção. Valores com aspas ou barra invertida são escapados.
set -eu

json_string() {
  printf '"%s"' "$(printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')"
}

json_bool() {
  case "$1" in
    true | TRUE | 1 | yes) printf 'true' ;;
    *) printf 'false' ;;
  esac
}

json_number() {
  case "$1" in
    '' | *[!0-9.]*) printf '%s' "$2" ;;
    *) printf '%s' "$1" ;;
  esac
}

target="${CONFIG_JS_TARGET:-/usr/share/nginx/html/config.js}"

# Padrões em variáveis próprias: dentro de ${VAR:-padrão} o primeiro "}" de {z} ou {lat} encerraria a expansão e
# cortaria a URL (mapa sem tiles e "Como chegar" com destino "{lat,{lng}}").
default_tiles='https://tile.openstreetmap.org/{z}/{x}/{y}.png'
default_directions='https://www.google.com/maps/dir/?api=1&destination={lat},{lng}'

cat >"$target" <<EOF
// Gerado na inicialização do contêiner (nginx/30-config-js.sh). Não editar.
window.__APP_CONFIG__ = {
  appName: $(json_string "${WEB_APP_NAME:-NANDESK}"),
  apiBaseUrl: $(json_string "${WEB_API_BASE_URL:-/api}"),
  mapTileUrl: $(json_string "${WEB_MAP_TILE_URL:-$default_tiles}"),
  mapAttribution: $(json_string "${WEB_MAP_ATTRIBUTION:-© OpenStreetMap contributors}"),
  directionsUrlTemplate: $(json_string "${WEB_DIRECTIONS_URL_TEMPLATE:-$default_directions}"),
  contactEmail: $(json_string "${WEB_CONTACT_EMAIL:-contato@nandesk.com.br}"),
  vapidPublicKey: $(json_string "${WEB_VAPID_PUBLIC_KEY:-}"),
  webVitalsSampleRate: $(json_number "${WEB_VITALS_SAMPLE_RATE:-0.1}" 0.1),
  demoNotice: $(json_bool "${WEB_DEMO_NOTICE:-true}"),
};
EOF

# Remove a versão pré-comprimida antiga para o gzip_static não servir a configuração do build.
rm -f "$target.gz"
echo "30-config-js.sh: config.js gerado"
