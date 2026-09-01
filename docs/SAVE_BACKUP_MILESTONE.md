# Save / Backup Foundation Milestone

This milestone adds a complete manual local-save export/import foundation while preserving Android automatic backup.

## Player flow

The Home screen now includes a **Save backup** card, and character creation includes a **Restore existing save** card so a player can recover after reinstalling without first creating a throwaway character.

- **Export save** opens Android's system document picker and creates a `.potw` backup file at the location chosen by the player.
- **Import save** opens the system document picker and reads a selected backup without requesting broad storage permission.
- Import is available both for an existing character and before character creation.
- A backup is fully decoded and validated before the current save is touched.
- Valid imports show the backed-up character name and require explicit confirmation before replacement/restoration.
- After a successful import, the activity recreates so all stores and UI state reload from the restored save.

## Snapshot scope

Format version 1 snapshots all current persistent RPG preference stores:

1. `path_of_the_wild_save` — character identity, fitness synchronization/reward ledger, calorie/food data, workout history, and other core state.
2. `path_of_the_wild_overworld` — overworld position, discovered/unlocked tiles, Adventure Point spending, resolved points of interest.
3. `path_of_the_wild_local_area_progress` — resolved local-area objects.
4. `path_of_the_wild_monsters` — captured monster roster, formation, bond data.
5. `path_of_the_wild_inventory` — coins and items.
6. `path_of_the_wild_party_vitals` — persistent HP/MP condition between battles.

Before export, character-scoped stores are initialized against the current character epoch. This makes a backup valid even if a new player exports before opening the Adventure screen for the first time.

## Format

`.potw` is a versioned UTF-8 text format with a `POTW_SAVE` header. Preference store names, keys, strings, and string-set members are URL-safe Base64 encoded so tabs, newlines, Unicode text, and empty strings can round-trip safely.

Supported SharedPreferences value types:

- String
- Int
- Long
- Float
- Boolean
- Set<String>

The encoder sorts stores, keys, and string-set members for deterministic output.

## Validation

An import is rejected before writing if any of the following is true:

- the file is empty or has the wrong header;
- the format version is unsupported;
- a store/value record is malformed or duplicated;
- an unknown value type appears;
- the six expected stores are not present exactly;
- character name or creation timestamp is invalid;
- any character-scoped store is missing its `character_epoch`;
- any scoped store's epoch does not match the core character creation timestamp;
- the selected file exceeds the current 2,000,000-character safety limit.

## Restore safety

The importer snapshots the existing save before replacement. All stores are written synchronously. If a store write fails, the importer attempts to restore the pre-import snapshot and reports failure rather than accepting a known partial import.

Android automatic backup remains enabled through `android:allowBackup="true"`; manual `.potw` export/import is an additional player-controlled recovery path.

## Tests

`SaveBackupCodecTest` covers:

- deterministic round-trip of every supported preference type;
- tabs/newlines/Unicode and string-set content;
- malformed-header rejection;
- missing-store rejection;
- missing character-epoch rejection;
- mismatched character-epoch rejection.

## Deliberately deferred

- Cloud-account synchronization.
- Multiple named in-app save slots.
- Encryption/password protection for exported files.
- Cross-version migrations beyond format version 1.
- A separate settings screen; manual backup currently lives on Home plus a restore-only entry during character creation.
