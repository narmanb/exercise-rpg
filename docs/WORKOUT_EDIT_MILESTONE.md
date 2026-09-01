# Workout editing milestone

This slice completes correction controls for the manual workout log.

## Implemented

- Existing workout history entries expose an Edit action.
- Editing reuses the normal workout form rather than maintaining a second set of validation rules.
- The original workout ID and performed-at timestamp are preserved.
- Category, name, duration, effort, note, working load, load unit, and per-set reps can be corrected.
- Saving an edit rewrites the existing entry instead of creating a duplicate workout.
- Cancel edit returns the form to a fresh workout state without changing history.
- Quick reuse is hidden while editing so a template cannot accidentally replace the entry being corrected.
- After an edit, workout-range summaries, quick-reuse templates, and strength personal records are recalculated from the updated history.
- The existing deletion confirmation remains available alongside Edit.
- Pure mutation tests cover replacement behavior and order preservation.

## Persistence compatibility

Editing uses the existing `workout_history` JSON in the core save store. No backup format or save migration is required.

## Deliberate boundary

This is record correction only. Editing a workout does not grant, remove, or recalculate RPG rewards because manually logged workouts still do not award RPG progression in the current prototype.
