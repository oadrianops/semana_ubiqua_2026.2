<p align="center">
  <img src="frontend/public/brand/svg/nandesk-logo.svg" alt="NANDESK" width="320" />
</p>

<p align="center"><strong>Em qual feira e em qual box encontrar o que você procura no Ver-o-Peso e na Feira da 25.</strong></p>

<p align="center">
  <a href="https://nandesk.com.br">nandesk.com.br</a> ·
  <a href="docs/ESPECIFICACAO.md">Especificação</a> ·
  <a href="docs/arquitetura.md">Arquitetura</a> ·
  <a href="docs/runbooks/deploy.md">Deploy</a>
</p>

---

**NANDESK** é um app web instalável (PWA) que mostra em qual feira, setor e box de Belém vende cada produto
(castanha-do-pará, jambu, banho de cheiro, tucupi…), com o **preço de referência e a data em que foi conferido**.
Os dados são mantidos por uma rede de **colaboradores** que vão até a feira, fazem check-in, fotografam e enviam a
atualização, e recebem uma comissão por envio aprovado. O **feirante não precisa operar nada**: ele só autoriza
aparecer. O app foi feito para o celular e para a rede da feira: abre rápido com sinal fraco e funciona sem internet
nas feiras salvas.

Projeto da equipe **Adriano Pinheiro da Silva, Leonardo Farias de Souza e Victor Santiago dos Santos Abreu** para o
Hackathon Semana Ubíqua 2026.2 (UNAMA).

> **Dados de demonstração.** Os boxes, nomes e preços do app são fictícios, criados para a banca testar. O app
> mostra esse aviso no topo de todas as páginas. A validação com feirantes ainda não aconteceu (veja Limitações).

## Objetivo

Turistas e moradores perdem tempo (e os feirantes perdem vendas) porque não há como saber, antes de andar a feira
inteira, onde está um produto. A NANDESK responde isso em segundos, em português, inglês ou espanhol, e dá
visibilidade digital ao feirante sem tirar tempo dele.

## Imagens

| Início (celular) | Busca | Box |
|---|---|---|
| ![Início](docs/images/inicio-celular.png) | ![Busca](docs/images/busca-celular.png) | ![Box](docs/images/box-celular.png) |

| Tarefa do colaborador | Moderação (antes e depois) |
|---|---|
| ![Tarefa](docs/images/tarefa-celular.png) | ![Moderação](docs/images/moderacao-desktop.png) |

## Funcionalidades

**Para quem visita a feira (sem conta)**
- Busca com sugestões, sem acento e com sinônimos em PT, EN e ES ("brazil nut", "castaña de Brasil"), e "você quis dizer".
- Para cada produto: feiras, setores e boxes, com preço de referência e "verificado há N dias".
- Feiras com mapa (OpenStreetMap) e alternativa em lista, horários e "como chegar".
- **Sem internet:** "Salvar para usar sem internet" guarda a feira no celular; a busca funciona offline e mostra
  a data dos dados.
- "Informação errada?" para avisar sobre um box.
- Instalação como app (Android e iPhone) e 3 idiomas.

**Para colaboradores**
- Tarefas por feira e tipo, ordenáveis pela distância (a posição não é gravada).
- Tarefa em 3 passos: **Aceitar → Fazer check-in → Enviar**. O check-in confere pelo GPS se a pessoa está na feira,
  e os prazos aparecem como barras.
- Fotos comprimidas no aparelho e enviadas uma a uma; sem sinal, o envio fica na fila e segue sozinho.
- Cadastro de box novo com o consentimento do feirante.
- Histórico com o motivo de cada recusa, extrato de comissões, reputação, notificações (no app e push) e perfil com
  chave PIX.

**Para moderação e administração**
- Fila de moderação com antes e depois, conflitos de versão, fotos restritas e alerta de GPS impreciso.
- Denúncias, sugestões de produto, tarefas, feiras (mapa e raio do check-in), boxes e consentimento, catálogo nos 3 idiomas.
- Regras de comissão, orçamento do mês, saldos e **registro** de repasses (o pagamento é feito por PIX fora do app;
  não há pagamento online), usuários, auditoria e indicadores.

## Tecnologias

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Security com JWT RS256, Data JPA, Validation, Actuator), Flyway, MySQL 8.4, Caffeine, Bucket4j, springdoc (OpenAPI), Web Push próprio (RFC 8291) |
| Frontend | React 19, TypeScript, Vite 8, PWA (Workbox, `injectManifest`), Tailwind CSS 4, TanStack Query, React Router, i18next, Leaflet, IndexedDB |
| Testes | JUnit 6, AssertJ, Testcontainers, ArchUnit, JaCoCo, Vitest, Testing Library, MSW, Playwright, axe-core, k6 |
| Infra | Docker (imagens não-root), Nginx, Docker Compose, Coolify, GitHub Actions, Prometheus e Grafana |

Versões exatas e justificativas no [ADR-0002](docs/adr/0002-versoes-da-stack.md).

## Demonstração

- **App:** https://nandesk.com.br
- **Vídeo de pitch:** _link do YouTube a incluir_
- **Credenciais de teste** (contas do seed de demonstração, sem acesso à administração):

| Papel | E-mail | Senha |
|---|---|---|
| Colaboradora | `ana@nandesk.local` | `demo-nandesk-2026` |
| Colaborador | `beto@nandesk.local` | `demo-nandesk-2026` |
| Moderação | `moderador@nandesk.local` | `demo-nandesk-2026` |

Para testar a tarefa completa no computador, permita a localização e simule a posição do Ver-o-Peso
(−1.4525, −48.5035) nas ferramentas do navegador (DevTools → Sensors). Também é possível criar uma conta nova de
colaborador.

## Como rodar

Pré-requisitos: Docker e Docker Compose. Para desenvolver: JDK 21 e Node 24.

```bash
git clone https://github.com/oadrianops/semana_ubiqua_2026.2.git nandesk
cd nandesk
cp .env.example .env
docker compose --profile app up -d --build     # MySQL, Mailpit, API e web
```

Abra http://localhost:8081 (e-mails em http://localhost:8025). O seed de desenvolvimento cria as contas da tabela
acima com a senha `troque-me-dev`.

**Desenvolvimento com recarga automática:**

```bash
docker compose up -d                                                     # só MySQL e Mailpit
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # API em :8080
cd frontend && npm ci && npm run dev                                     # web em :5173
```

**Verificação completa:**

```bash
cd backend && ./mvnw verify      # 128 unitários + 71 integração (MySQL 8.4), JaCoCo, ArchUnit, contrato OpenAPI
cd frontend && npm run ci        # lint, tipos, 87 testes, build e orçamentos de performance
cd frontend && npm run e2e       # jornadas J1 a J8 com Playwright e axe
```

Deploy em produção no Coolify: [docs/runbooks/deploy.md](docs/runbooks/deploy.md).

## Qualidade, medida

| O quê | Resultado |
|---|---|
| Busca encontra os 26 produtos do seed em PT e pelos sinônimos EN/ES (CA-1) | 26/26 |
| Cobertura do backend (linhas) | 91% no total; todos os pacotes de regra ≥ 80% |
| Cobertura do frontend (linhas) | 78,7% |
| JavaScript inicial (gzip) | 143,3 KB (limite 150) |
| Primeira tela no perfil "Feira lotada" (400 ms, 400/150 kbps) | 1,4 s na 1ª visita · 0,9 s na 2ª |
| Rede caindo no meio do envio | 1 upload e 1 envio, sem duplicar (J6) |
| Acessibilidade (axe, "serious"/"critical") | 0 |

## Limitações (honestas)

- **Dados fictícios.** Os 12 boxes, nomes e preços são ilustrativos. A posição da Feira da 25 é aproximada.
- **Validação com feirantes (CA-5) ainda não feita.** Roteiro e ata modelo em [docs/validacao/](docs/validacao/).
- **Sem fotos reais.** Não usamos banco de imagens nem imagens geradas por IA; as fotos chegam pelos colaboradores.
- **Comissões ilustrativas.** Não há parceiro financiador; o repasse é apenas registrado (sem pagamento online, por decisão de produto).
- **Mapa interno da feira** (RF-34, Could) não foi feito; o mapa mostra as feiras.
- **E-mail:** sem SMTP configurado, as contas novas entram já verificadas.
- **Push no iPhone** só funciona com o app instalado na tela de início (limitação do iOS).
- Textos legais são rascunhos sem revisão jurídica; traduções EN/ES sem revisão por falantes nativos.
- Testado em Chromium (desktop e emulação de Android). WebKit/iPhone e aparelhos reais ainda não passaram pela matriz da seção 15.5.

## Documentação

- [Especificação](docs/ESPECIFICACAO.md) · [Arquitetura](docs/arquitetura.md) · [ADRs](docs/adr/README.md)
- [Rastreabilidade](docs/rastreabilidade.md) · [Riscos](docs/riscos.md) · [Medição](docs/medicao.md) · [Backlog](docs/backlog.md)
- [API (OpenAPI)](docs/api/openapi.json) · [Design](DESIGN.md) · [Marca](docs/brand.md) · [Referência visual](docs/design/referencia-visual.md)
- [Deploy](docs/runbooks/deploy.md) · [Restauração](docs/runbooks/restore.md) · [Incidentes](docs/runbooks/incidentes.md)
- [Privacidade](docs/legal/politica-de-privacidade.md) · [Termos](docs/legal/termos-de-uso.md) · [Termo do feirante](docs/legal/termo-consentimento-feirante.md)
- [Como contribuir](CONTRIBUTING.md) · [Changelog](CHANGELOG.md)

## Uso de inteligência artificial

Seguindo o item 8.1 do edital: o código, os testes e a documentação deste repositório foram produzidos com apoio do
**Claude Code (modelo Claude Opus 5.5, da Anthropic)**, a partir da especificação escrita pela equipe
(`docs/ESPECIFICACAO.md`). A equipe definiu o problema, o escopo, as regras de negócio e as decisões de produto,
revisou o resultado e responde por ele. Nenhuma imagem do app foi gerada por IA. Os commits feitos com apoio da IA
trazem a linha `Co-Authored-By` correspondente.

## Licença

Código sob a [licença MIT](LICENSE). A marca NANDESK (nome, logo e ícones em `frontend/public/brand/`) não está
coberta pela licença MIT; veja [docs/brand.md](docs/brand.md).
