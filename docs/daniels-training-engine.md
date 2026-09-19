# Daniels training engine v1

## Scope and source

This module implements the deterministic part of Stage 2. Its primary source is
Jack Daniels, *Formula de corrida de Daniels*, Brazilian Portuguese second
edition (Artmed, 2013), translated from the 2005 second edition. The source PDF
is kept outside this Git repository in the local project aggregator.

Relevant source sections are Tables 2.1, 3.1 and 3.2 and Chapters 5 to 9. The
coefficient set is identified as `daniels-gilbert-vdot-2ed`; the engine release is
`daniels-2ed-ptbr-v1`. No OpenAI call is involved.

## VDOT from the 3 km test

Input units are integer metres and seconds. The prescribed test distance is
3,000 m; a project policy tolerance of +/-30 m accommodates small GPS error,
while the calculation is normalized to 3,000 m. Durations must be from 3:30 to
60:00 and the supported result is VDOT 20.00 to 85.00.

For velocity `v` in metres per minute and duration `t` in minutes:

```text
VO2 = -4.60 + 0.182258*v + 0.000104*v^2
fraction = 0.8 + 0.1894393*exp(-0.012778*t)
               + 0.2989558*exp(-0.1932605*t)
VDOT = VO2 / fraction
```

VDOT is rounded half-up to two decimals. The source test values and calculation
instant are retained in the immutable result.

## Pace ranges

Paces use whole seconds per kilometre. A quadratic inversion of the running
oxygen-cost equation is evaluated at the Table 2.1 intensity boundaries:

| Zone | VDOT fraction | Purpose |
| --- | ---: | --- |
| E | 59-74% | easy, recovery and long running |
| M | 75-84% | marathon-specific steady running |
| T | 83-88% | lactate-threshold development |
| I | 95-100% | aerobic-power development |
| R | I pace minus 15 s/km | speed and running economy |

The R rule is Daniels' six seconds per 400 m relationship for distance runners.
Every range stores `fastestSecondsPerKm` first and `slowestSecondsPerKm` second.
Final conversion rounds to the nearest whole second.

## Deterministic limits

- Keep a weekly load for at least three weeks before increasing it.
- Increase distance by at most 1.5 km per weekly session, capped at 15 km; or
  duration by six minutes per session, capped at 60 minutes.
- Keep at least 80% of weekly running time in E or M.
- Leave at least one intervening recovery day between T, I and R sessions.
- Long E running is capped at 30% of weekly distance and 150 minutes.
- M running is capped at 150 minutes and 16 miles (25.75 km).
- Continuous T lasts 20-60 minutes. T cruise intervals last 3-15 minutes with
  recovery equal to one fifth of work time. Total T is capped at 10% of weekly
  distance or time and 60 minutes.
- I repetitions last 30 seconds to five minutes, ideally three to five minutes.
  Recovery is active E running and no longer than the preceding work. Total I
  is capped at 8% of weekly distance/time, 10 km and 30 minutes.
- R repetitions last at most two minutes with recovery two to four times the
  work duration. Total R is capped at 5% of weekly distance or 3% of weekly
  time, 8 km and 20 minutes.

Validators return stable issue codes and do not silently repair a prescription.
The one-intervening-day recovery rule and the 3 km GPS tolerance are explicit
conservative project policies, rather than numeric rules attributed to Daniels.

## Neutral workout contract

`WorkoutDefinition` is the provider-neutral `workout.v1` contract. It contains
the engine version, date, ordered repeatable blocks and steps, duration in metres
or seconds, Daniels intensity, optional pace range and a short instruction. It
contains no Garmin identifier, payload field or delivery state. Garmin
compilation and weekly/season planning remain outside Stage 2.
