# Runbook: incidentes

## Severidade

| Nível | Exemplo | Resposta |
|---|---|---|
| S1 | Site fora do ar, dados pessoais expostos, chave vazada | Imediata; avisar a equipe toda |
| S2 | Envios não chegam à moderação, login falhando para todos | Até 4 h |
| S3 | Erro numa tela, tradução faltando | Próximo dia útil |

## Primeiros passos

1. **Confirmar:** `curl -fsS https://nandesk.com.br/actuator/health` e abrir o site no celular.
2. **Olhar os logs** no Coolify (serviço `api`, logs em JSON com `requestId`). O `X-Request-Id` da resposta liga a
   requisição do usuário à linha do log.
3. **Recuperar primeiro, investigar depois:** voltar para o deploy anterior (`deploy.md`, seção 6) costuma ser o
   caminho mais rápido.

## Casos específicos

- **Segredo vazado (JWT, PIX, VAPID, senha do banco):** gerar novo valor, trocar no Coolify e fazer redeploy. Trocar
  as chaves JWT encerra todas as sessões (esperado). A chave PIX só pode ser trocada junto com uma migração de dados
  (a versão da chave fica gravada em cada registro).
- **Envio duplicado ou perdido:** a fila do app usa `Idempotency-Key`; procure no log pelo `taskId` e confira em
  `submissions` se há um único registro.
- **Abuso (spam de denúncias, cadastro em massa):** os limites por IP da tabela 12.3 respondem 429; suspender a
  conta pelo painel (Usuários) devolve as tarefas dela para a lista.
- **Dados pessoais expostos:** registrar o que vazou, para quem e desde quando; avaliar comunicação à ANPD e aos
  titulares (LGPD, art. 48) com orientação jurídica.

## Pós-incidente (sem culpados)

Registre em `docs/sprints/` com: linha do tempo, impacto, causa raiz, o que funcionou, o que não funcionou e ações
(com issue). Se a causa for uma regra de negócio, atualize a especificação (mudança de requisito).
