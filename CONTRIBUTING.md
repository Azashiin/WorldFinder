# Contributing to WorldFinder

This repository accepts focused fixes, documentation improvements, performance work, and changes
to the public addon API or its loader-neutral Core implementation. Open an issue before starting a
large API change so the compatibility and versioning impact can be agreed first.

## Development requirements

- JDK 21 or newer; Gradle compiles and tests the libraries with the Java 21 toolchain.
- No copied or translated implementation from another seed-map or world-generation project.
- A clear source and compatible license for every new asset, fixture, or data table.
- Loader-neutral API signatures: public API types must not expose Minecraft, Fabric, NeoForge, or
  another mod's classes.

Run the complete library contract while developing API changes:

```powershell
.\gradlew.bat verifyLibraries
```

Before a release-affecting change is merged, run the same contract from a clean checkout:

```powershell
.\gradlew.bat clean verifyLibraries
```

## API compatibility

`worldfinder-api` 0.2.x preserves source and binary compatibility. Additive methods require safe
defaults when existing implementations would otherwise break. Removing, renaming, changing a
descriptor, or strengthening a precondition requires the next minor API line and a migration note.
`worldfinder-core` remains implementation-oriented until 1.0.

Update `docs/ADDON_API.md`, its complete inline addon example, and the API tests whenever an
addon-facing behavior changes. Do not document a contract that the public API and
external-consumer verification cannot compile.

## Pull requests

- Keep unrelated refactors separate.
- Explain the compatibility impact of every addon-facing change.
- Add contract coverage for changed API or Core behavior.
- Never commit caches, game files, logs, generated Maven repositories, credentials, or third-party
  mod JARs.
- Do not add game, loader, rendering, or native world-generation implementations to this
  API-and-Core repository.
