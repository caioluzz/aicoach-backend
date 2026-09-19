# Anamnese versionada do atleta

A anamnese é um snapshot estruturado e imutável. Cada envio cria uma nova versão;
versões anteriores permanecem disponíveis para rastrear qual contexto fundamentou
uma futura decisão do treinador. O schema inicial das respostas é `1.0`.

## Endpoints

| Método | Caminho | Resultado |
| --- | --- | --- |
| `POST` | `/api/athletes/{athleteId}/assessments` | Cria a próxima versão e retorna HTTP 201 |
| `GET` | `/api/athletes/{athleteId}/assessments/latest` | Retorna a versão mais recente |
| `GET` | `/api/athletes/{athleteId}/assessments` | Retorna o histórico, da versão mais nova para a mais antiga |

O `POST` exige um atleta já cadastrado. Atualizar respostas significa enviar outro
snapshot completo; a API não altera uma versão existente. A criação bloqueia o
registro do atleta durante a numeração para evitar duas versões iguais em envios
concorrentes.

## Exemplo de criação

```json
{
  "onboardingStatus": "COMPLETED",
  "physicalProfile": {
    "dateOfBirth": "1990-01-01",
    "weightKg": 70.0,
    "heightCm": 175,
    "gender": "MALE"
  },
  "runningProfile": {
    "experienceLevel": "INTERMEDIATE",
    "runningYears": 4,
    "currentWeeklyVolumeKm": 35.0,
    "recentAverageWeeklyVolumeKm": 32.0,
    "recentLongestRunKm": 14.0,
    "currentRunsPerWeek": 4
  },
  "availability": [
    { "dayOfWeek": "TUESDAY", "availableMinutes": 60 },
    { "dayOfWeek": "THURSDAY", "availableMinutes": 60 },
    { "dayOfWeek": "SUNDAY", "availableMinutes": 120 }
  ],
  "preferredLongRunDay": "SUNDAY",
  "surfaces": ["ROAD", "TRACK"],
  "equipment": ["GPS_WATCH", "HEART_RATE_MONITOR"],
  "strengthTraining": {
    "sessionsPerWeek": 2,
    "notes": "Treino funcional"
  },
  "health": {
    "hasMedicalRestrictions": false,
    "medicalRestrictions": null,
    "issues": [
      {
        "bodyArea": "joelho",
        "description": "Lesão antiga, sem sintomas atuais",
        "status": "RESOLVED",
        "painSeverity": 0,
        "startedOn": "2024-01-01",
        "restrictionNotes": null
      }
    ]
  },
  "recovery": {
    "averageSleepHours": 7.5,
    "sleepQuality": "GOOD",
    "recoveryDaysPerWeek": 2,
    "routineType": "FIXED",
    "routineNotes": "Trabalho diurno"
  },
  "targetRace": {
    "title": "10K",
    "date": "2027-03-14",
    "distanceMeters": 10000,
    "desiredTimeSeconds": 3000,
    "priority": "A_RACE"
  }
}
```

## Regras do contrato

- cada dia de disponibilidade aparece no máximo uma vez;
- o dia preferido para o longo precisa estar disponível;
- cada disponibilidade informa de 15 a 1440 minutos;
- restrições médicas marcadas como existentes exigem uma descrição;
- dor ativa usa intensidade de 1 a 10; questões resolvidas podem usar zero;
- a prova-alvo deve estar no futuro e ter distância e tempo desejado positivos;
- `IN_PROGRESS` identifica respostas ainda não confirmadas e `COMPLETED` encerra
  o onboarding naquela versão;
- campos textuais complementam os valores estruturados, mas não os substituem.

Dados de saúde não são registrados em logs pela implementação. A resposta nunca
inclui e-mail ou senha Garmin. O acesso à API continua sujeito à limitação conhecida
do MVP: autenticação e autorização ainda não estão implementadas.

## Valores enumerados

- experiência: `BEGINNER`, `RECREATIONAL`, `INTERMEDIATE`, `ADVANCED`;
- superfícies: `ROAD`, `TRACK`, `TRAIL`, `TREADMILL`;
- equipamentos: `GPS_WATCH`, `HEART_RATE_MONITOR`, `TREADMILL`, `TRAIL_SHOES`,
  `STRENGTH_EQUIPMENT`;
- questão de saúde: `RESOLVED`, `MANAGED`, `ACTIVE`;
- sono: `POOR`, `FAIR`, `GOOD`, `EXCELLENT`;
- rotina: `FIXED`, `FLEXIBLE`, `SHIFT_WORK`, `VARIABLE`;
- prioridade: `A_RACE`, `B_RACE`, `C_RACE`.
