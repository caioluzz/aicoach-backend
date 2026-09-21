# Provas intermediárias

## Objetivo e fronteira

Uma prova intermediária pertence a um plano geral aprovado, mas não é um novo
`Objective` principal. O `primaryObjective` do plano permanece inalterado e aparece
em toda resposta para tornar essa garantia verificável. A prova aceita somente
`B_RACE` ou `C_RACE`, deve estar no período do plano e terminar sua janela de
proteção ao menos oito dias antes da prova-alvo.

O ajuste é um overlay determinístico e versionado (`secondary-race-rules-v1`) sobre
as semanas já geradas. Ele não chama OpenAI, não altera fases ou metas do plano geral
e não muta silenciosamente uma semana aprovada ou já entregue ao Garmin. Cada item
informa o treino original e a proposta efetiva; a revisão/aprovação e eventual nova
entrega continuam nos fluxos das Etapas 5 e 6.

## Cadastro

`POST /api/athletes/{athleteId}/secondary-races`

```json
{
  "globalPlanId": 9,
  "title": "5 km do bairro",
  "raceDate": "2026-10-04",
  "distanceMeters": 5000,
  "targetTimeSeconds": 1200,
  "priority": "B_RACE"
}
```

A resposta contém a prova principal preservada, a janela afetada, a versão da regra,
a justificativa geral e os ajustes ordenados por data. `weeklyPlanId` e
`plannedActivityId` ligam cada decisão ao plano existente. Quando não existe treino
de qualidade adequado na semana, `ADD_RACE` tem esses IDs nulos.

`GET /api/athletes/{athleteId}/secondary-races` devolve o histórico em ordem de data.

## Regras determinísticas

- a prova substitui o treino `QUALITY_1` ou `QUALITY_2` mais próximo na mesma semana;
- sem treino de qualidade, a prova é adicionada explicitamente à agenda proposta;
- uma prova B usa janela de cinco dias antes e quatro depois, com taper curto,
  descanso no dia seguinte e retorno por sessões de recuperação;
- uma prova C usa janela menor, de três dias antes e dois depois;
- outro treino no dia da prova é removido;
- intensidade próxima é trocada por rodagem fácil, e distância/duração são
  recalculadas pelo percentual de carga registrado;
- mudanças que atravessam domingo/segunda-feira mantêm o `weeklyPlanId` de cada
  semana, tornando explícito o impacto na semana adjacente.

O cadastro rejeita plano não aprovado, data passada, duplicidade no mesmo plano,
prioridade A e prova que invada a proteção final da prova-alvo.

## Auditoria e integrações

`secondary_races` guarda a entrada, a prova principal, a janela, a versão da regra e
a explicação. `secondary_race_adjustments` guarda antes/depois de data, tipo,
distância, duração e percentual de carga. O cálculo reutiliza sessões semanais e seus
estados; não acessa Garmin e não requer credenciais. Testes usam apenas H2 e mocks.
