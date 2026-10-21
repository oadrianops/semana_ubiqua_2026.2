# Changelog

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e versões [SemVer](https://semver.org/lang/pt-BR/).
IDs de requisitos conforme `docs/ESPECIFICACAO.md` (v1.1.0).

## [Não lançado]

### Pendente
- Validação com feirantes (CA-5): roteiro e ata modelo prontos em `docs/validacao/`.
- Mapa interno da feira com a posição dos boxes (RF-34, Could).
- Fotos reais das feiras, enviadas pelo fluxo dos colaboradores com consentimento.
- Conferir a posição da Feira da 25 no painel (hoje aproximada).
- Matriz de aparelhos reais, Lighthouse CI e k6 contra a produção.

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

[Não lançado]: https://github.com/oadrianops/semana_ubiqua_2026.2/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/oadrianops/semana_ubiqua_2026.2/releases/tag/v1.0.0
