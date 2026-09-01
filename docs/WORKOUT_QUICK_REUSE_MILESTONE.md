# Workout Quick-Reuse Milestone

This milestone closes the roadmap gap that workout logging should remember recent exercises and previous values so repeated sessions are fast to enter.

## Player flow

The Training screen keeps the existing quick-session logger and adds:

- an optional **Exercise / session name** field;
- a **Quick reuse** card when workout history exists;
- up to four recent distinct exercise/session templates;
- one-tap refill of category, duration, effort, exercise/session name, and note;
- normal editing after refill before the new workout is saved.

Saving a reused workout always creates a new history entry. It never edits the older workout.

## Recent-template rules

`WorkoutQuickReuseRules` selects the most recent distinct templates.

- Matching is case-insensitive.
- The same named exercise in different workout categories remains distinct.
- If the same exercise/session was logged multiple times, the newest values are used.
- Legacy unnamed workouts collapse by their category for template purposes.
- The current Home/Training UI limit is four templates.

Workout names are trimmed, repeated whitespace is collapsed, and names are capped at 60 characters.

## Storage compatibility

`WorkoutEntry` now has an optional `name` field stored inside the existing `workout_history` JSON in `path_of_the_wild_save`.

Older entries that do not contain `name` remain valid and display their workout category as the fallback name. Existing callers of `WorkoutStore.add(...)` remain source-compatible because the new name argument is optional.

Because workout history remains inside the existing core save store, Android backup and manual `.potw` export/import automatically include the new field.

## Training history

History rows now show the exercise/session name when present while retaining category, duration, timestamp, optional effort, and notes.

## Tests

`WorkoutQuickReuseRulesTest` covers:

- name trimming, whitespace normalization, and maximum length;
- selecting the newest version of a repeated named exercise;
- keeping the same name distinct across workout categories;
- legacy unnamed workout behavior and template limits;
- non-positive template limits.

The integration candidate passed Android CI, JVM tests, debug APK assembly, and artifact upload before this milestone was documented.

## Deliberately deferred

- Structured set/rep/weight fields for strength exercises.
- Health Connect exercise-session import.
- Workout-derived RPG reward balancing.
- Daily/weekly objective integration.
