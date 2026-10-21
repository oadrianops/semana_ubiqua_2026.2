# ADR-0009: Fotos em volume local, atrás da interface FileStorage

- Situação: aceito
- Data: 2026-10-19
- Decisores: Adriano Pinheiro da Silva, Leonardo Farias de Souza, Victor Santiago dos Santos Abreu

## Contexto
O volume de fotos é pequeno (~300 KB cada após compressão) e o deploy é num único VPS.

## Opções consideradas
1. S3/compatível — escalável; contras: custo e credenciais.
2. Volume Docker local atrás de uma interface — simples; contras: backup próprio.

## Decisão
Interface `FileStorage` com a implementação local em `/data/uploads` (volume `uploads`). Toda foto passa pelo `ImagePipeline` (valida tipo real e tamanho mínimo, remove EXIF/GPS, redimensiona para 1600 px e gera miniatura). Fotos de envio são restritas a quem enviou e à moderação; fotos de box viram públicas quando aprovadas.

## Consequências
Backup diário do volume em `infra/scripts/backup.sh`. Trocar por S3 depois é uma nova implementação da interface.

## Requisitos relacionados
RF-18 · RN-06 · RN-13 · RN-22
