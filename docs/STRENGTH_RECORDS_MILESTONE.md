# Strength records milestone

This slice derives useful personal records from the detailed strength workout history without changing workout persistence or RPG balance.

## Implemented

- Named strength exercises with recorded load and/or set reps appear in a compact Strength records section.
- Exercise names are grouped case-insensitively so entries such as `Bench Press` and `bench press` share one record.
- The most recent spelling/capitalization is used as the display name.
- Each record reports the latest logged strength details and number of detailed sessions.
- Personal-best tracking includes:
  - heaviest recorded working load,
  - highest reps in a single set,
  - highest total reps in one logged session.
- Pound and kilogram loads are compared by equivalent weight so switching units does not create a false record.
- The most recently trained exercises are shown first, with the display capped to a compact recent set.
- Future-dated entries, unnamed exercises, non-strength workouts, and strength entries with no detailed fields are excluded.
- Pure JVM tests cover grouping, unit comparison, rep records, ordering, filtering, and display formatting.

## Persistence compatibility

Strength records are fully derived from the existing workout history. No new save keys or backup-format changes are required.

## Deliberate boundaries

- These records are informational only. They do not award XP, stats, Momentum, items, streak bonuses, or other RPG rewards.
- This milestone does not calculate estimated one-rep max, tonnage/volume, bodyweight-relative strength, or exercise-specific charts.
- It does not decide whether a heavier low-rep set should rank above or below a lighter high-rep set beyond the explicit heaviest-load record.
