# Inventory and economy milestone

The persistent inventory/economy slice connects exploration, shops, rewards, and combat to one character-scoped inventory.

Implemented:

- Character-scoped persistent coins and stackable items.
- New characters begin with a small prototype coin balance and two Field Tonics.
- Greenrest's Wayfarer Goods opens a real shop and purchases persist immediately.
- Echo Cave's supply chest grants a one-time persistent reward and stays opened for that character.
- Battle victories grant prototype coin rewards that scale modestly with protagonist level.
- The overworld exposes a compact inventory panel showing coins and owned item stacks.
- The Adventurer has a real Item submenu in combat rather than an unlimited hard-coded item command.
- Battle item entries show currently owned quantities and disable at zero.
- The selected item is removed from persistent inventory only after a valid target is chosen and the action resolves.
- Field Tonic restores HP to one ally.
- Focus Draught restores MP to one ally.
- Item definitions are centralized so shop data, inventory data, battle effects, names, and effect power use the same catalog definitions.
- Reward, purchase, stack-limit, consumption, HP-item, MP-item, and catalog behavior have regression tests.

Current prototype items:

- Field Tonic — restores 70 HP to one ally.
- Focus Draught — restores 20 MP to one ally.

Still intentionally provisional:

- Prices, starting coins, chest contents, and battle coin rewards are balance placeholders.
- The Item submenu is functional but not the final visual design.
- Equipment, key items, selling, loot tables, rarity, status-curing items, and larger shop inventories come later.
- Inventory and local-object progress currently persist in separate stores; a future database/save-system pass can make multi-system reward transactions atomic.
