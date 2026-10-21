# Registro de decisões de arquitetura (ADRs)

Formato MADR resumido (Anexo D da especificação). Situação "aceito" salvo indicação.

| ADR | Decisão | Situação |
|---|---|---|
| [ADR-0001](0001-monorepo.md) | Monorepo com backend, frontend, infra e docs | aceito |
| [ADR-0002](0002-versoes-da-stack.md) | Versões da stack | aceito |
| [ADR-0003](0003-monolito-modular.md) | Monólito modular por funcionalidade, com regras ArchUnit | aceito |
| [ADR-0004](0004-jwt-e-refresh.md) | Autenticação stateless com JWT próprio (RS256) e refresh rotativo em cookie | aceito |
| [ADR-0005](0005-sem-pagamento-online.md) | Sem pagamento online: comissões em extrato e repasses feitos fora da plataforma | aceito |
| [ADR-0006](0006-openstreetmap-leaflet.md) | OpenStreetMap + Leaflet, com URL de tiles configurável | aceito |
| [ADR-0007](0007-busca-normalizada-mysql.md) | Busca com termos normalizados no MySQL (sem motor externo na v1) | aceito |
| [ADR-0008](0008-geofence-por-feira.md) | Geofence no nível da feira, não do box | aceito |
| [ADR-0009](0009-arquivos-em-volume.md) | Fotos em volume local, atrás da interface FileStorage | aceito |
| [ADR-0010](0010-notificacoes.md) | Notificações: in-app, SSE e Web Push próprio (RFC 8291, VAPID) | aceito |
| [ADR-0011](0011-pwa.md) | PWA em vez de apps nativos | aceito |
| [ADR-0012](0012-changeset-moderado.md) | Changeset JSON moderado em vez de edição direta dos dados públicos | aceito |
| [ADR-0013](0013-offline-first.md) | Offline-first: snapshot por feira com ETag e cache persistido no aparelho | aceito |
| [ADR-0014](0014-upload-uma-a-uma.md) | Upload de fotos uma a uma, idempotente, antes do envio | aceito |
| [ADR-0015](0015-orcamentos-de-performance.md) | Orçamentos de performance e perfil "Feira lotada" como critério de aceite no CI | aceito |
| [ADR-0016](0016-direcao-de-design.md) | Direção de design "Lousa de preços" | aceito |
| [ADR-0017](0017-modo-demonstracao.md) | Modo de demonstração: seed em produção, conta verificada sem SMTP e aviso de dados fictícios | aceito |
