# Protagonist Class Foundation Milestone

This milestone adds the first persistent protagonist-class layer without inventing the still-undecided class roster, subclass tree, stats, or skill system.

## Approved design boundary

- Every protagonist starts as **Adventurer**.
- Exercise type does **not** determine or lock the protagonist's RPG class.
- A later RPG class/job choice will be made in-game.
- Exact later jobs/classes, subclasses, stats, and skills remain intentionally unspecified and are not introduced here.

## Persistence

`ProtagonistClassStore` stores class state in the core persistent save (`path_of_the_wild_save`) so manual save/export coverage follows the existing core-store backup path.

Stored keys:

- `protagonist_class_epoch`: character creation epoch the class state belongs to.
- `protagonist_class_id`: stable catalog ID for the current protagonist class.

Class state is character-scoped. If the saved class epoch belongs to another character, or a legacy/current save has no recognized class ID, the state initializes to `adventurer` for the current character.

## Catalog / migration behavior

`ProtagonistClassCatalog` currently contains only the approved starting class:

- `adventurer` — Adventurer

Unknown or missing IDs resolve safely to Adventurer. The assignment API rejects IDs that are not present in the approved catalog, preventing future UI from silently persisting arbitrary class strings.

## Home UI

Home now reads the persistent protagonist class instead of hardcoding the word `Adventurer`.

The character summary shows:

- current persisted class name
- current protagonist level
- walking XP / level progress

A small **Class path** card explains that Adventurer is the starting job and that exercise type never locks later class choices.

## Tests

`ProtagonistClassRulesTest` covers:

- missing class -> Adventurer
- unknown class -> Adventurer
- known Adventurer round-trip
- new-character epoch reinitialization
- legacy save with no class initialization
- valid current-character state remaining unchanged

## Explicitly not included

This milestone does **not** choose or implement:

- later class/job roster
- subclasses
- class-selection level or story trigger
- class stats
- class abilities / skills
- respec rules
- class balance
- equipment restrictions

Those remain design decisions for a later milestone.
