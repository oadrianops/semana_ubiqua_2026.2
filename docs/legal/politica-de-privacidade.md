# Política de Privacidade da NANDESK (rascunho)

> Rascunho do projeto acadêmico, sem revisão jurídica. Antes de um lançamento público, revise com alguém da área
> (seção 13 da especificação). Versão de referência: 2026-10.

## Quem somos

A NANDESK mostra em qual feira e em qual box de Belém (Ver-o-Peso e Feira da 25) encontrar cada produto. É mantida por
estudantes da UNAMA (Adriano Pinheiro da Silva, Leonardo Farias de Souza e Victor Santiago dos Santos Abreu).
Contato: **contato@nandesk.com.br**.

## O que coletamos e por quê

| Dado | De quem | Para quê | Base legal (LGPD, art. 7º) | Por quanto tempo |
|---|---|---|---|---|
| Nome, e-mail e telefone (opcional) | Colaboradores | Conta, comunicação e tarefas | Execução de contrato (V) | Enquanto a conta existir |
| Senha (guardada como hash) | Colaboradores | Entrar na conta | Execução de contrato (V) | Enquanto a conta existir |
| Chave PIX (cifrada, exibida mascarada) | Colaboradores | Repassar as comissões | Execução de contrato (V) | Enquanto houver relação e o necessário por lei |
| Localização no check-in | Colaboradores | Comprovar que a pessoa estava na feira | Legítimo interesse (IX) | 90 dias |
| Fotos dos envios | Colaboradores e o que aparece nelas | Conferir e publicar o box | Execução de contrato (V); consentimento do feirante (I) | Evidências: 90 dias; recusadas: 30 dias |
| Nome do feirante, nome e foto do box | Feirantes | Mostrar o box no app | Consentimento (I) | Até a revogação (+30 dias para apagar fotos) |
| Termos buscados | Ninguém (sem IP nem usuário) | Saber o que falta no catálogo | Não é dado pessoal | 180 dias |

Quem só consulta a vitrine não precisa de conta e não é identificado. **Não usamos rastreadores nem analytics de
terceiros.** As fotos têm os metadados (EXIF e GPS) apagados ao chegar.

## Com quem compartilhamos

Com ninguém para fins comerciais. O mapa usa tiles do OpenStreetMap, carregados pelo seu navegador. As notificações
push passam pelo serviço de push do seu navegador (Google, Mozilla, Microsoft ou Apple), sempre cifradas.

## Seus direitos (LGPD, art. 18)

No **Perfil** do app você pode corrigir seus dados, **baixar uma cópia** (JSON) e **pedir a exclusão** da conta. Na
exclusão, os dados pessoais são anonimizados; registros financeiros ficam guardados pelo prazo legal, sem o seu nome.
Para outros pedidos, escreva para contato@nandesk.com.br.

## Feirantes

O box só aparece com o consentimento do feirante, registrado com data e forma (verbal ou termo assinado). O nome do
feirante só aparece com um consentimento próprio. Para tirar o box do ar, fale com quem fez o cadastro ou escreva
para contato@nandesk.com.br: o box sai na hora e as fotos são apagadas em até 30 dias.

## Segurança

HTTPS em todas as páginas, senhas com hash BCrypt, chave PIX cifrada (AES-256-GCM), acesso por papel, registros de
auditoria para ações administrativas e cópias de segurança diárias.

## Mudanças

Mudanças nesta política aparecem no app com a data da nova versão.
