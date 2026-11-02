# syntax=docker/dockerfile:1.7
# Web da NANDESK (seção 16.3): build com Node 24, runtime com Nginx não-root. O /config.js é gerado a cada
# inicialização a partir das variáveis WEB_*, então a mesma imagem serve qualquer ambiente.

FROM node:24.22.2-alpine3.24 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build \
    && find dist -type f \( -name '*.js' -o -name '*.mjs' -o -name '*.css' -o -name '*.html' -o -name '*.svg' -o -name '*.json' -o -name '*.webmanifest' \) \
       -size +1k -exec gzip -9 -k {} \;

FROM nginxinc/nginx-unprivileged:1.31.7-alpine3.24
LABEL org.opencontainers.image.title="nandesk-web" \
      org.opencontainers.image.description="PWA da NANDESK: feiras de Belém" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/oadrianops/semana_ubiqua_2026.2"
USER root
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --chmod=755 nginx/30-config-js.sh /docker-entrypoint.d/30-config-js.sh
COPY --from=build --chown=101:101 /app/dist /usr/share/nginx/html
USER 101
ENV API_UPSTREAM=http://api:8080 \
    API_HEALTH_UPSTREAM=http://api:8091 \
    NGINX_ENVSUBST_FILTER=^API_
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
