# Correspondência e cumprimento de atividades

O backend compara corridas importadas com sessões planejadas sem usar IA. O resultado total e os
resultados por etapa ficam persistidos nas tabelas `activity_comparisons` e
`activity_step_comparisons`, podendo ser reutilizados pelo treinador semanal.

## Pareamento

Para cada corrida, o backend procura sessões do mesmo atleta entre D-1 e D+1. Uma sessão na mesma
data sempre é elegível; em datas adjacentes, a diferença de duração ou distância precisa ser de no
máximo 35%. A escolha minimiza diferença de data, distância e duração, usando o id como desempate.
Uma sessão e uma atividade só podem participar de um pareamento.

Sem candidata, a corrida recebe `matchType=UNPLANNED`, `classification=DIFFERENT` e percentual
zero, pois não existe prescrição contra a qual calcular cumprimento. O endpoint de reconciliação
persiste sessões vencidas sem corrida como `MISSED`, `NOT_EXECUTED` e zero.

## Alinhamento de etapas

Blocos repetidos são expandidos na ordem bloco, repetição e etapa. O alinhamento usa:

1. amostras FIT (`RECORDS`), fechando cada intervalo ao atingir seu tempo ou distância;
2. laps (`LAPS`) quando não há amostras suficientes;
3. rateio dos totais (`TOTAL`) como fallback explicitamente identificado;
4. `NONE` quando não existe execução disponível.

Cada resultado registra limites de tempo e distância, duração, distância, ritmo calculado,
frequência cardíaca e cadência observadas. FC e cadência são informativas enquanto o modelo de
treino não prescrever faixas para esses campos; portanto não alteram silenciosamente a nota.

## Política `activity-comparison-v1`

As tolerâncias são versionadas em `ComparisonTolerancePolicy` e a versão usada é gravada em cada
resultado:

- duração: 10%;
- distância: 10%;
- ritmo: 5% além da faixa prescrita;
- ritmo de 360 s/km apenas para converter etapas sem faixa de ritmo em tempo esperado no rateio;
- excedido: mais de 15% acima do alvo de duração ou distância;
- cumprido: percentual maior ou igual a 90;
- parcial: percentual maior ou igual a 60 e menor que 90;
- diferente: percentual menor que 60;
- não executado: até 5% do critério primário da etapa, ou sessão vencida sem atividade.

Para uma métrica com alvo `T`, valor realizado `A` e tolerância `r`, a nota é:

```text
desvio = abs(A - T) / T
nota = max(0, 100 * (1 - max(0, desvio - r)))
```

O ritmo recebe 100 dentro da faixa e usa o limite mais próximo fora dela. Em cada etapa, o critério
primário (tempo ou distância) pesa 60% e o ritmo 40%, quando existe alvo de ritmo. O total usa 70%
para a média das etapas ponderada pelo tempo esperado, 15% para duração total e 15% para
distância total. Somente componentes aplicáveis entram no denominador:

```text
percentual = soma(nota_do_componente * peso) / soma(pesos_aplicáveis)
```

A explicação persistida inclui versão, notas, pesos, percentual e classificação. Reprocessar os
mesmos dados com a mesma versão produz o mesmo resultado.

## Endpoints

- `POST /api/v1/activities/{activityId}/comparison`: calcula ou recalcula e persiste;
- `GET /api/v1/activities/{activityId}/comparison`: recupera o resultado persistido;
- `GET /api/v1/activities/comparisons/athletes/{athleteId}`: lista resultados reutilizáveis;
- `POST /api/v1/activities/comparisons/athletes/{athleteId}/reconcile?throughDate=AAAA-MM-DD`:
  registra treinos não executados até a data informada.

Corridas novas são comparadas automaticamente após a importação. Se o cálculo falhar, a
atividade continua importada e a sincronização seguinte tenta recuperar a comparação ausente.
