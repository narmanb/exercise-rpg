# Party vitals persistence milestone

This slice makes battle HP/MP attrition persist between encounters so healing, consumables, retreating, and inns have lasting RPG consequences.

Implemented:

- Current HP and MP are stored per character for the Adventurer and captured monsters.
- Victory, manual retreat, and defeat all save the participating player combatants' current HP/MP when leaving battle.
- The next encounter reapplies those saved values instead of silently restoring everyone to full.
- Saved values are clamped to current derived maximum HP/MP, so later level/stat changes cannot create invalid vitals.
- Monsters in reserve keep their previous wounds/MP state when a different active party fights.
- Newly captured monsters have no saved wound state and therefore begin at their current derived maximums.
- A party that enters combat with every player combatant at 0 HP immediately resolves as defeated instead of producing an unusable battle queue.
- Greenrest's Trailside Inn now performs a full restore. It clears saved attrition for the entire character roster, including reserve monsters.

Current persistence boundary:

- HP/MP are committed when battle ends or the player retreats, not after every individual combat action.
- Consumable inventory is already persisted when an item resolves. A future consolidated database/save transaction layer can make combat-vitals and inventory writes atomic if crash-safe mid-battle persistence becomes necessary.

Still provisional:

- Inn restoration is currently free; inn pricing/rest limits are not yet balance decisions.
- KO/revival rules outside the inn are still open for design.
- The overworld does not yet show an exact party-condition panel; that is the next UI slice.
