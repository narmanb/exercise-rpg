# Core Save Modularization Milestone

This milestone is an engineering-only cleanup: it moves core preference-backed save models out of the 60+ KB `MainActivity.kt` without intentionally changing game behavior, save keys, or backup format.

## Extracted from MainActivity

`CoreGameStore.kt` now owns:

- `CharacterProfile`
- `FoodEntry`
- `GameStore`

The store continues to use the existing core SharedPreferences store named by `SaveBackupRules.CORE_STORE` (`path_of_the_wild_save`).

## Save compatibility

No preference keys were renamed or converted. Existing keys remain exactly the same, including:

- character name / creation epoch
- Health Connect baseline
- direct step-sensor baseline
- legacy prototype overworld compatibility keys
- calorie target
- per-day food entries

Because the underlying store and keys are unchanged, existing local saves and version-1 manual backups do not require a migration.

## Backup hardening

Manual backup capture already initializes character-scoped stores before reading them. It now also calls `ProtagonistClassStore.ensureCharacter(...)` before capture.

The protagonist class is stored in the core save rather than a separate SharedPreferences store. Explicitly ensuring it before export means legacy characters receive the approved Adventurer class keys before the snapshot is serialized, which avoids exporting a partially initialized class foundation.

## Why this was done

`MainActivity.kt` had accumulated activity lifecycle code, UI, character/profile persistence, prototype world compatibility state, and calorie-entry persistence in one file. This made otherwise small changes require large or temporary patch workflows.

Separating persistence from the activity reduces that coupling and makes later save/class work easier to test and review.

## Explicitly unchanged

This milestone does not alter:

- character creation behavior
- fitness baselines or rewards
- Adventure Point spending
- monster or party state
- food/calorie behavior
- backup format version
- protagonist class design
- any balancing values
