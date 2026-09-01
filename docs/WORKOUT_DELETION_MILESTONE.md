# Workout deletion milestone

This slice lets the player correct accidental workout logs without introducing a second workout database or special correction ledger.

## Implemented

- Each visible training-history entry has a Delete action.
- Deletion requires an explicit confirmation dialog showing the exercise/session name and duration.
- Cancelling or dismissing the dialog leaves the workout untouched.
- Confirmed deletion removes the entry from the existing persisted workout history.
- After deletion, the screen reloads the authoritative history so all derived views update together:
  - quick-reuse templates,
  - 7/30/90-day and all-time summaries,
  - category totals and active-day counts,
  - strength personal records.
- Workout serialization is shared between add and delete paths so both write the same compatible JSON format.
- Pure JVM tests cover targeted removal, preserved ordering, missing IDs, and empty histories.

## Persistence compatibility

Deletion continues to use the existing `workout_history` value inside the core `path_of_the_wild_save` preferences. No new save keys or backup-format changes are required.

## Deliberate boundaries

- Deletion is permanent after confirmation; there is no undo queue in this milestone.
- Editing an existing workout in place is not implemented yet.
- Only entries currently visible in the selected history range can be deleted from this UI.
- Deleting a workout does not directly alter RPG rewards because manual workouts still grant no RPG rewards at this stage.
