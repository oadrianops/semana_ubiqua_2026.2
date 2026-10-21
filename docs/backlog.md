# Backlog e situação do escopo

Situação em 2026-10-21 (v1.0.0). Detalhe por requisito em `docs/rastreabilidade.md` (gerado do código e dos testes).

## Entregue

- RF-01 a RF-33, RF-35 e RF-36: vitrine, busca, PWA e uso sem conexão, conta, colaboração, moderação, comissões e repasses
  registrados, denúncias, notificações, painel e LGPD.
- CA-1 (26 produtos encontrados nos 3 idiomas), CA-2 (tarefa em 3 passos), CA-3 (3 idiomas sem chave crua),
  CA-6 (sem rede e com rede caindo) e CA-7 (Feira lotada) com testes automatizados.

## Pendente (não feito ainda, sem simulação)

| Item | Por quê | Próximo passo |
|---|---|---|
| RF-34: mapa interno da feira (Could) | Exige levantamento da posição dos boxes | Planta de cada feira no painel |
| CA-5: validação com feirantes | Depende de ida às feiras | `docs/validacao/roteiro-feirantes.md` |
| Fotos reais das feiras e dos boxes | Sem fotos próprias; banco de imagens e IA descartados | Chegam pelo fluxo dos colaboradores, com consentimento |
| Posição exata da Feira da 25 | Coordenada aproximada | Conferir no painel → Feiras |
| Revisão jurídica dos textos legais | Rascunhos | Revisão antes de lançamento público |
| Revisão das traduções por falantes | Traduções feitas pela equipe | Pedir revisão EN/ES |
| Matriz de aparelhos reais (15.5) | Sem os aparelhos durante o hackathon | Android de entrada, intermediário e iPhone |
| Lighthouse CI e k6 contra a produção | Precisa do ambiente no ar | `perf/search.js`; `npx lhci autorun` |
| SonarQube Cloud e alertas do Grafana | Should/Could | Configurar token e regras |

## Fora do escopo (Won't, por decisão de produto)

Pagamento online, carteira ou gateway (P2); delivery ou reserva; outras cidades; fidelidade; gestão de vendas para o
feirante; apps nativos; download de mapas para uso offline.
