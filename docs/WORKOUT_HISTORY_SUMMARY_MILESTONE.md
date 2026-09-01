# Workout history summary milestone

This slice makes the existing manual workout history useful beyond a lifetime entry list while keeping exercise rewards completely separate.

## Implemented

- Training history can be viewed over 7-day, 30-day, 90-day, or all-time ranges.
- Date windows use local calendar days rather than raw elapsed-hour math.
- Each selected range reports:
  - workout count,
  - total logged minutes,
  - distinct active days,
  - minutes grouped by workout category.
- The history list follows the selected range and remains newest-first.
- Very large ranges show the latest 50 entries while the summary still uses the complete selected range.
- Future-dated records are excluded from summaries.
- Pure JVM tests cover range boundaries, category aggregation, active-day counting, all-time behavior, and future-date rejection.

## Deliberate boundaries

- These summaries are informational only. They do not award XP, stats, Momentum, streak bonuses, or other RPG rewards.
- There is no punitive streak system.
- Strength-volume analytics, personal records, charts, and exercise-specific progression remain separate future features.
