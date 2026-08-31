# Combat milestone status

The first playable combat slice is integrated with the overworld.

Implemented in this milestone:

- FFX-style speed timeline with repeated turns for faster combatants.
- Action-specific turn delay.
- Adventurer positioned behind up to three captured monsters.
- Center-slot monster guards the Adventurer from ordinary enemy targeting while conscious.
- Special attacks may explicitly bypass center protection.
- Adventurer commands include Attack, Skills, Item, Defend, and prototype Capture.
- Captured monsters use up to four species techniques plus universal Focus.
- Focus restores MP and guards for the waiting period.
- Capture currently succeeds at or below 30% HP as a temporary rule only.
- Captured species are persisted to the character-specific monster roster.
- Monster roster stores Bond independently from protagonist-synchronized level.
- Party slot assignment is explicit and does not automatically force the first capture into Center.

Next planned slice:

1. Roster/formation management UI.
2. Build battles from the player's actual active roster instead of temporary party members.
3. Award Bond to conscious active monsters after victory.
4. Expand species-specific combat techniques and basic stat scaling.
5. Revisit the final capture procedure later rather than treating the 30% threshold as final design.
