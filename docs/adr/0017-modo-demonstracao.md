# ADR-0017: Modo de demonstração: seed em produção, conta verificada sem SMTP e aviso de dados fictícios

- Situação: aceito
- Data: 2026-10-20
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
A banca do hackathon vai testar em nandesk.com.br. Ainda não temos dados reais coletados com feirantes nem servidor de e-mail contratado. A especificação (10.4) diz que o seed não roda em produção.

## Opções consideradas
1. Produção vazia — fiel à 10.4; contras: a banca não teria o que testar.
2. Seed de demonstração opcional em produção, com aviso na interface e contas de teste sem acesso à administração.

## Decisão
- `APP_SEED_ENABLED=true` carrega feiras, 26 produtos, 12 boxes fictícios, regras de comissão, uma verba do mês e tarefas abertas. Desvio consciente da 10.4, só para a demonstração.
- Senha das contas de teste em `SEED_DEFAULT_PASSWORD` e senha da administração separada em `SEED_ADMIN_PASSWORD` (≥ 12): o README divulga só colaborador e moderação.
- Faixa "Dados de demonstração: boxes e preços fictícios" (`WEB_DEMO_NOTICE=true`).
- Sem SMTP (`APP_MAIL_ENABLED=false`), as contas novas entram verificadas; com SMTP por variáveis, o fluxo normal de confirmação vale.

## Consequências
Desligar o seed e o aviso quando os dados reais entrarem (validação com feirantes, CA-5). Dados fictícios nunca devem ser apresentados como reais.

## Requisitos relacionados
seção 10.4 · RF-10 · CA-5
