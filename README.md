# World Finder

World Finder is a client-side NeoForge seed map for vanilla Minecraft worlds.

It is designed as an in-game alternative to ChunkBase for Minecraft `26.1.2`: enter a server seed manually, or let the mod read the seed automatically in singleplayer, then browse biomes and seed-based structure markers from inside the game.

This project is currently a beta. Use it as a map/finder helper, not as a guaranteed replacement for `/locate`.

## Requirements

- Minecraft `26.1.2`
- NeoForge `26.1.2`
- Client side only
- Vanilla world generation

## Main Features

- Fullscreen seed map opened with `M` by default.
- Singleplayer seed auto-detection.
- Manual seed input for multiplayer servers where the owner shared the seed.
- Overworld, Nether, and End map views.
- Biome colors with `Fast` and `Detail` render modes plus separate `Surf` and `Deep` layers.
- Biome search and biome highlighting.
- Seed-based structure markers using Minecraft item icons.
- Structure filter grid with persistent enabled/disabled filters.
- Completed structure markers saved locally.
- Slime chunk overlay.
- Chunk grid toggle.
- Player marker using the player head/skin when available.
- World spawn marker.
- End navigation helper with a 1000-block radius overlay.
- Right-click map menu for chat, teleport when allowed, and waypoint creation.
- Optional waypoint integration for JourneyMap, Xaero's Minimap/World Map, and FTB Chunks when those mods are installed.

## Known Limits

- The mod is vanilla-only. Modded world generation is not supported.
- Multiplayer accuracy depends on entering the exact server seed and matching the vanilla version.
- Teleport requires permission to run teleport commands.
- Some structure/template details are seed-based predictions and should still be checked in-game.
- The End terrain view is seed-based and still needs more visual testing at every zoom level.

## Controls

- `M`: open World Finder.
- Mouse drag: pan the map.
- Mouse wheel: zoom toward the cursor.
- Max zoom-out is limited to `1px / 4 blocks`.
- Left click a structure filter: toggle it.
- Right click a structure filter: show only that structure.
- Right click the map: open the context menu.
- Ctrl + click the map: toggle the biome under the cursor as a biome filter.

## Credits And Licenses

World Finder is licensed under `LGPL-3.0-or-later`.

The project uses or adapts work from:

- `xpple/SeedMapper`: seed-map cache/executor design and generated Java bindings, `LGPL-3.0-or-later`.
- `xpple/cubiomes`: Java bindings around cubiomes, `MIT`.
- Cubitect's `cubiomes`: vanilla seed/worldgen algorithms, `MIT`.

See `NOTICE.md` for details.
