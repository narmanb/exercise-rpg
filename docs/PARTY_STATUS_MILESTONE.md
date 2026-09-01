# Party status milestone

This slice makes persistent battle attrition visible outside combat and removes duplicated player-stat construction.

Implemented:

- `PlayerPartyFactory` is the shared source for protagonist combat stats and active player-party combatants.
- `RosterBattleFactory` uses the shared party factory instead of maintaining a separate protagonist HP/MP/speed formula.
- The overworld shows a compact Party condition panel beneath the monster roster.
- The panel applies saved persistent vitals to the same derived combatants used by battle.
- Each active member shows current/max HP, current/max MP, formation slot, and KO state.
- The panel summarizes whether the party is fully restored, wounded, KO'd, or below full MP.
- Empty active monster slots are reported without inventing placeholder combatants.
- Regression tests cover shared stat scaling, formation filtering, persisted vitals, and equality between the battle player side and `PlayerPartyFactory` output.

Behavior notes:

- No saved vitals entry means that combatant is at its currently derived full HP/MP.
- Level changes therefore raise maximum stats without requiring migration of saved values; saved current HP/MP are clamped when reapplied.
- Reserve monsters are not displayed in Party condition until assigned to an active formation slot, but their saved vitals remain character-scoped and can reappear when they return to the active party.
- Greenrest's inn clears saved attrition, so the panel immediately returns to full derived values after resting.

Still provisional:

- The panel is intentionally compact and text-first; final visual treatment, portraits, bars, status-effect icons, and detailed character sheets come later.
- Persistent status ailments are not implemented yet.
- Current HP/MP persistence is still stored separately from the eventual unified RPG database/save transaction layer.
