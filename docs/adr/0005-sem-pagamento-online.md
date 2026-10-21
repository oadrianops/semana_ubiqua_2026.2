# ADR-0005: Sem pagamento online: comissões em extrato e repasses feitos fora da plataforma

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Colaboradores ganham comissão por envio aprovado. Integrar um meio de pagamento traria custo, KYC, risco regulatório e prazo que um hackathon não comporta. A decisão de produto P2 proíbe pagamento online, carteira ou gateway.

## Opções consideradas
1. Gateway de pagamento / PIX automático — repasse imediato; contras: proibido pela P2, custo e risco.
2. Extrato imutável + repasse registrado pela administração depois de pago no banco.

## Decisão
O `LedgerEntry` é um extrato só de inserção (crédito, repasse, estorno, ajuste), com saldo calculado e mínimo para repasse (RN-09). A administração paga por PIX fora do app e registra o repasse com a referência. A chave PIX fica cifrada (AES-GCM, RN-22) e só aparece mascarada; ver a chave completa gera registro na auditoria.

## Consequências
Nenhum dinheiro passa pela plataforma. A conciliação é manual e auditável. Quem registra o repasse não pode ser a pessoa que o recebe (RN-11).

## Requisitos relacionados
RF-25 a RF-28 · RN-07 a RN-11
