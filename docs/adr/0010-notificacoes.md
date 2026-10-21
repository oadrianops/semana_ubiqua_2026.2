# ADR-0010: Notificações: in-app, SSE e Web Push próprio (RFC 8291, VAPID)

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Colaboradores precisam saber de tarefas novas nas suas feiras e de prazos. Bibliotecas de Web Push para Java estão paradas ou puxam BouncyCastle, e serviços como Firebase exigem conta e SDK.

## Opções consideradas
1. Biblioteca `web-push` (Java) — pronta; contras: manutenção irregular e dependências pesadas.
2. Firebase Cloud Messaging com SDK — confiável; contras: conta Google, SDK no app e rastreamento.
3. Implementação própria com a criptografia do JDK (ECDH P-256, HKDF, AES-128-GCM) e VAPID ES256.

## Decisão
Notificação gravada no banco (sino com contador), entregue em tempo real por SSE com ticket de uso único, e por Web Push cifrado (`aes128gcm`) para os serviços dos navegadores. Antispam por feira, horário de silêncio 22h–6h e inscrições vencidas (404/410) removidas. Só endereços HTTPS dos serviços de push conhecidos são aceitos, para evitar SSRF (`PushEndpointPolicy`). Sem chaves VAPID, o push fica desligado e o resto funciona.

## Consequências
Nenhuma conta de terceiros. O código de criptografia tem testes com vetores da RFC 8291 e um IT com servidor de push falso.

## Requisitos relacionados
RF-14 · RF-19 · seção 9.8
