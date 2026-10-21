# ADR-0004: Autenticação stateless com JWT próprio (RS256) e refresh rotativo em cookie

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O PWA precisa funcionar com rede ruim, sem depender de serviço externo de identidade, e o token não pode ficar em `localStorage` (8.6).

## Opções consideradas
1. Sessão em servidor (cookie de sessão) — simples; contras: estado no servidor e CSRF em todas as rotas.
2. Provedor externo (Keycloak, Auth0) — completo; contras: mais um serviço para operar ou custo, e login social fora do escopo.
3. JWT de acesso curto em memória + refresh token rotativo em cookie HttpOnly, SameSite=Strict, Path=/api/v1/auth.

## Decisão
JWT RS256 de 15 minutos, assinado pela própria API (Spring Security Resource Server), guardado só em memória no app. Refresh token opaco, rotativo e com validade absoluta, em cookie `HttpOnly`, `Secure` em produção e `SameSite=Strict`; as rotas que usam o cookie também conferem o `Origin`. O app faz uma única renovação simultânea em caso de 401. As chaves vêm por arquivo ou por variável PEM (prático no Coolify); sem chaves, a API gera um par efêmero e avisa no log.

## Consequências
Sem estado de sessão no servidor e sem CSRF nas rotas de dados. Reuso de refresh token revoga a família inteira. Trocar as chaves derruba as sessões (aceitável). Bloqueio temporário após 5 senhas erradas (seção 12.1) e limite de 5 logins por minuto por IP.

## Requisitos relacionados
RF-08 a RF-10 · seção 12.1
