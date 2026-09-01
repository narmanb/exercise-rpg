# Path of the Wild

Working title for a native Android exercise-driven monster-catching RPG.

Real-world walking feeds an actual RPG rather than replacing it: eligible steps provide Adventure Points for overworld discovery, Momentum for training actions, and a modest walking contribution to normal protagonist XP. Conventional gameplay XP now has its own persistent ledger so battles, quests, and other RPG systems can award the same character progression later without treating walking as the entire XP system.

The app is built in Kotlin with Jetpack Compose and is designed to adapt across phone, tablet, foldable, portrait, landscape, and resizable Android windows.

## Current playable foundation

### Fitness and progression

- Character creation with a character-specific fitness epoch so pre-character activity does not count.
- Health Connect step reading plus direct Android `TYPE_STEP_COUNTER` support.
- Persistent reconciliation between delayed Health Connect totals and immediate sensor steps to avoid double counting.
- Foreground-aware sensor registration and Health Connect refresh behavior.
- Persistent reward watermarks so app restarts, provider catch-up, and sensor reboots cannot replay earned rewards.
- Prototype walking rewards: Adventure Points, Momentum, and a modest XP contribution.
- Persistent protagonist XP model combining walking XP with a separate gameplay-XP source.
- Protagonist class foundation with a persistent class identity; the full class roster and class skill kits are still intentionally undecided.
- Fitness diagnostics exposing source counters, reconciliation state, tracking mode, and activity-recognition observations.

### Exploration

- Persistent overworld tile discovery driven by Adventure Points.
- Free-movement local maps entered from overworld points of interest.
- Greenrest town and Echo Cave prototype areas with collision, interactions, persistent resolved events, and return-to-overworld flow.
- Activity-recognition signal collection is present for future movement validation, but it does not currently reject or alter step rewards.

### Combat and monsters

- Playable speed-timeline combat with action-specific delay and a visible turn forecast.
- Adventurer plus up to three active captured monsters in North, Center, and South formation slots.
- Center-slot guardian protection for the Adventurer, with explicit support for attacks that bypass it.
- Persistent monster roster, reserve storage, formation management, protagonist-level synchronization, and Bond progression.
- Species-specific monster techniques, MP, Focus/guard behavior, player targeting, enemy turns, victory, and retreat.
- Prototype capture is wired into combat and persistence, but the current low-HP capture rule is temporary and not the final capture design.
- Overworld and local-area encounters use the same roster-driven battle system.

### Inventory, recovery, and economy

- Character-scoped persistent coins and stackable items.
- Greenrest shop purchases and one-time Echo Cave chest rewards.
- Inventory-backed battle items with real quantities and consumption.
- Persistent party HP/MP between encounters.
- Field Tonic and Focus Draught can restore active party members outside battle.
- Greenrest inn recovery and persistent party-condition display.
- Prototype battle coin rewards.

### Fitness logs and other systems

- Manual workout logging with Strength, Cardio, Mobility, Sport, and Other categories, duration, optional effort, notes, history, and quick reuse of recent exercises.
- Strength entries can additionally record an optional working load in pounds or kilograms and per-set reps such as `8/8/6`.
- Manual calorie logging, configurable daily target, and calorie-history chart/range views.
- Manual save export/import with validation and rollback protection.
- Character-scoped save state across overworld progress, local events, monsters, inventory, party vitals, protagonist class, fitness rewards, protagonist gameplay XP, and workout history.
- Responsive bottom navigation / navigation rail layouts.
- GitHub Actions compile, unit-test, debug-APK build, and artifact upload on `main`.

## Intentionally unfinished

This is still an active prototype. Combat numbers, reward rates, economy values, encounter compositions, final capture mechanics, protagonist class roster/skills, quests, deeper monster Bond systems, equipment, status effects, bosses, final art, broader world content, and exercise-to-RPG reward balance remain under development.

Workout logging currently records activity but does not grant RPG rewards. The project avoids locking unresolved design choices into permanent systems merely to fill placeholders.

## Documentation

Milestone notes in `docs/` describe the implemented boundaries and known temporary rules. `docs/ROADMAP.md` contains the broader design direction, while `docs/RESPONSIVE_UI.md` documents device-layout rules.
