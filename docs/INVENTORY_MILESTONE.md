# Inventory and economy milestone

The first persistent inventory/economy slice connects exploration, shops, rewards, and combat to one character-scoped inventory.

Implemented:

- Character-scoped persistent coins and stackable items.
- New characters begin with a small prototype coin balance and two Field Tonics.
- Field Tonic restores HP and is consumed from real inventory when its targeted battle action resolves.
- The battle command displays the currently owned Field Tonic count and disables the item when none remain.
- Greenrest's Wayfarer Goods opens a real shop and purchases persist immediately.
- Echo Cave's supply chest grants a one-time persistent reward and stays opened for that character.
- Battle victories grant prototype coin rewards that scale modestly with protagonist level.
- The overworld exposes a compact inventory panel showing coins and owned item stacks.
- Item definitions are centralized so shop data, inventory data, and battle effects share the same Field Tonic definition.
- Reward, purchase, stack-limit, consumption, and battle-item catalog behavior have regression tests.

Current prototype items:

- Field Tonic — HP restoration; fully connected to battle use.
- Focus Draught — MP restoration item definition and shop inventory exist, but battle use is not connected yet.

Still intentionally provisional:

- Prices, starting coins, chest contents, and battle coin rewards are balance placeholders.
- The final item menu UX is not designed yet; the current battle command exposes Field Tonic directly.
- Equipment, key items, selling, loot tables, rarity, and larger shop inventories come later.
- Inventory and local-object progress currently persist in separate stores; a future database/save-system pass can make multi-system reward transactions atomic.
