# Treinador semanal

## Responsabilidade e fronteiras

O treinador semanal transforma uma semana do plano geral aprovado em um
microciclo detalhado. Cada versão preserva o plano geral, a semana macro, a fase,
a anamnese e o perfil Daniels que originaram as sessões.

Esta etapa termina no contrato neutro de sessões, blocos e passos. Ela não compila
payload Garmin, não envia workouts, não ingere resultado de treino e não adapta o
plano depois da execução.

## Pré-requisitos

- atleta existente;
- plano geral pertencente ao atleta e com status `APPROVED`;
- semana e fase existentes no plano;
- snapshot de anamnese e perfil Daniels vinculados ao plano aprovado;
- ausência de restrição médica declarada ou dor ativa grave (7 ou mais).

O planejamento usa o snapshot aprovado, e não troca silenciosamente para uma
anamnese ou métrica mais recente.

## Endpoints

### Gerar uma versão

```http
POST /api/athletes/7/weekly-plans
Content-Type: application/json

{"seasonPlanId":21,"weekNumber":4}
```

A resposta `201` contém o volume total calculado e a hierarquia
sessão -> bloco repetível -> passo:

```json
{
  "id": 31,
  "athleteId": 7,
  "seasonPlanId": 21,
  "seasonPlanWeekNumber": 4,
  "version": 1,
  "weekStart": "2026-10-05",
  "weekEnd": "2026-10-11",
  "targetVolumeKm": 40.0,
  "plannedDistanceMeters": 39850,
  "plannedDurationSeconds": 12740,
  "summary": "Semana de consolidação com um estímulo T.",
  "sessions": [
    {
      "order": 1,
      "name": "Rodagem fácil",
      "scheduledDate": "2026-10-06",
      "workoutType": "EASY_RUN",
      "plannedDistanceMeters": 8000,
      "plannedDurationSeconds": 2640,
      "blocks": [
        {
          "order": 1,
          "repetitions": 1,
          "steps": [
            {
              "order": 1,
              "kind": "WORK",
              "durationType": "DISTANCE",
              "durationValue": 8000,
              "targetZone": "E_PACE",
              "targetPaceFastestSecondsPerKm": 330,
              "targetPaceSlowestSecondsPerKm": 330,
              "instruction": "Ritmo confortável"
            }
          ]
        }
      ]
    }
  ]
}
```

Os ritmos são derivados pelo backend do perfil persistido; não são aceitos da
resposta do modelo.

### Consultar

```text
GET /api/athletes/{athleteId}/weekly-plans/{weeklyPlanId}
GET /api/athletes/{athleteId}/weekly-plans/latest?seasonPlanId=21&weekNumber=4
GET /api/athletes/{athleteId}/weekly-plans?seasonPlanId=21&weekNumber=4
```

Cada nova geração acrescenta uma versão. Versões anteriores não são alteradas.

## Validação anterior ao `save`

A proposta inteira é rejeitada com `422` quando qualquer regra falha:

- sessões ordenadas, em datas únicas, dentro da semana e em dias disponíveis;
- duração de cada sessão limitada aos minutos disponíveis daquele dia;
- quantidade de corridas limitada pelo perfil e pelos dias de recuperação;
- volume calculado a no máximo 5% do alvo da semana macro;
- no máximo um longo, no dia preferido, limitado a 30% do volume e 150 minutos;
- no máximo duas sessões de qualidade, ou uma em recuperação/taper;
- sessões T/I/R espaçadas por pelo menos um dia e cercadas por aquecimento e
  desaquecimento em E;
- ao menos 80% do tempo de treino em E/M e limites semanais de T/I/R;
- durações e recuperações dos estímulos conforme o motor Daniels;
- lesão ativa moderada (dor 4 a 6) impede T/I/R; restrição médica ou dor 7+
  bloqueia a geração antes da chamada externa;
- a semana da prova contém exatamente uma sessão `RACE`, na data e distância do
  objetivo.

Passos em tempo são convertidos em distância pelo ritmo Daniels; passos em
distância são convertidos em tempo pela mesma fonte. Repetições de bloco entram
em todos os totais.

## Responses API e segurança

O gateway chama `POST /v1/responses`, envia `store:false` e exige Structured
Outputs com JSON Schema estrito e `additionalProperties:false`. O parser exige
`status:completed`, trata `refusal`, captura uso/latência e aplica retry limitado
somente a conexão, `429` e `5xx`.

Configuração:

```text
OPENAI_API_KEY                       obrigatória para gerar; nunca persistida ou registrada
OPENAI_API_URL                       padrão https://api.openai.com/v1
OPENAI_MODEL_WEEKLY_PLANNER          padrão gpt-5-mini
OPENAI_WEEKLY_MAX_OUTPUT_TOKENS      padrão 12000
OPENAI_TIMEOUT_SECONDS               padrão 60
OPENAI_MAX_RETRIES                   padrão 2
```

O SDK/API da OpenAI recomenda carregar a chave do ambiente, e Structured Outputs
garante conformidade estrutural; as regras esportivas continuam obrigatoriamente
no backend:

- [Quickstart](https://developers.openai.com/api/docs/quickstart)
- [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)

Os testes usam um gerador mock/stub e respostas locais; nunca leem
`OPENAI_API_KEY` e nunca consomem tokens.
