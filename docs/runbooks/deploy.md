# Runbook: deploy no Coolify (nandesk.com.br)

Produção roda no Coolify do VPS da equipe, com o `docker-compose.prod.yml` deste repositório. O proxy do Coolify
cuida do domínio, do certificado TLS (Let's Encrypt) e do HTTP/2. Nenhuma porta da aplicação é publicada no host.

```text
navegador ──HTTPS──▶ proxy do Coolify ──▶ web (Nginx, :8080) ──/api──▶ api (Spring Boot, :8080) ──▶ db (MySQL 8.4)
                                              └─ /actuator/health ──▶ api (:8091, só o health)
```

## 1. Antes do primeiro deploy

1. **DNS:** registro `A` de `nandesk.com.br` (e de `www.nandesk.com.br`, se quiser) apontando para o IP do VPS.
   Se o VPS tiver IPv6, crie também o `AAAA`.
2. **Firewall do VPS:** portas 80 e 443 abertas (TCP; UDP 443 se for usar HTTP/3). Nada mais precisa ficar exposto.
3. **Requisitos do VPS:** 2 vCPU e 4 GB de RAM (a API usa até 75% da memória do contêiner; o MySQL, ~400 MB).
4. **Gerar os segredos** numa máquina com `openssl` (Git Bash no Windows serve):

   ```bash
   infra/scripts/gen-jwt-keys.sh        # cria secrets/jwt-private.pem e secrets/jwt-public.pem
   infra/scripts/gen-vapid-keys.sh      # imprime as 3 variáveis do push
   openssl rand -base64 32              # APP_CRYPTO_PIX_KEY
   openssl rand -base64 24              # MYSQL_PASSWORD (repita para MYSQL_ROOT_PASSWORD)
   ```

   A pasta `secrets/` está no `.gitignore`. Guarde as chaves num cofre de senhas: trocar o `APP_CRYPTO_PIX_KEY`
   depois torna ilegíveis as chaves PIX já gravadas.

## 2. Criar o recurso no Coolify

1. **Projects → (seu projeto) → + New → Public Repository** (ou *Private Repository with GitHub App*).
2. Repositório `https://github.com/oadrianops/semana_ubiqua_2026.2`, branch `main`.
3. **Build Pack: Docker Compose** e **Docker Compose Location: `/docker-compose.prod.yml`**.
4. Em **Services**, no serviço **web**, preencha **Domains** com `https://nandesk.com.br:8080`. O `:8080` indica
   a porta do contêiner; o público acessa normalmente em `https://nandesk.com.br`. Para o `www`, acrescente
   `https://www.nandesk.com.br:8080` (o Coolify gera o certificado dos dois).
5. Não preencha domínio nos serviços **api** e **db**: eles só conversam pela rede interna.

## 3. Variáveis de ambiente

O Coolify lista sozinho as variáveis obrigatórias do compose (as que têm `:?`). Preencha em **Environment Variables**:

| Variável | Valor | Observação |
|---|---|---|
| `MYSQL_PASSWORD` | senha forte | obrigatória |
| `MYSQL_ROOT_PASSWORD` | outra senha forte | obrigatória |
| `APP_JWT_PRIVATE_KEY` | conteúdo de `secrets/jwt-private.pem` | marque **Is Multiline?** |
| `APP_JWT_PUBLIC_KEY` | conteúdo de `secrets/jwt-public.pem` | marque **Is Multiline?** |
| `APP_CRYPTO_PIX_KEY` | saída do `openssl rand -base64 32` | obrigatória |
| `APP_PUSH_VAPID_PUBLIC_KEY` / `APP_PUSH_VAPID_PRIVATE_KEY` | saída do `gen-vapid-keys.sh` | sem elas o push fica desligado |
| `APP_PUBLIC_BASE_URL` | `https://nandesk.com.br` | links dos e-mails |
| `APP_SEED_ENABLED` | `true` só para a demonstração do hackathon | carrega dados fictícios (ADR-0017) |
| `SEED_DEFAULT_PASSWORD` | `demo-nandesk-2026` | senha das contas de teste publicada no README; se trocar, atualize o README |
| `SEED_ADMIN_PASSWORD` | senha só da administração (≥ 12) | **não divulgue** |
| `APP_MAIL_ENABLED` | `false` (sem SMTP) ou `true` | sem SMTP, contas novas entram verificadas |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | dados do SMTP | só com `APP_MAIL_ENABLED=true` |
| `APP_MAIL_FROM` | `nao-responda@nandesk.com.br` | remetente |
| `WEB_DEMO_NOTICE` | `true` enquanto os dados forem fictícios | faixa no topo do app |

Opcionais: `WEB_MAP_TILE_URL` (troca o provedor de tiles; ajuste o `img-src` em
`frontend/nginx/security-headers.conf`), `WEB_VITALS_SAMPLE_RATE` e `APP_CONTACT_EMAIL`.

## 4. Deploy

1. Clique em **Deploy**. O primeiro build leva de 5 a 10 minutos (Maven e npm baixam as dependências).
2. A ordem de subida respeita os healthchecks: `db` saudável → `api` pronta (readiness na porta 8091) → `web`.
3. As migrações do Flyway rodam na inicialização da API. Com `APP_SEED_ENABLED=true`, o log mostra
   `Seed de demonstração carregado: 2 feiras, 26 produtos, 12 boxes`.

## 5. Smoke test (depois de cada deploy)

```bash
curl -fsS https://nandesk.com.br/actuator/health                      # {"status":"UP",...}
curl -fsS "https://nandesk.com.br/api/v1/search?q=jambu" | head -c 300   # resultado com a Feira da 25
curl -sI https://nandesk.com.br/ | grep -i "content-security-policy\|strict-transport"
```

No celular: abrir o site, buscar "castanha", abrir um box, salvar o Ver-o-Peso para usar sem internet, ativar o
modo avião e conferir que a feira e a busca continuam funcionando, com a data dos dados.

## 6. Atualizar e voltar versão

- **Atualizar:** push na `main` e **Redeploy** (ou ative *Auto Deploy* no recurso). Também dá para usar o webhook do
  Coolify no `release.yml` (segredos `COOLIFY_WEBHOOK` e `COOLIFY_TOKEN`, com aprovação no Environment `production`).
- **Voltar versão:** em **Deployments**, escolha o deploy anterior e **Redeploy**. É seguro porque as migrações são
  só aditivas (expandir → contrair, seção 16.9).
- **Desligar a demonstração:** `APP_SEED_ENABLED=false` e `WEB_DEMO_NOTICE=false`. O seed não apaga dados; para
  começar do zero com dados reais, restaure um banco vazio (ver `restore.md`).

## 7. Lançar um levantamento de campo (boxes reais)

Boxes conferidos na feira entram pelo painel (Painel → Boxes → Cadastrar box) ou, para um lote, pelo script
`infra/scripts/importar-campo.mjs` (ADR-0018). Nomes, fotos e termos **nunca** entram no repositório: o manifesto e as
fotos ficam em `file/campo/` (ignorado pelo Git) e vão direto para o volume `uploads`.

1. Fotos: sem pessoas de rosto (recorte ou desfoque antes), JPEG ou PNG até 5 MB; o servidor recodifica e tira o EXIF.
2. Manifesto: feira (endereço, posição, horários, textos, setores novos e capa) e boxes (número, setor, nome do box,
   nome do feirante só se o termo autorizar, forma do consentimento, Instagram, ofertas e fotos).
3. Simule e depois rode, com a senha da administração em variável de ambiente (nunca na linha de comando):

   ```bash
   export NANDESK_ADMIN_PASSWORD='...'   # a de SEED_ADMIN_PASSWORD no Coolify
   node infra/scripts/importar-campo.mjs file/campo/feira-da-25.json --api https://nandesk.com.br --dry-run
   node infra/scripts/importar-campo.mjs file/campo/feira-da-25.json --api https://nandesk.com.br
   unset NANDESK_ADMIN_PASSWORD
   ```

   Pode rodar de novo: box existente não é recriado, ofertas são regravadas e fotos só sobem para box sem foto.
4. Se o feirante mudar de ideia: Painel → Boxes → o box → "Revogar consentimento e tirar do ar". O box sai na hora e
   as fotos são apagadas pela retenção em até 30 dias (RN-13).

## 8. Backup

Os volumes `db-data` e `uploads` persistem entre deploys. Backup diário pelo cron do VPS, na pasta onde o Coolify
clonou o projeto (veja o caminho em **Configuration → General**), ou num diretório com uma cópia do repositório:

```bash
0 4 * * * cd /caminho/do/projeto && COMPOSE="docker compose -p <nome-do-projeto-no-coolify> -f docker-compose.prod.yml" infra/scripts/backup.sh >> backups/backup.log 2>&1
```

Retenção: 7 diários e 4 semanais. Copie a pasta `backups/` para fora do servidor. A restauração está em `restore.md`.

## 9. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `web` não sobe, esperando a `api` | API sem ficar pronta | Logs da `api`: senha do banco, `APP_CRYPTO_PIX_KEY` com tamanho errado ou PEM colado sem quebras de linha |
| Login funciona e cai a cada deploy | Sem `APP_JWT_PRIVATE_KEY`/`PUBLIC_KEY` (par efêmero) | Preencha as duas variáveis PEM |
| 403 ao entrar | `APP_PUBLIC_BASE_URL` diferente do domínio | Use exatamente `https://nandesk.com.br` |
| Mapa sem imagem | CSP bloqueando o provedor de tiles | Ajuste o `img-src` do `security-headers.conf` |
| Push não chega no iPhone | O iOS só entrega push com o app instalado na tela de início | Instalar pelo "Compartilhar → Adicionar à Tela de Início" |
