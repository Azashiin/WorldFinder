# Changelog

All notable changes to WorldFinder API and Core are documented in this file. The published API
follows Semantic Versioning.

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
