# AI RunCoach Backend

API Spring Boot que persiste atletas e atividades, agenda sincronizações e consome o
microsserviço Garmin. Este repositório é independente do repositório
`aicoach-microservice`; a pasta que contém ambos não é um monorepo.

## Requisitos

- JDK 17
- Docker com Compose (recomendado) ou MySQL 8 existente
- microsserviço Garmin em `http://localhost:8000` para sincronizações reais
- chave da OpenAI para gerar propostas de plano geral e semanal

## Execução local

Defina os segredos apenas no ambiente. O arquivo `.env.example` lista os nomes,
mas o Spring Boot não carrega `.env` automaticamente.

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
$env:DB_PASSWORD = 'senha-local-do-mysql'
$env:GARMIN_ENCRYPTION_KEY = [Convert]::ToBase64String(
  [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
$env:OPENAI_API_KEY = 'chave-local-da-openai'
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Guarde a chave de criptografia local em um gerenciador de segredos: trocá-la torna
as senhas Garmin já persistidas impossíveis de descriptografar. A chave e a senha
do banco que existiam no histórico Git devem ser consideradas comprometidas e
rotacionadas; esta etapa remove os valores do estado atual, sem reescrever histórico.

## Endpoints atuais

| Método | Caminho | Estado |
| --- | --- | --- |
| `POST` | `/api/athletes/new` | Cadastra atleta, credenciais Garmin e objetivos |
| `GET` | `/api/athletes` | Lista entidades de atleta; não retorna a senha Garmin |
| `POST` | `/api/athletes/{id}/assessments` | Cria uma versão estruturada da anamnese |
| `GET` | `/api/athletes/{id}/assessments/latest` | Consulta a anamnese mais recente |
| `GET` | `/api/athletes/{id}/assessments` | Consulta o histórico versionado da anamnese |
| `POST` | `/api/athletes/{id}/season-plans` | Gera e valida uma proposta de plano geral |
| `GET` | `/api/athletes/{id}/season-plans/latest` | Consulta a versão mais recente do plano |
| `GET` | `/api/athletes/{id}/season-plans` | Consulta o histórico de planos |
| `POST` | `/api/athletes/{id}/season-plans/{planId}/review` | Aprova ou rejeita um rascunho |
| `POST` | `/api/athletes/{id}/weekly-plans` | Detalha uma semana de um plano geral aprovado |
| `GET` | `/api/athletes/{id}/weekly-plans/latest` | Consulta a versão semanal mais recente |
| `GET` | `/api/athletes/{id}/weekly-plans` | Consulta o histórico de uma semana |
| `POST` | `/api/athletes/{id}/weekly-plans/{planId}/review` | Aprova ou rejeita uma versão validada |
| `POST` | `/api/athletes/{id}/weekly-plans/{planId}/regenerate` | Rejeita a versão e solicita nova proposta |
| `POST` | `/api/athletes/{id}/weekly-plans/{planId}/edits` | Cria uma versão manual validada |
| `GET` | `/api/athletes/{id}/weekly-plans/{planId}/garmin/preview` | Pré-visualiza a compilação Garmin |
| `POST` | `/api/athletes/{id}/weekly-plans/{planId}/garmin/deliveries` | Envia e agenda a semana aprovada |
| `POST` | `/api/athletes/{id}/weekly-plans/{planId}/garmin/confirmations` | Confirma a semana no calendário |
| `PUT` | `/api/athletes/{id}/weekly-plans/{planId}/garmin/deliveries/{deliveryId}` | Atualiza e reagenda uma sessão |
| `DELETE` | `/api/athletes/{id}/weekly-plans/{planId}/garmin/deliveries/{deliveryId}` | Cancela uma sessão externa |
| `POST` | `/api/v1/activities` | Persiste uma atividade enviada no corpo |
| `POST` | `/api/v1/activities/sync` | Executa a sincronização manual de todos os atletas |
| `POST` | `/api/v1/activities/sync/{athleteId}` | Executa a sincronização manual de um atleta |
| `GET` | `/api/v1/activities/sync/status` | Consulta status e contadores da sincronização |
| `GET` | `/api/v1/activities/sync/status/{athleteId}` | Consulta o status de um atleta |
| `POST` | `/api/v1/activities/{activityId}/comparison` | Calcula ou recalcula o cumprimento |
| `GET` | `/api/v1/activities/{activityId}/comparison` | Consulta o cumprimento persistido |
| `POST` | `/api/v1/activities/{activityId}/review` | Gera ou reutiliza a avaliação curta da atividade |
| `GET` | `/api/v1/activities/{activityId}/review` | Consulta a avaliação e a auditoria dos aprofundamentos |
| `GET` | `/api/v1/activities/comparisons/athletes/{athleteId}` | Lista comparações do atleta |
| `POST` | `/api/v1/activities/comparisons/athletes/{athleteId}/reconcile` | Registra sessões vencidas não executadas |

O contrato, as validações e um payload completo estão em
[`docs/athlete-assessment-api.md`](docs/athlete-assessment-api.md).
O fluxo, as regras e a integração OpenAI do plano geral estão em
[`docs/season-plan-api.md`](docs/season-plan-api.md).
O contrato do treinador semanal, os limites determinísticos e as fronteiras com
Garmin/adaptação estão em [`docs/weekly-plan-api.md`](docs/weekly-plan-api.md).
O fluxo de compilação, idempotência, estados e recuperação da entrega está em
[`docs/garmin-workout-delivery-api.md`](docs/garmin-workout-delivery-api.md).
O pipeline de descoberta, checkpoint, retries e importação está em
[`docs/activity-sync-api.md`](docs/activity-sync-api.md).
O pareamento, alinhamento, tolerâncias e cálculo reproduzível de cumprimento estão em
[`docs/activity-comparison-api.md`](docs/activity-comparison-api.md).
A política de avaliação, a ferramenta interna e as garantias de acesso progressivo
à telemetria estão em [`docs/activity-review-api.md`](docs/activity-review-api.md).

Não existe endpoint de login/autenticação do usuário e não há Spring Security
habilitado. A sincronização roda no startup e, por padrão, a cada duas horas. Ela
descobre metadados com uma janela de recuperação e baixa o FIT apenas de IDs ausentes.

## Persistência mapeada

As migrations V1–V15 criam atleta, credenciais Garmin, resumo de atividade, indicador
de teste VDOT, laps, telemetria e o esquema ainda não usado de planejamento. O fluxo
atual persiste o resumo recebido do microsserviço e, por cascata JPA, seus laps e
registros de telemetria. A V8 adiciona snapshots versionados da anamnese,
disponibilidade por dia, superfícies, equipamentos e histórico de saúde. Nenhum
cálculo da Etapa 3 depende da IA: o perfil Daniels é calculado pelo motor
determinístico, e toda proposta OpenAI é novamente validada pelo backend antes de
ser persistida. A V9 versiona planos, fases, semanas e critérios de revisão. A V10
cria planos semanais versionados e enriquece sessões, blocos e passos com totais,
instruções e ritmos Daniels auditáveis. A V11 registra revisão e aprovação; a V12
persiste entrega Garmin, IDs externos, idempotência, tentativas e confirmações. A V13
adiciona checkpoint, status e contadores do pipeline de sincronização. A V14 persiste o
pareamento e os resultados determinísticos total e por etapa para reuso semanal.
A V15 registra avaliações curtas e toda solicitação justificada de detalhe; a
telemetria de 10 s, 5 s ou bruta não é copiada para essas tabelas.

## Testes

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
.\mvnw.cmd test
```

Os testes usam H2 em memória, Flyway desabilitado e clientes externos simulados; não
tocam no MySQL local nem fazem login externo. As respostas OpenAI do treinador de
atividade também são simuladas, inclusive chamadas de ferramenta. A primeira auditoria do baseline
executou as sete migrations com sucesso contra um MySQL 8 local vazio. A suíte cobre
carga do contexto, atividades, anamnese, motor Daniels e ciclos completos dos
planos geral e semanal. A integração OpenAI é testada sem rede e sem consumir
tokens.

## Riscos conhecidos

- autenticação e autorização da API ainda não existem;
- a chave de criptografia anterior esteve versionada e precisa ser rotacionada;
- a criptografia atual usa AES sem modo autenticado e não possui rotação de chave;
- o e-mail Garmin fica legível no banco e as credenciais são enviadas ao
  microsserviço a cada sincronização;
- a descoberta é limitada à quantidade configurada; intervalos muito longos sem
  execução podem exigir ampliar `GARMIN_SYNC_DISCOVERY_LIMIT` temporariamente;
- feedback e adaptação dos próximos treinos continuam fora desta etapa;
- a integração Garmin não oficial pode mudar sem aviso e precisa de monitoramento.
