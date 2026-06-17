# Changelog

## 0.1.2

### Added

- Added a 1000-block radius navigation overlay in the End.
- Added a dedicated map tile texture manager to improve texture reuse and reduce rendering spikes.

### Changed

- The map now opens on the player's current dimension while still allowing manual dimension switching afterward.
- Improved End rendering performance by using terrain/void data directly instead of resolving every End biome tile.
- Improved End readability with an opaque void background so the world behind the UI no longer shows through empty map areas.
- Improved map loading stability when switching dimensions quickly.
- Reduced underground biome loading pressure so the map remains more responsive while deep biome tiles load.

### Fixed

- Fixed manual dimension switching being reset back to the player's current dimension.
- Fixed old biome jobs delaying newly selected dimensions.
- Fixed End center void areas taking too long to display.
- Fixed completed or hovered map state sometimes carrying over when dimensions changed.

## 0.1.1

### Added

- Added safer teleport handling that searches for a valid two-block air space before placing the player.
- Added waypoint creation support for compatible minimap mods, with stricter availability checks.
- Added per-dimension biome texture caching so switching dimensions no longer evicts already loaded map textures.
- Added client-side filtering for World Finder teleport feedback messages to keep chat clean.

### Changed

- Reworked End map rendering with cubiomes terrain height data so void, islands, and the central island are visually distinct.
- Improved biome map loading performance with larger async throughput, texture upload throttling, and better cache reuse.
- Improved map responsiveness and panel layout for different screen sizes.
- Improved biome and structure cache behavior so maps remain available after loading and do not reload unnecessarily when switching dimensions.
- Updated structure/filter icons and marker visuals for a more consistent interface.

### Fixed

- Fixed End biome tiles being misaligned with real world generation.
- Fixed End void sometimes rendering with the same color as islands.
- Fixed dimension changes clearing or invalidating map display state unexpectedly.
- Fixed `/wf reload` so it properly refreshes World Finder caches, profile data, markers, and open map state.
- Fixed teleport destinations sometimes placing the player inside blocks or unsafe terrain.
- Fixed waypoint integration detection showing unavailable integrations.
- Fixed slime chunk detection to use vanilla-compatible seed logic.
- Fixed structure marker filtering and several incorrect/missing marker icons.
