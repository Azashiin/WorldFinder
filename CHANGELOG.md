# Changelog

All notable changes to WorldFinder API and Core are documented in this file. The published API
follows Semantic Versioning.

## [Unreleased]

### Documentation

- Corrected the addon guide to describe WorldFinder 0.2.1's restored vanilla fallback on remote,
  modded, and unsupported-world previews.
- Clarified that addons replace or extend the generation they explicitly handle, while unhandled
  queries retain WorldFinder's version-specific vanilla reference.
- Confirmed that this correction does not change the API or Core binary surface. The published
  `0.2.0` coordinates remain compatible with every WorldFinder `0.2.x` client.

## [0.2.0] - 2026-08-02

### Added

- Stable loader-neutral addon entrypoint and registration contracts.
- Namespaced biome and structure targets for extending WorldFinder catalogs.
- World-generation contexts, capabilities, cancellation, point and regional biome queries,
  structure queries, and explicit handled, unsupported, and failed results.
- Vertical biome sampling contracts that distinguish surface queries from fixed-height queries.
- Optional loader-neutral waypoint provider contracts.
- Minecraft-independent Core services for bounded biome tiles, asynchronous caches, map state,
  structure clustering, resolver chains, diagnostics, and addon orchestration.
- Binary API-surface verification for the 0.2 line.
- Reproducible binary, source, Javadoc, and Maven publications for both modules.

### Compatibility

- `worldfinder-api` 0.2.x preserves source and binary compatibility.
- `worldfinder-core` remains implementation-oriented until 1.0.
