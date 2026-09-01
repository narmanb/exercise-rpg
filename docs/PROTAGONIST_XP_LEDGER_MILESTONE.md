# Protagonist XP Ledger Milestone

This milestone separates **normal protagonist XP** from the fitness ledger without changing any current reward or level balance.

## Problem addressed

Before this milestone, the protagonist's level was calculated directly from `totalWalkingXpGranted`. That made walking XP effectively the entire character XP system even though the approved design treats walking as one modest source of normal RPG XP alongside future gameplay sources such as battles and quests.

## Implemented

`ProtagonistProgressStore` adds a character-scoped non-fitness XP ledger in the existing core save.

Stored keys:

- `protagonist_progress_epoch`
- `protagonist_gameplay_xp`

The state is reset to zero whenever the character creation epoch changes. Legacy/current characters that have no gameplay XP key initialize at zero.

`ProtagonistProgressRules` now combines:

- walking XP from the fitness reward ledger
- gameplay XP from the protagonist progression ledger

into the total XP supplied to `RpgProgression`.

The sum is monotonic for normal positive awards and saturates at `Long.MAX_VALUE` instead of overflowing.

## Current behavior

No battle, quest, encounter, or other gameplay source awards gameplay XP yet.

Therefore, for every existing character at migration:

`total XP = walking XP + 0 gameplay XP`

so current level/progress is unchanged.

Home now displays total XP and the walking contribution separately. This makes the distinction visible without choosing any new reward values.

## Save / backup behavior

Gameplay XP is stored in `SaveBackupRules.CORE_STORE`, so the version-1 manual backup format does not need another required SharedPreferences store or a format bump.

Before manual export, `SaveBackupStore` explicitly ensures the current protagonist progression state exists. Legacy characters therefore export a fully initialized zero-gameplay-XP ledger rather than a partially initialized save.

## Tests

`ProtagonistProgressRulesTest` covers:

- combining walking and gameplay XP
- rejecting negative contributions from reducing total XP
- overflow-safe total XP
- positive-only gameplay XP growth
- overflow-safe gameplay awards
- new-character reset detection
- legacy missing-key initialization
- preserving initialized current-character state

## Intentionally not decided here

This milestone does **not** choose:

- battle XP amounts
- quest XP amounts
- encounter XP
- XP multipliers
- class-based XP modifiers
- exercise/walking XP rebalance
- the level curve

Those remain balance/design decisions. This milestone only provides the durable source separation required to support them later.
