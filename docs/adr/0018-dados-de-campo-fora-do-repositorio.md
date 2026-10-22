# ADR-0018: Dados de campo fora do repositório e boxes reais ao lado dos de exemplo

- Situação: aceito
- Data: 2026-10-21
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
Em 21/10/2026 a equipe foi à Feira da 25 e voltou com dois termos assinados (box 67, roupas e armarinho; boxes 2 e 3,
hortifrúti), fotos dos boxes e depoimentos. O termo promete que, se o feirante mudar de ideia, "o box sai do
aplicativo na hora e as fotos são apagadas em até 30 dias". Até aqui todos os boxes eram fictícios (ADR-0017) e o
único jeito de um box entrar era uma tarefa `NEW_STALL`, que exige check-in na feira.

## Opções consideradas
1. Colocar nomes e fotos no seed (`dev-seed.json`). Prós: aparece sozinho em qualquer ambiente. Contras: o
   repositório é público; o histórico do Git guarda as fotos para sempre e a promessa de apagar deixa de valer.
2. Cadastrar pelo painel, com as fotos no volume de produção, e manter no repositório só código e catálogo. Prós:
   revogar o consentimento apaga de verdade (RN-13); segue a regra de não versionar `file/`. Contras: precisa de
   telas e de um script para o lote do levantamento.

## Decisão
Opção 2.
- O painel ganha o cadastro do box conferido em campo (número, setor, nome do feirante com o consentimento, forma
  do consentimento e Instagram), as ofertas com preço de referência e as fotos: principal, galeria e capa da feira.
  Fotos do painel já nascem públicas; tirar uma foto ou revogar o consentimento agenda a remoção do arquivo.
- `infra/scripts/importar-campo.mjs` lança um levantamento inteiro pela API a partir de um manifesto e das fotos
  em `file/campo/` (fora do Git). Pode rodar de novo sem duplicar nada.
- Fotos com pessoas: só entram recortadas ou desfocadas (RN-22). Fotos de bancas de terceiros sem termo ficam de fora.
- `stalls.demo` marca os boxes fictícios do seed. A interface mostra "Exemplo" neles e a faixa de demonstração passa
  a dizer que só os boxes marcados são fictícios. Na lista da feira os boxes conferidos vêm primeiro.
- O catálogo (produtos e categorias vistos na feira) e o endereço da Feira da 25 não são dados pessoais: entram no
  seed, que também atualiza bancos já carregados com setores novos e a marca de exemplo.

## Consequências
Produção passa a misturar boxes reais e de exemplo, sempre identificados. Quando houver boxes reais suficientes,
desligar o seed (`APP_SEED_ENABLED=false`) e os boxes de exemplo saem da vitrine pelo painel. O termo assinado em
papel continua com a equipe; o app registra só a forma, a data e quem cadastrou (RN-13).

## Requisitos relacionados
RF-03 · RF-29 · RN-13 · RN-14 · RN-22 · CA-5 · ADR-0017
