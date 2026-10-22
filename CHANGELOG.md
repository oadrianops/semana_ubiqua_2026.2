# Changelog

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e versões [SemVer](https://semver.org/lang/pt-BR/).
IDs de requisitos conforme `docs/ESPECIFICACAO.md` (v1.3.0).

## [Não lançado]

### Pendente
- Validação no Ver-o-Peso e mais boxes reais; preço por lote ("3 por R$ 10,00").
- Mapa interno da feira com a posição dos boxes (RF-34, Could).
- Conferir por GPS a posição da Feira da 25.
- Matriz de aparelhos reais, Lighthouse CI e k6 contra a produção.

## [1.2.2] - 2026-10-22

### Corrigido
- Sem como sair da conta nem chegar ao perfil no computador: o topo ganha o menu da conta (perfil, histórico,
  ganhos e "Sair"), no celular e na tela larga.
- Envio "sumia" depois de mandado: ele fica na lista "Em andamento" como "Em revisão" até a moderação decidir, a tela
  de envio leva ao histórico e o histórico explica os resultados (em revisão, aprovado, recusado com o motivo).

## [1.2.1] - 2026-10-22

### Corrigido
- Tela branca ao abrir um box para quem já tinha usado o app: a versão nova lia a galeria de um box guardado no
  aparelho pela versão anterior, que não tinha esse campo. A página do box aceita os dados antigos, o cache de
  consultas guardado no aparelho é descartado a cada versão publicada e cada tela ganhou uma proteção de erro que
  mostra "Tentar de novo" em vez de derrubar o app.

## [1.2.0] - 2026-10-22 — nove feiras e posição dos boxes

### Adicionado
- Sete feiras de Belém na demonstração (Cremação, Pedreira, Bandeira Branca, Icoaraci, Benguí, Mercado de São Brás e
  Praça da República), com endereço, posição e horário pesquisados e 21 boxes de exemplo (ADR-0019).
- Capas do Ver-o-Peso e do Mercado de São Brás do Wikimedia Commons, com o crédito do autor e da licença abaixo da
  foto; o painel aceita o crédito ao enviar uma capa (`markets.cover_credit`, V6).
- Posição do box no painel e botão "Como chegar ao box" (boxes 67 e 2 e 3 da Feira da 25).
- Categorias "Carnes e frangos" e "Plantas e flores"; ícones das categorias novas.

## [1.1.0] - 2026-10-21 — primeiros boxes reais da Feira da 25

### Adicionado
- **Validação em campo (CA-5):** dois feirantes da Feira da 25 entrevistados, com termo assinado; atas em
  `docs/validacao/`.
- **Box conferido em campo pelo painel** (RF-29, RN-13): cadastro a partir do termo, ofertas com preço de referência,
  fotos principal e de galeria, Instagram do box e foto de capa da feira. Revogar o consentimento agenda a remoção de
  todas as fotos do box.
- **Página do box** com galeria de fotos, Instagram (pedido de um feirante na validação) e marca "Exemplo" nos boxes
  fictícios, também na busca, no produto, na feira e sem internet (ADR-0018).
- `infra/scripts/importar-campo.mjs`: lança um levantamento inteiro pela API; nomes e fotos ficam fora do repositório.
- Catálogo: 18 produtos e 3 categorias vistos na Feira da 25 (hortifrúti, roupas e armarinho).

### Corrigido
- Mapa sem tiles e "Como chegar" abrindo um destino inválido em produção: o `config.js` do contêiner cortava as URLs
  com `{z}` e `{lat}`. Teste roda o script de verdade.
- Feira da 25 na Av. Rômulo Maiorana (antiga 25 de Setembro), entre a Tv. Antônio Baena e a Av. José Bonifácio; antes
  a posição ficava a cerca de 550 m da avenida.

## [1.0.0] - 2026-10-21 — entrega do Hackathon Semana Ubíqua 2026.2

### Adicionado
- **Vitrine pública** (RF-01 a RF-07): feiras com mapa e lista, busca com sugestões, sinônimos em PT/EN/ES e
  "você quis dizer", páginas de box, produto e categoria, preço de referência com a data da verificação em cada
  linha, aviso de dado antigo (RN-12) e "Informação errada?" com campo-armadilha e limite por IP (RN-16).
- **PWA e uso sem conexão** (RF-35, RF-36): manifesto e ícones da marca, service worker com precache, app shell no
  `index.html`, snapshot por feira com ETag, "Salvar para usar sem internet", busca local com a mesma normalização
  do servidor, data dos dados salvos e aviso após 7 dias (RN-28).
- **Conta** (RF-08 a RF-12): cadastro com termos e declaração de maioridade, confirmação de e-mail, login com
  bloqueio temporário, refresh rotativo em cookie, redefinição de senha, perfil, chave PIX cifrada e mascarada,
  feiras de interesse, exportação dos dados e exclusão com anonimização (RN-20).
- **Colaboração** (RF-13 a RF-20, RF-23, RF-26): tarefas por feira e tipo ordenáveis pela distância, aceite com
  reserva única (RN-01), check-in pela cerca da feira (RN-05), prazos como barra proporcional (RN-02), fotos
  comprimidas no aparelho e enviadas uma a uma com `Idempotency-Key` (RN-27), fila de envios que segue sozinha
  quando a rede volta, rascunho salvo, histórico com motivo da recusa, extrato e reputação (RN-17).
- **Moderação e administração** (RF-21, RF-22, RF-24 a RF-33): fila com antes/depois, conflitos de versão
  (RN-26), fotos restritas, segregação de funções (RN-11), denúncias com verificação automática, sugestões de
  produto, tarefas e orçamento com modo voluntário (RN-10), feiras com mapa e raio, boxes e consentimento
  (RN-13), catálogo nos 3 idiomas, regras de comissão, saldos e repasses feitos fora da plataforma (sem pagamento
  online), usuários, auditoria e indicadores.
- **Notificações** (RF-14, RF-19): sino com contador, SSE com ticket de uso único e Web Push próprio (RFC 8291,
  VAPID) restrito aos serviços de push dos navegadores.
- **Infraestrutura**: imagens Docker não-root, Nginx com CSP e cabeçalhos da seção 12.5, Compose de
  desenvolvimento e de produção para o Coolify (`nandesk.com.br`), scripts de chaves, backup e restauração,
  Prometheus/Grafana, k6, workflows de CI (backend, frontend, E2E, segurança, release) e Dependabot.
- **Qualidade**: 128 testes unitários, 71 testes de integração com MySQL 8.4 (CA-1 com os 26 produtos, corrida da
  RN-01, matriz de segurança, contrato OpenAPI), 87 testes do frontend, jornadas E2E J1 a J8 com axe e perfil de
  rede "Feira lotada", ArchUnit e orçamentos de performance no CI.
- **Documentação**: 17 ADRs, arquitetura, riscos, medição, rastreabilidade gerada, runbooks de deploy, restauração e
  incidentes, rascunhos legais e material de validação.

### Corrigido durante a verificação com banco e navegador reais
- Português resolvia como idioma não suportado e mostrava chaves cruas.
- Duas coleções `List` buscadas juntas (Hibernate) e sinônimos que só diferiam por acento quebravam o seed no MySQL.
- `/actuator/health` exigia login, o que impediria o healthcheck do Docker de ficar saudável.
- O job de retenção falharia na chave estrangeira das fotos de envio a partir do 90º dia (RN-22).
- Inscrição de push aceitava qualquer URL (risco de SSRF).
- Nome acessível sem espaço antes do número do box e campo de foto sem rótulo.
- Revisão de design (impeccable): cabeçalho transbordando a 390 px, filtros cobrindo resultados, linha de preço
  quebrada no celular, marca "atualizado" ausente, títulos de aba iguais, textos do servidor sem tradução, item ativo
  do painel invisível e desistência sem confirmação (`docs/design/revisao-2026-10-21.md`).
- Primeiro CI no GitHub (PR #10): a imagem web não compilava no Docker (teste importando `contracts/`), o stack
  local não liberava a origem `http://localhost:8081` no login, as tabelas que rolam para o lado no celular não
  recebiam foco pelo teclado e o Trivy apontava para uma versão retirada.

[Não lançado]: https://github.com/oadrianops/semana_ubiqua_2026.2/compare/v1.2.2...HEAD
[1.2.2]: https://github.com/oadrianops/semana_ubiqua_2026.2/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/oadrianops/semana_ubiqua_2026.2/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/oadrianops/semana_ubiqua_2026.2/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/oadrianops/semana_ubiqua_2026.2/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/oadrianops/semana_ubiqua_2026.2/releases/tag/v1.0.0
