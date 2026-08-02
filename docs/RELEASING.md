# Publishing API and Core

The source repository contains no publishing credential and does not commit generated artifacts to
`main`.

## Verify locally

Use Java 21 or newer from a clean checkout:

```powershell
.\gradlew.bat clean verifyLibraries
```

This verifies the API/Core tests, the frozen API 0.2 binary surface, Javadocs, Maven metadata,
external dependency resolution, Core's transitive API dependency, and the six distributable JARs.

## Publish the developer repository

1. Confirm that `mod_version` and `CHANGELOG.md` describe the intended library release.
2. Enable GitHub Pages with **GitHub Actions** as its source for `Azashiin/WorldFinder`.
3. Run the manual **Publish API and Core** workflow, or publish a matching GitHub release.
4. Resolve `fr.asashiin.worldfinder:worldfinder-api:<version>` from
   `https://azashiin.github.io/WorldFinder/` in a clean external addon project.

The workflow preserves older Maven coordinates and versioned Javadocs on the generated
`developer-pages` branch. Do not edit that branch manually.

Patch releases in the `0.2.x` line preserve source and binary compatibility for the API. Breaking
changes require a new API line and a migration note. Core remains implementation-oriented until
1.0.
