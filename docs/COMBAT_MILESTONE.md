# Combat milestone status

The first playable combat foundation is now integrated with both overworld and local-area encounters.

## Implemented

- FFX-style speed timeline with repeated turns for faster combatants.
- Action-specific turn delay.
- Adventurer positioned behind up to three captured monsters.
- Center-slot monster guards the Adventurer from ordinary enemy targeting while conscious.
- Special attacks may explicitly bypass center protection.
- Adventurer commands include Attack, Skills, Item, Defend, and prototype Capture.
- Captured monsters use species-specific technique loadouts of up to four techniques plus universal Focus.
- Focus restores MP and guards for the waiting period.
- Captured species persist to the character-specific monster roster.
- Captured monsters synchronize their effective battle level to the protagonist.
- Monster combat stats scale from species data and protagonist level.
- Monster Bond persists separately from synchronized level.
- Conscious active monsters receive prototype Bond progress after victory.
- Roster/formation management is playable with North, Center, South, and Reserve assignments.
- Captures enter Reserve rather than automatically forcing the first captured monster into Center.
- Battles now assemble the player side from the actual active roster instead of hard-coded temporary party members.
- Overworld encounters launch the real battle screen and remain cleared after victory.
- Local-area encounters can use the same battle system and persistent roster/Bond rules.

## Intentionally temporary / unresolved

- Capture currently succeeds at or below 30% HP. This is a prototype rule only, not the final capture procedure.
- Exact combat numbers, stat formulas, technique power, MP costs, action delays, and Bond gain amounts are tuning values.
- Full class/subclass combat kits for the protagonist are still to be designed.
- Status effects, equipment, advanced passives, boss mechanics, and deeper monster Bond unlocks remain later milestones.

## Next combat-facing work

1. Improve battle presentation and combat feedback while keeping the responsive portrait layout.
2. Add more encounter compositions and enemy behavior rather than using only the prototype pairings.
3. Connect inventory/items so the Item command uses actual owned consumables.
4. Add proper rewards/loot/XP for RPG battles.
5. Design the final capture procedure later with the user rather than locking in the 30% prototype rule.
