# WorldFinder API and Core

This repository contains the loader-neutral libraries used to create compatibility addons for
WorldFinder. It intentionally contains only the public API, the Minecraft-independent Core, and
their developer documentation. The WorldFinder mod, its user interface, native Minecraft adapters,
and Fabric or NeoForge builds are not part of this repository.

## Modules

- `worldfinder-api` is the stable public contract for addons. It defines biome and structure
  targets, world-generation queries and results, cancellation, capabilities, vertical biome
  sampling, addon registration, and optional waypoint providers.
- `worldfinder-core` provides Minecraft-independent map, cache, resolver-chain, and addon
  orchestration services. It exposes `worldfinder-api` transitively and is intended for advanced
  integrations and testing tools.

Both libraries target Java 21 and contain no Minecraft, Fabric, NeoForge, or map-mod classes.

## Using the API

Addon projects should compile against the API without bundling it:

```groovy
repositories {
    maven {
        url = uri('https://azashiin.github.io/WorldFinder/')
    }
    mavenCentral()
}

dependencies {
    compileOnly 'fr.asashiin.worldfinder:worldfinder-api:0.2.0'
}
```

The installed WorldFinder mod supplies the API at runtime. Addons must declare WorldFinder as a
required dependency in their Fabric or NeoForge metadata and must not shadow, include, or package
the API inside their own JAR.

The complete registration, resolver, vertical sampling, threading, and loader metadata contracts
are documented in [the addon guide](docs/ADDON_API.md).

## Building locally

Windows:

```powershell
.\gradlew.bat verifyLibraries
```

Linux or macOS:

```bash
./gradlew verifyLibraries
```

The verification task runs API/Core tests, checks the frozen API 0.2 binary surface, validates
Javadocs, publishes both modules to an isolated Maven repository, resolves them as an external
consumer, and collects their binary, source, and Javadoc JARs under `build/release/libraries`.

To test an unpublished checkout from another project, publish locally with:

```powershell
.\gradlew.bat publishLibraries
```

The Maven repository is written to `build/repository` by default. It can be redirected with
`-Pworldfinder_maven_repository=<directory>`.

Maintainers can publish the versioned Maven metadata and Javadocs through the manual
`Publish API and Core` GitHub Actions workflow. The generated artifacts are kept on the dedicated
`developer-pages` branch; generated binaries are never committed to `main`.

## Compatibility policy

`worldfinder-api` preserves source and binary compatibility throughout the `0.2.x` line. Breaking
changes require a new API line and a documented migration path. `worldfinder-core` remains an
implementation-oriented library and does not guarantee binary compatibility before 1.0.

An addon must explicitly report unsupported world-generation profiles, dimensions, sampling modes,
or heights. It must never interpret an unsupported underground request as a surface request or
return guessed data as an exact result.

## License

WorldFinder API and Core are licensed under `LGPL-3.0-or-later`. See [LICENSE](LICENSE).
