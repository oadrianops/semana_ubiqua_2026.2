# ADR-0008: Geofence no nível da feira, não do box

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O check-in comprova presença física. O GPS de celular dentro de mercados cobertos erra dezenas de metros, e os boxes ficam a poucos metros uns dos outros.

## Opções consideradas
1. Cerca por box — mais preciso no papel; contras: falso negativo constante e cadastro de coordenadas de cada box.
2. Cerca por feira (centro + raio) com tolerância à precisão informada.

## Decisão
Cada feira tem centro e raio (padrão 250 m, ajustável no painel, 50 a 2000 m). O check-in aceita se a distância menos a precisão (limitada a 100 m) couber no raio; precisão ruim passa com o alerta `LOW_ACCURACY` para a moderação, que confere pelas fotos. A posição não fica guardada no aparelho e as coordenadas do check-in são apagadas após 90 dias (RN-21).

## Consequências
Menos recusas injustas; a confirmação do box fica com as fotos e com a moderação.

## Requisitos relacionados
RF-17 · RN-05 · RN-21
