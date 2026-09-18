# AI RunCoach Backend

API Spring Boot que persiste atletas e atividades, agenda sincronizações e consome o
microsserviço Garmin. Este repositório é independente do repositório
`aicoach-microservice`; a pasta que contém ambos não é um monorepo.

## Requisitos

- JDK 17
- Docker com Compose (recomendado) ou MySQL 8 existente
- microsserviço Garmin em `http://localhost:8000` para sincronizações reais

## Execução local

Defina os segredos apenas no ambiente. O arquivo `.env.example` lista os nomes,
mas o Spring Boot não carrega `.env` automaticamente.

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
$env:DB_PASSWORD = 'senha-local-do-mysql'
$env:GARMIN_ENCRYPTION_KEY = [Convert]::ToBase64String(
  [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
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
| `POST` | `/api/v1/activities` | Persiste uma atividade enviada no corpo |

Não existe endpoint de login/autenticação do usuário e não há Spring Security
habilitado. A sincronização é interna: a cada intervalo configurado, o backend
consulta atletas, busca primeiro um teste cujo nome contenha `Teste 3km` ou `VDOT`
e, depois disso, persiste atividades posteriores ainda não conhecidas.

## Persistência mapeada

As migrations V1–V7 criam atleta, credenciais Garmin, resumo de atividade, indicador
de teste VDOT, laps, telemetria e o esquema ainda não usado de planejamento. O fluxo
atual persiste o resumo recebido do microsserviço e, por cascata JPA, seus laps e
registros de telemetria. Nenhum cálculo numérico de VDOT é executado nesta versão;
o sistema apenas identifica e marca a atividade de teste.

## Testes

```powershell
$env:JAVA_HOME = 'C:\caminho\para\jdk-17'
.\mvnw.cmd test
```

Os testes usam H2 em memória, Flyway desabilitado e cliente Garmin simulado; não
tocam no MySQL local nem fazem login externo. A primeira auditoria do baseline
executou as sete migrations com sucesso contra um MySQL 8 local vazio. A suíte cobre
carga do contexto, rejeição de atividade duplicada e persistência do teste VDOT com
lap e telemetria.

## Riscos conhecidos

- autenticação e autorização da API ainda não existem;
- a chave de criptografia anterior esteve versionada e precisa ser rotacionada;
- a criptografia atual usa AES sem modo autenticado e não possui rotação de chave;
- o e-mail Garmin fica legível no banco e as credenciais são enviadas ao
  microsserviço a cada sincronização;
- o método chamado `dailySyncRoutine` roda, por padrão, a cada 60 segundos;
- não há retry/backoff, endpoint manual de sincronização ou teste automatizado
  das migrations contra MySQL;
- as entidades de planejamento existem, mas motor Daniels, treinador OpenAI e envio
  de workouts Garmin estão fora deste baseline.
