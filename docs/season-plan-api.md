# Plano geral até a prova

## Responsabilidade

O plano geral é o mapa estratégico entre a data de criação e a prova-alvo. Ele
contém fases, objetivos, progressão semanal prevista, semanas de recuperação,
taper e critérios de revisão. Ele deliberadamente não contém sessões diárias,
blocos ou passos de treino; o treinador semanal da Etapa 4 está documentado em
[`weekly-plan-api.md`](weekly-plan-api.md).

Cada versão fica vinculada ao atleta, objetivo, versão concluída da anamnese e
perfil Daniels mais recente. Uma nova geração nunca altera uma versão anterior.

## Pré-requisitos

- atleta existente;
- objetivo `ACTIVE`, futuro e pertencente ao atleta;
- objetivo com data e distância iguais à prova da anamnese mais recente;
- anamnese mais recente com onboarding `COMPLETED`;
- perfil Daniels persistido em `athlete_metrics`.

## Endpoints

### Criar uma proposta

```http
POST /api/athletes/7/season-plans
Content-Type: application/json

{"objectiveId": 12}
```

A resposta `201` sempre nasce com status `DRAFT`. A chamada pode retornar:

- `404` se o atleta não existir;
- `422` se faltarem pré-requisitos ou a proposta falhar na validação determinística;
- `502` se a OpenAI estiver indisponível, recusar ou devolver resposta incompleta/inválida.

### Consultar

```text
GET /api/athletes/{athleteId}/season-plans/{planId}
GET /api/athletes/{athleteId}/season-plans/latest
GET /api/athletes/{athleteId}/season-plans
```

### Aprovar ou rejeitar

```http
POST /api/athletes/7/season-plans/21/review
Content-Type: application/json

{"decision":"APPROVE","comment":"Estrutura inicial aprovada"}
```

`decision` aceita `APPROVE` e `REJECT`. Somente um `DRAFT` pode ser revisado. Ao
aprovar uma nova versão para a mesma prova, a versão aprovada anterior passa a
`SUPERSEDED`. O sistema não envia nada ao Garmin nesta etapa.

## Validação anterior à persistência

A saída da OpenAI só é persistida quando todas as regras forem satisfeitas:

- quantidade, numeração e datas exatas das semanas;
- fases ordenadas, contíguas e cobrindo o plano inteiro;
- fase final encerrando o plano;
- volume positivo e limitado a 150% da maior referência recente;
- progressão ordinária de no máximo 10% por semana;
- no máximo quatro semanas de carga sem recuperação em planos longos;
- redução mínima de 10% nas semanas de recuperação e taper;
- taper contínuo, com no máximo três semanas e incluindo a semana da prova;
- critérios `HEALTH_CHANGE`, `RACE_CHANGE`, `INTERRUPTION` e
  `PROGRESS_DIVERGENCE` presentes e descritos.

Qualquer violação rejeita a proposta inteira antes do `save`.

## Responses API

O gateway usa `POST /v1/responses`, sem armazenamento remoto (`store: false`). A
saída é solicitada por `text.format` com `type: json_schema`, `strict: true` e
`additionalProperties: false` em todos os objetos. O backend ainda valida o
domínio porque conformidade estrutural não garante coerência esportiva.

O parser exige `status: completed`, procura `output_text`, trata `refusal`
explicitamente e registra ID da resposta, modelo, tokens e latência. Erros HTTP
`429`, `5xx` e falhas de conexão recebem retry limitado. Outros `4xx` falham sem
retry. Contrato baseado na documentação oficial:

- [Responses API](https://developers.openai.com/api/reference/typescript/resources/beta/subresources/responses/methods/create)
- [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)

## Configuração

```text
OPENAI_API_KEY              obrigatória para gerar
OPENAI_API_URL              padrão https://api.openai.com/v1
OPENAI_MODEL_PLANNER        padrão gpt-5-mini
OPENAI_MAX_OUTPUT_TOKENS    padrão 8000
OPENAI_TIMEOUT_SECONDS      padrão 60
OPENAI_MAX_RETRIES          padrão 2
```

A chave não é persistida nem registrada em log.
