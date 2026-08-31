# Local-area maps

Local areas are separate free-movement maps entered from overworld points of interest such as towns and caves.

## Approved movement rule

- Adventure Points are spent to open new traversable overworld tiles.
- Once the player enters a town, cave, dungeon interior, or similar local area, movement inside that map costs **0 Adventure Points**.
- Local terrain still has collision/passability rules; free movement does not mean walking through walls, water, or rock.
- Leaving a local area returns the player to the same overworld tile.
- If the player is still standing on a mapped town/cave overworld tile after leaving, the UI provides a direct re-entry action so walking away and back is unnecessary.

## Current prototype maps

### Greenrest

- 14×12 local town map.
- Grass/path layout with blocked building footprints and passable doors.
- Placeholder Trailside Inn, Wayfarer Goods shop, Town Scout NPC, landmark, and overworld exit.

### Echo Cave

- 12×10 cave map.
- Floor, rock, and water collision.
- Placeholder chest, landmark, local enemy encounter, and cave-mouth exit.
- Local encounter launches the same roster-driven battle system used by overworld encounters.

## Local progress

Local event state is scoped to the current character epoch.

Currently persistent:

- Opened chest IDs.
- Cleared local encounter IDs.

Resolved chest/encounter markers disappear from the local map. Starting a new character clears this local-event ledger so one character cannot inherit another character's local progress.

## Data-model direction

The local-area definition deliberately separates:

- map dimensions,
- terrain tiles,
- player start position,
- object/interactible positions and types,
- runtime progress.

This keeps authored map data separate from save-state and is intended to remain compatible with the future standalone tile-map editor. The editor should eventually export the same conceptual data rather than requiring hand-coded maps.

Local-area validation currently rejects maps where:

- dimensions or terrain size are invalid,
- start position is out of bounds or blocked,
- an interaction object is out of bounds,
- an interaction object is placed on blocked terrain,
- two interaction objects occupy the same tile.

## Still later

- Real art/tilesets and autotiling.
- Building interiors within towns where useful.
- Actual NPC dialogue and quests.
- Shop inventory/economy.
- Inn/rest functionality.
- Inventory-backed chest rewards.
- More dungeon encounters and bosses.
- Persistent local position between app sessions if that becomes desirable; currently the important permanent state is event resolution, while re-entering a local area starts at its entrance.
