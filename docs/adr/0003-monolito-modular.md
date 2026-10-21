# ADR-0003: Monólito modular por funcionalidade, com regras ArchUnit

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Uma equipe de três pessoas, um VPS e prazo de hackathon. Mesmo assim, o domínio tem fronteiras claras (vitrine, tarefas, envios, comissões, LGPD) e precisa evoluir sem virar um emaranhado.

## Opções consideradas
1. Microsserviços — isolamento forte; contras: operação, rede e consistência distribuída desproporcionais ao tamanho.
2. Monólito em camadas técnicas (controllers/, services/) — simples; contras: as regras de negócio se espalham.
3. Monólito modular por funcionalidade com regras verificadas — um deploy, fronteiras explícitas.

## Decisão
Pacote `br.com.nandesk` com 16 módulos: `shared`, `user`, `file`, `market`, `catalog`, `stall`, `search`, `auth`, `task`, `submission`, `commission`, `report`, `notification`, `audit`, `admin` e `lgpd`, cada um com `api/application/domain/infra`.

O `ArchitectureTest` (ArchUnit) garante: sem ciclos entre módulos, `api` não usa `infra`, `domain` não depende de Spring Web, nada de `@Transactional` em controller, injeção só por construtor, nenhum `Instant.now()` direto e entidades só em `domain`.

Os ciclos naturais foram quebrados assim: referências por id (`MarketSubscription.marketId`, `LedgerEntry.taskId`); eventos depois do commit (`StallHidden`, `TaskPublished`, `SubmissionReviewed`, `PayoutRegistered`…); composição da vitrine no módulo `search`; e a segregação de funções (RN-11) em `shared.security.SegregationOfDuties`. A retenção (LGPD) fica em `lgpd`, que conversa com `file`, `submission` e `stall` para desfazer vínculos antes de apagar.

## Consequências
Um único JAR e um único banco, com fronteiras testadas a cada build. Extrair um módulo no futuro (ex.: notificações) fica viável. Custo: ArchUnit e eventos exigem disciplina.

## Requisitos relacionados
RNF-11 · seção 9.3
