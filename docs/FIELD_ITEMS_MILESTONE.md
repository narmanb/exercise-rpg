# Field item milestone

This slice connects persistent inventory and persistent party vitals outside combat so restorative supplies matter during exploration as well as battles.

Implemented:

- Field Tonic can restore HP to a conscious active party member between encounters.
- Focus Draught can restore MP to a conscious active party member between encounters.
- Field use targets the same derived protagonist/monster combatants shown by Party condition and used by battle.
- A restorative item is validated before inventory consumption.
- Full-HP/full-MP targets reject the corresponding item without consuming it.
- KO'd targets reject both current restorative items; these supplies do not act as revival items.
- Successful use consumes exactly one inventory item and persists the resulting HP/MP immediately.
- The new Field supplies panel displays owned quantities and enables only targets that can actually benefit from the selected item.
- Pure rules tests cover healing, MP restoration, max-value clamping, full-resource rejection, and KO rejection.

Behavior notes:

- Field Tonic and Focus Draught use the same `ItemCatalog` definitions as shops and combat.
- Reserve monsters keep their stored vitals but are not field-item targets until assigned to an active formation slot.
- Resting at Greenrest's inn remains the full-party recovery option and does not consume items.

Current limitation:

- Inventory and party-vitals persistence are separate stores, so the successful field-use write is not yet one atomic database transaction. The operation validates the target first, consumes inventory second, then persists vitals. A future unified RPG database/save layer should make cross-system transactions atomic.
- Revival items, status recovery, equipment consumables, and utility field items are not implemented yet.
