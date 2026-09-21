# Avaliação da atividade com detalhe sob demanda

Esta entrega implementa a Etapa 9 sobre o resumo determinístico persistido pela
Etapa 8. A branch foi criada a partir de `master` depois do merge da PR #9; não há
dependência pendente de branch ou PR anterior.

## Fluxo

1. `POST /api/v1/activities/{activityId}/review` exige uma comparação já persistida.
2. `ActivityReviewPolicy` decide em código se existe valor em chamar a OpenAI.
3. Atividades sem execução, com menos de 10 minutos/1 km, ou plenamente cumpridas
   (pelo menos 95% e sem etapa discrepante) recebem avaliação determinística e
   status `SKIPPED`.
4. Nos demais casos, o coach recebe apenas o resumo total e por etapa da Etapa 8.
5. Se faltar evidência, o modelo pode chamar a ferramenta interna
   `get_activity_segment_details`. Essa ferramenta não é exposta como endpoint.
6. Cada solicitação é validada e persistida com intervalo, campos, resolução,
   contexto e motivo antes de qualquer ponto de telemetria ser montado.
7. O resultado curto é persistido e pode ser consultado com
   `GET /api/v1/activities/{activityId}/review`.

Chamadas repetidas ao `POST` reutilizam avaliações concluídas ou ignoradas, evitando
novo custo. Respostas registram modelo, ID, tokens e latência.

## Contrato da ferramenta interna

`get_activity_segment_details` recebe:

- `queryType`: `TIME` (segundos decorridos) ou `KILOMETER` (quilômetros);
- `start` e `end`: menor intervalo que contém a suspeita;
- `resolution`: `STEP`, `TEN_SECONDS`, `FIVE_SECONDS` ou `RAW`;
- `fields`: combinação de `PACE`, `HEART_RATE`, `CADENCE` e `ALTITUDE`;
- `contextBefore` e `contextAfter`: até 60 s ou 0,25 km;
- `reason`: justificativa específica com pelo menos 12 caracteres.

`STEP` devolve somente os agregados determinísticos já persistidos. A visão inicial
do coach já contém esses agregados, então uma primeira inspeção de telemetria pode
começar em `TEN_SECONDS`. `FIVE_SECONDS` exige uma consulta `TEN_SECONDS`
sobreposta; `RAW` exige uma consulta `FIVE_SECONDS` sobreposta. A ferramenta também
limita o tamanho dos intervalos conforme a resolução:

| Resolução | Tempo máximo | Distância máxima |
| --- | ---: | ---: |
| `STEP` | 6 h | 100 km |
| `TEN_SECONDS` | 30 min | 5 km |
| `FIVE_SECONDS` | 10 min | 2 km |
| `RAW` | 2 min | 0,5 km |

Assim, telemetria em alta resolução nunca integra o contexto inicial e só é
entregue após uma solicitação justificada, auditada e progressivamente refinada.

## Persistência

A migration V15 cria:

- `activity_reviews`: decisão da política, avaliação curta e metadados da chamada;
- `activity_segment_detail_requests`: trilha auditável de cada aprofundamento.

A política atual é versionada como `activity-review-policy-v1`. As regras de skip,
limites de janela, contexto e progressão são determinísticas e não ficam no prompt.

## Configuração

```text
OPENAI_MODEL_ACTIVITY_REVIEW=gpt-5-mini
OPENAI_ACTIVITY_REVIEW_MAX_OUTPUT_TOKENS=2500
OPENAI_ACTIVITY_REVIEW_MAX_INSPECTIONS=4
```

Os testes substituem o coach e as respostas da OpenAI por doubles locais. Nenhuma
chamada real à OpenAI ou ao Garmin é feita.
