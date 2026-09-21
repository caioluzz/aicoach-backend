# Feedback e adaptação

## Objetivo

A Etapa 10 registra a percepção do corredor e combina esse dado com o cumprimento
determinístico da Etapa 8. Nenhuma regra de segurança depende da OpenAI. A avaliação
da Etapa 9 continua disponível como explicação, mas não substitui os limiares abaixo.

## Endpoints

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/api/athletes/{athleteId}/feedback` | Registra feedback e cria a decisão de adaptação |
| `GET` | `/api/athletes/{athleteId}/feedback/decisions` | Retorna o histórico auditável mais recente primeiro |

O feedback pode ser associado a uma atividade por `activityId` ou apenas ao dia. Quando
associado, a atividade deve pertencer ao atleta e `feedbackDate` deve coincidir com a
data de início. As escalas de esforço e fadiga vão de 1 a 10; dor, de 0 a 10; sono,
de 0 a 24 horas. A localização é obrigatória quando a dor é maior que zero.

Exemplo:

```json
{
  "activityId": 44,
  "feedbackDate": "2026-09-20",
  "perceivedEffort": 9,
  "fatigue": 7,
  "sleepHours": 6.5,
  "painSeverity": 0,
  "painLocation": null,
  "feeling": "BAD",
  "comment": "O treino ficou muito mais difícil que o esperado."
}
```

## Regras versionadas

A versão inicial é `feedback-adaptation-rules-v1`, aplicada nesta ordem:

1. dor `>= 7`: suspensão das sessões seguintes e proposta de revisar o plano geral;
2. dor entre 4 e 6: redução de 40% e retirada de intensidade;
3. fadiga `>= 8` com sono `<= 6 h`: redução de 35% e retirada de intensidade;
4. esforço `>= 9` com execução parcial, diferente, excedida, não executada ou
   abaixo de 75%: redução de 25% e retirada de intensidade;
5. sinais isolados menores geram somente monitoramento.

Dor moderada ou fadiga/sono recorrentes tornam-se causa material somente após duas
decisões anteriores de redução nos últimos 14 dias. Assim, o terceiro evento propõe
revisão do plano geral; eventos isolados alteram no máximo a semana corrente.

## Revisão dos próximos treinos

A decisão examina os sete dias seguintes e somente planos `APPROVED` ou `DELIVERED`.
Sessões que já possuem comparação são excluídas. Cada proposta guarda valores
originais e propostos de distância, duração e estresse, com uma das ações:

- `KEEP`;
- `REDUCE`;
- `REPLACE_WITH_EASY`;
- `REST`.

O plano aprovado não é editado silenciosamente. As propostas ficam persistidas para
revisão humana. Enquanto a decisão estiver na janela de sete dias, o próximo plano
semanal recebe a redução como contexto e o validador aplica o volume efetivo e bloqueia
T/I/R quando indicado. Uma suspensão de 100% impede gerar uma nova semana até haver
avaliação e novo feedback. Esse limite evita que cada variação diária recrie o plano
semanal ou o plano geral.

## Auditoria

`athlete_feedback` preserva a entrada do corredor. `adaptation_decisions` preserva a
versão das regras, os valores usados, o resumo do cumprimento, os alertas e a causa
material. `workout_adjustments` preserva a revisão individual de cada sessão futura.
O fluxo não chama OpenAI nem Garmin.
