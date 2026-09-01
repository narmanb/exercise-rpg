# Detailed strength workout milestone

This slice extends the existing manual workout log without replacing its quick-session workflow.

## Implemented

- Strength workouts can optionally record a working load.
- Load units support pounds (`lb`) and kilograms (`kg`).
- Strength workouts can optionally record reps for each set using compact input such as `8/8/6`.
- Reps parsing also accepts commas and spaces.
- Saved workout history displays the strength details alongside the existing duration, effort, date, and note fields.
- Quick reuse restores the previous load, unit, and set-rep pattern along with the existing workout fields.
- Legacy workout entries remain valid; older records simply have no strength-detail fields.
- Non-strength workout categories discard strength-only fields rather than carrying hidden stale values into cardio, mobility, sport, or other sessions.
- Validation caps implausibly large values and set counts before persistence.
- Pure JVM tests cover parsing, validation, category isolation, unit handling, and display summaries.

## Persistence compatibility

The workout log remains stored inside the existing core `path_of_the_wild_save` preferences under `workout_history`. New JSON fields are optional:

- `load`
- `loadUnit`
- `setReps`

Because the manual backup system already captures the entire core preference store, detailed workout data is included automatically without changing the backup format version.

## Deliberate boundaries

- A strength entry currently uses one working load plus a rep count for each set. Different weights for individual sets are not modeled yet.
- Workout logging does not grant RPG XP, Momentum, stats, class changes, or other rewards in this milestone. Exercise-reward balance remains a separate design decision.
- Rest timers, exercise-specific charts, estimated one-rep max, volume analytics, personal records, and Health Connect workout import are not part of this slice.
