# Entrega de workouts ao Garmin

## Fronteira e pré-condição

O backend orquestra e persiste a entrega; o adaptador Python compila o contrato
neutro `workout.v1` para o payload Garmin. Nenhuma rota usa navegador. Somente um
plano semanal em `APPROVED` pode ser pré-visualizado e enviado pela primeira vez.
Confirmação, atualização e cancelamento aceitam `APPROVED` ou `DELIVERED` porque
gerenciam uma entrega já criada.

As credenciais Garmin são lidas da entidade do atleta, descriptografadas apenas no
backend e enviadas ao adaptador. Elas não aparecem em respostas, hashes ou erros.
Em instalações que não sejam estritamente locais, defina a mesma
`GARMIN_ADAPTER_API_KEY` nos dois processos e proteja o tráfego com TLS ou uma rede
privada.

## Endpoints

Todos os caminhos começam com
`/api/athletes/{athleteId}/weekly-plans/{weeklyPlanId}/garmin`.

| Método | Sufixo | Efeito |
| --- | --- | --- |
| `GET` | `/preview` | Compila todas as sessões sem autenticar no Garmin ou persistir entrega |
| `POST` | `/deliveries` | Cria/reutiliza, envia e agenda todas as sessões |
| `POST` | `/confirmations` | Consulta o mês no calendário e confirma todas as sessões |
| `PUT` | `/deliveries/{deliveryId}` | Substitui o workout e refaz o agendamento |
| `DELETE` | `/deliveries/{deliveryId}` | Remove o agendamento e o template externo |

O preview retorna por sessão o `contentHash` SHA-256 e o payload exato que o
adaptador enviaria. O envio retorna IDs externos e estados persistidos.

## Compilação

- `WARMUP`, `WORK`, `RECOVERY` e `COOLDOWN` viram os tipos correspondentes do Garmin;
- `TIME` usa segundos e `DISTANCE` usa metros;
- limites de pace em segundos/km viram limites de velocidade em m/s;
- bloco com uma execução é achatado; bloco repetido vira `RepeatGroupDTO`;
- a chave idempotente aparece no nome e na descrição externos para descoberta
  depois de timeout ou resposta perdida.

## Estados e idempotência

A migration V12 cria `garmin_workout_deliveries`. A unicidade é dada por atleta,
atividade planejada, versão do plano e hash de conteúdo; a chave externa é o
SHA-256 dessa composição.

```text
PENDING -> DELIVERING -> SCHEDULED -> CONFIRMED
                 |
                 +-> FAILED
SCHEDULED/CONFIRMED -> CANCELLING -> CANCELLED
```

Repetir o envio de uma entrega `SCHEDULED` ou `CONFIRMED` não cria outro workout.
O adaptador também procura a chave entre os workouts antes do upload. Um plano só
passa a `DELIVERED` quando todas as sessões forem encontradas no calendário.
Cancelar qualquer sessão devolve o plano a `APPROVED`.

Falhas externas deixam estado retomável, incrementam `attemptCount` e persistem
somente mensagem sanitizada. Backend e adaptador fazem tentativas limitadas para
falhas de transporte. Configure `GARMIN_DELIVERY_MAX_ATTEMPTS`,
`GARMIN_DELIVERY_RETRY_DELAY_MS`, `GARMIN_DELIVERY_CONNECT_TIMEOUT_MS` e
`GARMIN_DELIVERY_READ_TIMEOUT_MS`.

## Fora do escopo

Esta etapa não importa atividades com eficiência, não compara planejado versus
executado, não adapta a próxima semana e não analisa telemetria pós-treino.
