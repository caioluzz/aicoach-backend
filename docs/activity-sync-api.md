# Pipeline de sincronização de atividades

O backend executa uma sincronização no evento de inicialização da aplicação e,
por padrão, repete o job a cada duas horas. O processo usa duas chamadas distintas ao
adaptador Garmin:

1. descobre até 100 atividades por metadados, sem baixar FIT;
2. consulta o identificador no banco e baixa/processa somente atividades ausentes.

O índice único de `activities.garmin_activity_id` permanece como proteção final de
idempotência. Execuções concorrentes do mesmo atleta são ignoradas no processo atual.

## Operação manual e observação

| Método | Caminho | Resultado |
| --- | --- | --- |
| `POST` | `/api/v1/activities/sync` | Sincroniza todos os atletas agora |
| `POST` | `/api/v1/activities/sync/{athleteId}` | Sincroniza um atleta agora |
| `GET` | `/api/v1/activities/sync/status` | Estado mais recente de todos os atletas |
| `GET` | `/api/v1/activities/sync/status/{athleteId}` | Estado mais recente de um atleta |
| `POST` | `/api/v1/activities/athletes/{athleteId}/import` | Importa diretamente uma atividade pelo `garminActivityId` |

Cada resposta informa `status`, checkpoint, horários de tentativa/sucesso,
quantidades descoberta/importada/ignorada e o último erro sanitizado. Os estados são
`NEVER_RUN`, `RUNNING`, `SUCCESS` e `FAILED`. Logs estruturados pelo marcador
`activity_sync` registram fase, tentativa, contagens e tipo do erro, sem credenciais.

## Checkpoint e recuperação

O checkpoint é o maior `started_at` observado em uma execução completamente bem-
sucedida. A consulta seguinte retrocede 24 horas por padrão. Essa sobreposição recupera
atividades que chegaram atrasadas; IDs já persistidos são descartados antes do FIT.
Se qualquer FIT falhar depois dos retries, o checkpoint não avança.

## Configuração

As variáveis `GARMIN_SYNC_FIXED_DELAY_MS` e `GARMIN_SYNC_INITIAL_DELAY_MS` valem
`7200000` por padrão. Também podem ser configurados `GARMIN_SYNC_DISCOVERY_LIMIT`,
`GARMIN_SYNC_RECOVERY_WINDOW_HOURS`, `GARMIN_SYNC_MAX_ATTEMPTS`,
`GARMIN_SYNC_RETRY_DELAY_MS`, `GARMIN_SYNC_CONNECT_TIMEOUT_MS` e
`GARMIN_SYNC_READ_TIMEOUT_MS`.

Os testes usam mocks/fakes e nunca chamam a Garmin real.

## Importação direta por link

O frontend extrai o identificador numérico de uma URL `connect.garmin.com/.../activity/{id}`
e envia `{ "garminActivityId": 123 }` ao endpoint de importação. O backend solicita
somente essa atividade ao adaptador, valida que o ID devolvido é o solicitado e preserva
a idempotência pelo índice único. Essa rota não depende da atividade estar entre as
100 mais recentes.
