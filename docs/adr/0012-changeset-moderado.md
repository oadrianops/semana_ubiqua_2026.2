# ADR-0012: Changeset JSON moderado em vez de edição direta dos dados públicos

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Dados vêm de colaboradores remunerados. Um erro ou fraude publicado direto prejudica o feirante e a confiança no app.

## Opções consideradas
1. Edição direta com reversão — rápido; contras: dado errado no ar até alguém notar.
2. Envio como changeset (Anexo A) que a moderação aprova ou recusa, com detecção de conflito por versão.

## Decisão
O colaborador envia um changeset com `baseVersion` por oferta e pelo box. A moderação vê antes e depois lado a lado, e conflitos (o dado mudou depois do envio) exigem confirmação explícita (RN-26). Aprovar aplica tudo numa transação, credita a comissão e registra na auditoria; recusar exige motivo e pode reabrir a tarefa.

## Consequências
Os dados públicos são sempre revisados. Custo: a fila de moderação precisa de gente; o painel mostra o tempo médio de revisão.

## Requisitos relacionados
RF-18 · RF-21 · RF-22 · RN-26 · Anexo A
