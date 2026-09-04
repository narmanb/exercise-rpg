# Incremental Guide Architecture Review

Reference reviewed: `Sasani-Likes-Penguins/Godot-Incremental-Game-Guide`.

## Scope and authority

Path of the Wild / Exercise RPG remains authoritative. The reference is a documentation guide, not a runnable Godot game repository, so this work adapts useful architectural ideas rather than forking or redesigning the game around it.

The approved rules remain unchanged:

- real exercise is the only source of exercise rewards;
- walking grants Adventure Points plus modest protagonist XP;
- exercise type never determines RPG class;
- captured monsters level with the protagonist and Bond/Mastery remains a second progression axis;
- existing party, turn-forecast combat, capture, overworld, local-area, inventory, calorie, workout, and Momentum directions remain authoritative.

## Licensing constraint

The reference repository currently has no detected license and no `LICENSE` file. Treat its prose and GDScript examples as reference material only. Do not copy implementation code verbatim into this repository unless a compatible license or explicit permission is later provided. Reimplement ideas independently in Kotlin.

## Useful ideas to adapt

### 1. Explicit service boundaries

The guide recommends separating systems behind service boundaries and communicating through events rather than allowing UI, combat, save, and progression code to call through one large object. The Kotlin equivalent should be small domain services/interfaces rather than a Godot Autoload singleton.

Target fitness boundary:

`Fitness Sources -> Activity Reconciliation -> Validated Exercise Events -> Reward Engine -> RPG / Adventure Progression`

Health Connect, Android cumulative step counter, platform detector, custom motion tracking, future wearable/import sources, and debug simulation should all enter through source-neutral observation contracts.

### 2. Offline reconciliation as a load-time pipeline

The guide's idle system records a timestamp and computes what happened while the game was closed. Exercise RPG must not turn elapsed time into rewards, but the pipeline shape is useful:

1. load the last processed source cursor/baseline;
2. query/read new source observations;
3. determine the unprocessed delta for each source;
4. reconcile overlapping sources;
5. validate/filter activity;
6. produce canonical validated exercise deltas;
7. advance the monotonic eligible-activity ledger;
8. let `FitnessRewardEngine` grant only newly crossed reward thresholds;
9. persist the new reconciliation cursors and reward watermark.

### 3. Data-driven static game definitions

The guide keeps static item/enemy/skill/dungeon definitions separate from mutable player state. Path of the Wild should gradually move hard-coded catalogs (monster definitions, encounter definitions, equipment, dungeon definitions, achievement definitions) toward validated data definitions. This is useful even before modding exists.

### 4. Mastery as a second axis

The guide's per-item mastery is conceptually compatible with monster Bond/Mastery: normal monster combat level should continue to follow the protagonist, while a separate per-owned-monster progression track can unlock relationship/mastery bonuses and milestones.

Do not import the guide's mastery pool automatically. A shared mastery pool is not currently part of the approved monster design.

### 5. Event-driven achievements/statistics

A central statistics/achievement observer is useful. It can listen to canonical domain events such as validated walking rewards, map discoveries, captures, bond milestones, victories, boss clears, item discoveries, and workout records without those systems depending on achievement code.

### 6. Data-driven dungeon state

Useful patterns include immutable dungeon definitions, separate mutable clear/progress state, explicit entry requirements, ordered encounter sequences, boss metadata, and persistent clear counts. The existing Path of the Wild combat engine remains authoritative.

## Things Path of the Wild already does better

### Fitness exactly-once rewards

`FitnessRewardEngine` already uses a monotonic rewarded eligible-step watermark. Repeated synchronization does not repeatedly grant the same walking XP, Adventure Points, or Momentum.

### Cross-source step reconciliation

`StepReconciler` already distinguishes confirmed Health Connect steps from live unconfirmed steps and prevents a later Health Connect catch-up from blindly adding the same activity twice. The reference guide does not solve overlapping external fitness sources because that is outside its idle-game problem.

### Save validation and rollback

The current `.potw` backup format has an explicit format version, typed values, required-store validation, character-epoch consistency checks, and rollback if import fails. The guide's sample save system is simpler and should not replace this.

### Native Android lifecycle/fitness integration

Path of the Wild already has Health Connect, Activity Recognition, Android sensor handling, foreground fitness-service experimentation, runtime capability checks, and Android-specific persistence. The guide's Android chapter is primarily Godot export/UI advice and does not improve these platform integrations.

### RPG combat direction

Path of the Wild already has a party-aware turn timeline, turn forecast, techniques, formation/guard behavior, capture, persistent vitals, inventory items, and planned active RPG combat. The guide's Melvor-style auto-combat loop is not a replacement.

## Do not import

- elapsed-time-generated exercise or fake idle walking rewards;
- Melvor-style automatic/tick combat as the main battle system;
- RuneScape/Melvor XP numbers unchanged;
- prestige/reset loops unless separately approved later;
- a mastery pool for monsters unless separately approved;
- Godot Autoload/EventBus implementation code;
- Godot-specific Android export structure;
- unvalidated runtime JSON overrides during the first playable milestone;
- any reference code verbatim while the reference repository remains unlicensed.

## Staged integration plan

### Stage 1 — Fitness pipeline contracts (now)

Add source-neutral observation/cursor/event contracts and pure tests. Do not rewire the live tracker or rewards yet.

Goals:
- make cumulative-source baseline/delta semantics explicit;
- handle source epoch/reset cleanly;
- define a canonical validated-exercise event boundary;
- keep `FitnessRewardEngine` independent of source type.

### Stage 2 — Reconciliation implementation

Generalize the current step reconciliation behind the new contracts while preserving current save keys and behavior. Add adapters for Health Connect, platform counter/detector, and custom motion. Add tests for overlapping sources, reopen, repeated sync, reboot, source switching, delayed Health Connect, and provider corrections.

Do this only after the custom motion tracker has acceptable real-step sensitivity.

### Stage 3 — Persistence hardening and migrations

Add a save-schema migration registry before format version 2 is needed. Prefer staged/transaction-like writes for reconciliation state and reward watermarks. Keep manual backup validation/rollback. Add crash-interruption tests around reconciliation checkpoints.

### Stage 4 — Progression configuration

Keep the current prototype XP curve until gameplay tuning exists. Introduce a table/config boundary so protagonist XP curves and walking reward rates can be changed without touching save semantics or fitness reconciliation.

### Stage 5 — Monster Bond/Mastery

Create a separate Bond/Mastery rules module with data-driven milestone thresholds and bonuses. Monster effective combat level continues to follow protagonist level. Bond gains must come from approved monster interactions/gameplay rather than determining class or duplicating normal levels.

### Stage 6 — Statistics and achievements

Add an event/statistics ledger, then achievements built on top of it. Candidate categories: lifetime validated steps, exploration, captures, bond/mastery, bosses, RPG challenges, workouts, and carefully designed streaks.

### Stage 7 — Combat/dungeon data

Move encounter, enemy, boss, equipment, and dungeon definitions toward immutable data definitions while retaining the existing Path of the Wild battle engine. Add persistent boss/dungeon clear counts and progression gates where design calls for them.

### Stage 8 — Mod-ready data layer

Later, define validated JSON schemas and import rules for static content. Keep executable code mods out of the first implementation. Android storage/import UX must be designed explicitly rather than copying Godot's `user://mods/` convention.
