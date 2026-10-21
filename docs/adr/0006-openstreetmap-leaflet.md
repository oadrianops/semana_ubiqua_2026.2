# ADR-0006: OpenStreetMap + Leaflet, com URL de tiles configurável

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O app mostra as feiras no mapa e o raio de check-in. Não queremos chave de API paga nem rastreamento de terceiros, e o mapa não pode ser essencial (sempre com alternativa em lista).

## Opções consideradas
1. Google Maps — conhecido; contras: chave, custo, termos e rastreamento.
2. OpenStreetMap + Leaflet — livre, leve; contras: política de uso dos tiles públicos.

## Decisão
Leaflet 1.9 carregado sob demanda, tiles OpenStreetMap pela URL de `WEB_MAP_TILE_URL` (tempo de execução), atribuição sempre visível com link para os direitos, e nenhum tile em cache no service worker (política do OSM). A CSP libera só o host dos tiles.

## Consequências
Para tráfego maior, trocar a URL por um provedor de tiles (ex.: MapTiler) sem novo build, ajustando o `img-src` da CSP. Situação de produção: "proposto" até medir o tráfego real.

## Requisitos relacionados
RF-01 · RNF-07 · seção 9.6
