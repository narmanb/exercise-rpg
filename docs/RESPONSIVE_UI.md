# Responsive UI Rules

Path of the Wild must not be designed around one phone model or one aspect ratio.

## Rules

- Compose layouts respond to the **current app window**, not a saved device resolution.
- No gameplay screen may require a fixed portrait or landscape resolution.
- Compact windows use bottom navigation; wider windows use a navigation rail.
- Primary content is constrained to readable maximum widths on tablets instead of stretching controls across the screen.
- Scrollable screens use `LazyColumn`/responsive grids so short displays remain usable.
- Touch targets should remain at least 48 dp where practical.
- System safe-drawing insets are respected.
- The world grid sizes itself from available width rather than pixel assumptions.
- Orientation changes, split-screen, tablets, foldables, and desktop-sized Android windows must preserve state.

## Test matrix

Before calling a UI milestone complete, check at least:

- 320 × 568 dp compact portrait
- 360 × 800 dp common portrait
- 412 × 915 dp tall portrait
- 800 × 360 dp short landscape
- 1280 × 800 dp tablet/large window
- Resizable/split-screen widths around the 600 dp navigation breakpoint

The specific device used during development is never the layout specification.
