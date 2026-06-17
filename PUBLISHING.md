# Publishing Notes

## Upload

- File: `build/libs/worldfinder-0.1.2.jar`
- Version name: `0.1.2`
- Game version: `26.1.2`
- Loader: `NeoForge`
- Side: `Client`
- Java: `25`
- License: `LGPL-3.0-or-later`

## Short Summary

World Finder is a client-side seed map and structure finder for vanilla Minecraft worlds, built as an in-game ChunkBase-style helper.

## Description

World Finder lets you enter a known server seed, or automatically read the seed in singleplayer, then browse vanilla biomes and seed-based structure markers directly in-game. It supports Overworld, Nether, and End views, biome highlighting, structure filters, completed markers, teleport actions when allowed, and waypoint creation through supported map mods.

## Features

- Singleplayer seed auto-detection.
- Manual seed profiles for multiplayer servers.
- Overworld, Nether, and End seed map views.
- Surface and underground biome layers.
- Biome search and biome highlighting.
- Vanilla structure markers with Minecraft item icons.
- Persistent structure filters and completed marker state.
- Slime chunk and chunk grid overlays.
- Player head marker and world spawn marker.
- Right-click map menu for chat, teleport, and waypoints.
- Optional waypoint integration with JourneyMap, Xaero, and FTB Chunks.

## 0.1.2 Highlights

- Map opens on the player's current dimension, while still allowing manual dimension switching.
- Faster and cleaner End map rendering with opaque void background and a 1000-block navigation radius.
- Better cache reuse and texture upload throttling for smoother biome map loading.
- More stable behavior when changing dimensions quickly.
- Reduced underground biome loading pressure.

## Limits

- Vanilla world generation only.
- Multiplayer accuracy requires the exact server seed.
- Modded world generation is not supported.
- Teleport requires permission to run teleport commands.
- This is a beta; structure results should still be checked in-game when precision matters.

## Credits

World Finder uses or adapts work from `xpple/SeedMapper`, `xpple/cubiomes`, and Cubitect's `cubiomes`. See `NOTICE.md`.
