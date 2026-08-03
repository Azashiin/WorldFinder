# WorldFinder Addon API 0.2

The `worldfinder-api` artifact is the public, loader-neutral contract for compatibility
addons. It lets a separate client mod contribute searchable biomes and structures, resolve
world-generation results, or expose an optional waypoint provider without depending on
WorldFinder internals.

Public signatures contain only Java and WorldFinder-owned types. Minecraft, Fabric, NeoForge,
and a target world-generation mod must stay behind the addon's own adapter.

## Version and compatibility policy

WorldFinder `0.2.0` implements API generation 2. Check the installed runtime through a method:

```java
if (WorldFinderApi.apiVersion() < 2) {
    throw new IllegalStateException("WorldFinder API generation 2 is required");
}
```

Do not use the deprecated `WorldFinderApi.API_VERSION` field for runtime negotiation. Java may
inline public constants into an addon at compile time, while `WorldFinderApi.apiVersion()` reads
the generation implemented by the installed runtime.

Patch releases in the `0.2.x` line preserve source and binary compatibility for the published
API. A breaking contract change requires a new minor line, such as `0.3.0`, and a migration note.
The published `worldfinder-core` artifact is implementation-oriented and does not have the same
compatibility guarantee before 1.0.

## Maven dependency

The 0.2.0 coordinates are:

```text
fr.asashiin.worldfinder:worldfinder-api:0.2.0
fr.asashiin.worldfinder:worldfinder-core:0.2.0
```

Most addons need only `worldfinder-api`. Compile against it without embedding it:

```groovy
repositories {
    maven {
        name = 'WorldFinder'
        url = uri('https://azashiin.github.io/WorldFinder/')
        content {
            includeGroup 'fr.asashiin.worldfinder'
        }
    }
}

dependencies {
    compileOnly 'fr.asashiin.worldfinder:worldfinder-api:0.2.0'
}
```

Do not use `implementation`, Fabric Loom `include`, Shadow, Jar-in-Jar, or another bundling
mechanism for the API. The installed WorldFinder mod supplies these classes at runtime, and the
loader metadata below makes that runtime dependency explicit.

The repository is anonymous and requires no GitHub account or access token. Published source and
Javadoc artifacts use the same coordinates. The browsable API documentation is available at
<https://azashiin.github.io/WorldFinder/javadoc/0.2.0/api/>.

To validate unpublished API changes, use the local file repository produced by this checkout:

```powershell
.\gradlew.bat :api:publishMavenJavaPublicationToWorldFinderLocalRepository
```

This writes binary, source, Javadoc, POM, and Gradle metadata below `build/repository`. Point the
addon build at that directory with an absolute path:

```groovy
repositories {
    maven {
        url = uri('C:/path/to/WorldFinder/build/repository')
    }
    mavenCentral()
}

dependencies {
    compileOnly 'fr.asashiin.worldfinder:worldfinder-api:0.2.0'
}
```

The WorldFinder build can place the file repository elsewhere with
`-Pworldfinder_maven_repository=<directory>`.

## Loader registration

Keep the `WorldFinderAddon` implementation and resolver in a shared source set. Loader-specific
code should only expose that implementation and declare WorldFinder as a required client
dependency. Registration is transactional: if `register` fails, none of that addon's targets or
resolvers are retained. Registration closes before map queries begin, so do not register from a
static initializer or attempt late registration.

### Fabric

Expose the implementation through WorldFinder's custom `worldfinder` entrypoint in
`fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "crystal_worldfinder_compat",
  "version": "1.0.0",
  "name": "Crystal World - WorldFinder Compat",
  "environment": "client",
  "entrypoints": {
    "worldfinder": [
      "com.example.crystalcompat.CrystalWorldFinderAddon"
    ]
  },
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": ">=1.21.1 <=26.2",
    "worldfinder": ">=0.2.0 <0.3.0"
  }
}
```

Also declare the target world-generation mod when the compatibility addon cannot operate
without it. Narrow the Minecraft range to the releases actually tested by the addon. Fabric
supplies the entrypoint instance to WorldFinder during client initialization.

### NeoForge

Expose the same implementation as a Java service. Create the following file in the addon JAR:

```text
META-INF/services/fr.asashiin.worldfinder.api.WorldFinderAddon
```

Its content is one fully qualified provider class per line:

```text
com.example.crystalcompat.CrystalWorldFinderAddon
```

The provider class must be public and have an accessible no-argument constructor. Declare the
runtime dependency in the addon's `META-INF/neoforge.mods.toml`, replacing the owner mod ID with
the addon's real ID:

```toml
[[dependencies.crystal_worldfinder_compat]]
modId="worldfinder"
type="required"
versionRange="[0.2.0,0.3.0)"
ordering="NONE"
side="CLIENT"
```

WorldFinder discovers these services during NeoForge client construction. Service discovery and
individual registration failures are isolated and reported, so one invalid provider does not
discard providers that loaded successfully.

The complete loader-neutral example later in this guide can be used from either a Fabric or a
NeoForge addon. Loader discovery metadata is the only part that differs between the two examples
above; the WorldFinder-facing implementation remains shared.

## World-generation context

Every query carries an immutable `WorldgenContext` containing the seed, vanilla dimension
family, server type, runtime attributes, and a `WorldgenProfile`. The profile is part of cache
identity and describes the declared base rules separately from the seed and dimension. It is not
an inventory of every installed mod or a remote server's private datapacks; registered resolvers
compose over that baseline and remain responsible for detecting their own compatibility mod.

For a vanilla Normal screen, WorldFinder supplies a version-specific profile such as:

```java
WorldgenProfile.vanilla("1.21.1", WorldgenProfile.VANILLA_NORMAL_PRESET_ID)
```

Its ID is `WorldgenProfile.VANILLA_PROFILE_ID`, its Minecraft version matches the running exact
release, its preset is `WorldgenProfile.VANILLA_NORMAL_PRESET_ID`, and its property map is empty.
A compatibility addon that is itself loaded
only alongside its target world-generation mod should at minimum check the version, preset, and
dimension in `supports`. Do not construct the legacy unspecified profile for new code, and do
not use transient data as profile properties. Future version adapters and identified generation
stacks can provide different profile IDs and stable properties.

`WorldgenContext.attributes()` contains runtime query metadata rather than world-generation
identity. In 0.2.0 it includes the loader name and, whenever at least one worldgen resolver is
registered, an opaque screen-session token used to isolate asynchronous caches. Treat unknown
attributes as optional; never persist the session token or make
generated output depend on undocumented keys.

Every exact native fallback is intentionally limited to that release's vanilla Normal profile.
The presence of another mod does not by itself disable this baseline. Automatic seed reuse is
disabled only when the integrated world exposes an unrecognized active preset or biome source, or
data that actually replaces Minecraft's vanilla generation definitions. With a manually entered
seed, the screen then labels its result as a vanilla Normal preview.

An addon must return `handled` results for generation it owns; it must not rely on the vanilla
fallback reproducing Amplified, Large Biomes, Single Biome, datapack, or modded terrain. Without a
compatible addon, WorldFinder deliberately keeps displaying the version-specific vanilla reference
instead of hiding it or claiming to reproduce unsupported custom generation.

## Resolver selection and fallback

Resolvers are ordered by descending `priority()`, with their namespaced ID as the deterministic
tie-breaker. `supports(context)` must be a cheap, non-blocking applicability check.

`ResolverResult` makes ownership explicit:

- `ResolverResult.unhandled()` delegates to the next resolver and eventually to WorldFinder's
  vanilla engine.
- `ResolverResult.handled(value)` claims the query. For structures, a handled empty list is a
  valid final answer and suppresses lower-priority fallback.

Biome tiles use this order:

1. WorldFinder offers the complete tile to regional resolvers through
   `resolveBiomeRegion`.
2. The first valid handled region owns the tile.
3. If every regional resolver returns unhandled, WorldFinder generates the vanilla tile.
4. Point resolvers are then offered each sample through `resolveBiome`; unhandled samples retain
   the vanilla value.

Regional resolution therefore has precedence over point resolution across the complete resolver
chain. A lower-priority regional implementation can handle a tile before a higher-priority
point-only implementation is considered. Implement `resolveBiomeRegion` for normal map work and
keep `resolveBiome` as a compatibility or unusual-query fallback.

WorldFinder 0.2 does not issue its native progressive-preview LOD while any worldgen resolver is
registered. The current capability contract cannot prove that a resolver which handles the target
`blockStep` also handles a different coarse step, and silently falling back to vanilla for that
temporary level would be incorrect. Each query an addon does receive therefore remains the actual
requested map level; a future API capability may make explicit multi-LOD previews opt-in.

`capabilities()` advertises optimized operations with `WorldgenCapability`. It does not replace
the handled/unhandled result. `REGION_BIOMES`, `POINT_BIOMES`, `TERRAIN_COVERAGE`, and
`STRUCTURES` describe the corresponding implementation paths.

Structure resolution is also addon-first. Every enabled built-in target is queried through its
reserved `worldfinder:<preset>` ID before native work starts. A resolver may replace that target
without registering new metadata; `handled(List.of())` intentionally suppresses it. Registered
custom structure IDs use the addon's own namespace and have no native fallback. The
`worldfinder` namespace is reserved for these built-ins.

Use the constants in `WorldFinderStructureTargets` instead of spelling reserved target IDs by
hand. They are grouped under `Overworld`, `Nether`, and `End`, with immutable `ALL` sets for
capability declarations and tests. For example:

```java
if (query.targetId().equals(WorldFinderStructureTargets.Overworld.VILLAGE)) {
    return resolveModdedVillages(query);
}
return ResolverResult.unhandled();
```

Resolver failures are isolated per scan cell, but they are never interpreted as a successful empty
answer. WorldFinder continues with healthy lower-priority resolvers and any native fallback,
publishes those safe markers, then retries the incomplete cell with bounded backoff. A persistent
addon failure is shown as a partial result. Throw `CancellationException` when the query token is
cancelled; cancellation is control flow and is not reported as an addon failure.

WorldFinder's built-in vanilla structure catalog remains available in every supported client
context. A recognized integrated vanilla world supplies one coherent live resource epoch.
Remote-server and unsupported-world previews instead use a separate private manager backed by the
exact vanilla templates bundled with the running Minecraft version. Addon resolvers and background
workers never receive Minecraft's live reloadable manager.

A manually entered seed therefore retains template-dependent starts and piece-derived variants
without requiring WorldFinder on the server. These results remain a reference for that Minecraft
version's vanilla generation; they do not claim to reproduce a remote server's private datapacks or
unsupported modded terrain.

A structure addon can replace a built-in target by handling its reserved `worldfinder:*` ID,
including with an intentionally empty result, or extend the catalog with its own namespaced
`StructureSearchTarget`. If no addon handles a built-in target, WorldFinder preserves the vanilla
baseline.

## Regional biome contract

`BiomeRegionQuery` describes a rectangular row-major sample grid. Sample `(x, z)` represents:

```text
blockX = originBlockX + x * blockStep
blockZ = originBlockZ + z * blockStep
blockY = sampleY                 when samplingMode == FIXED_Y
blockY = addon-derived surface   when samplingMode == SURFACE
```

A handled `BiomeRegion` must have exactly the requested width and height. Its biome channel is
palette-indexed: `biomePalette` stores unique namespaced IDs, while `biomeIndices[z * width + x]`
selects one entry for each sample. `BiomeSamplingMode.SURFACE` is explicit and currently valid
only in the Overworld; `sampleY` is nominal in that mode. A world-generation addon that owns
custom terrain derives its own surface projection. `FIXED_Y` means the supplied Y is exact.
Point resolvers receive the same mode through `BiomeQuery`.

Register every custom biome returned by the engine as a `BiomeSearchTarget` so it has a stable map
color and appears in the search UI. Do not register metadata for `minecraft:*` biomes merely to
replace their calculation: native biome IDs are reserved, but a resolver may return them directly.
`SURFACE` and `UNDERGROUND` describe the biome's nature, not whether its row happens to be usable in
the current map view. Underground targets remain visible in the catalog and become selectable only
on fixed-height layers supported by the active generation profile.

Before either point or regional biome resolution, WorldFinder calls
`supportsBiomeSampling(context, samplingMode, sampleY)`. Override it when the addon supports
`FIXED_Y`, and reject exact heights outside the addon's real generation range. The additive default
is deliberately safe for existing addons: an old resolver continues to receive Overworld surface
queries, but never silently receives an Overworld underground query that it could misinterpret as
surface. The historical fixed-Y Nether and End behavior is preserved. Returning `false` delegates
to another resolver or the native fallback without reporting an addon failure.

Terrain coverage is a separate optional row-major byte channel. It never changes biome identity:

- unsigned `0` means void;
- unsigned `255` means fully covered terrain at that sample;
- intermediate values may represent partial coverage at a coarse scale.

Every supplied channel also declares a `TerrainCoverageResolution`: `SAMPLE_POINT` describes the
queried block columns, `CHUNK_CENTER` describes exact centre-column observations expanded across
chunks, and `END_ISLAND_TOPOLOGY` is explicitly only a coarse End silhouette. `UNKNOWN` is valid
only when the array is absent. Use the six-argument `BiomeRegion` constructor rather than relying
on the legacy non-null => `SAMPLE_POINT` convention.

If a resolver announces `TERRAIN_COVERAGE`, every handled region must include the complete
coverage array. If the addon does not own terrain generation, omit the capability and construct
the region without that array. In the End, WorldFinder can then retain its native terrain layer
while still using the addon's biome result. A void cell must still return its true biome ID;
never invent a biome such as `example:void` merely to request a black pixel.

Regional work receives a `CancellationToken`. Check it at least once per output row, or after
another small bounded batch, and let `CancellationException` escape. This prevents obsolete tiles
from consuming workers after a pan, zoom, seed change, or dimension change.

## Complete example

This example registers one biome, one structure, and a resolver with regional, point, terrain,
and structure paths. The constant biome fill is intentionally a small stand-in for the addon's
own thread-safe generation engine.

```java
package com.example.crystalcompat;

import fr.asashiin.worldfinder.api.WorldFinderAddon;
import fr.asashiin.worldfinder.api.WorldFinderRegistrar;
import fr.asashiin.worldfinder.api.target.BiomeSearchTarget;
import fr.asashiin.worldfinder.api.target.StructureSearchTarget;
import fr.asashiin.worldfinder.api.world.BiomeQuery;
import fr.asashiin.worldfinder.api.world.BiomeRegion;
import fr.asashiin.worldfinder.api.world.BiomeRegionQuery;
import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.StructureQuery;
import fr.asashiin.worldfinder.api.world.StructureResult;
import fr.asashiin.worldfinder.api.world.TerrainCoverageResolution;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenCapability;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenProfile;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;

import java.util.List;
import java.util.Set;

public final class CrystalWorldFinderAddon implements WorldFinderAddon {
    private static final String CRYSTAL_FIELDS = "crystalworld:crystal_fields";
    private static final String CRYSTAL_SPIRE = "crystalworld:crystal_spire";

    @Override
    public String id() {
        return "crystal_worldfinder_compat";
    }

    @Override
    public String displayName() {
        return "Crystal World - WorldFinder Compat";
    }

    @Override
    public void register(WorldFinderRegistrar registrar) {
        registrar.registerBiome(new BiomeSearchTarget(
                CRYSTAL_FIELDS,
                "Crystal Fields",
                WorldDimension.OVERWORLD,
                BiomeSearchTarget.Layer.SURFACE,
                0xFF8F78D8
        ));
        registrar.registerStructure(new StructureSearchTarget(
                CRYSTAL_SPIRE,
                "Crystal Spire",
                WorldDimension.OVERWORLD
        ));
        registrar.registerResolver(new CrystalResolver());
    }

    private static final class CrystalResolver implements WorldgenResolver {
        @Override
        public String id() {
            return "crystalworld:worldgen";
        }

        @Override
        public int priority() {
            return 100;
        }

        @Override
        public boolean supports(WorldgenContext context) {
            WorldgenProfile profile = context.profile();
            return context.dimension() == WorldDimension.OVERWORLD
                    && profile.id().equals(WorldgenProfile.VANILLA_PROFILE_ID)
                    && profile.worldPresetId().equals(WorldgenProfile.VANILLA_NORMAL_PRESET_ID);
        }

        @Override
        public boolean supportsBiomeSampling(
                WorldgenContext context,
                BiomeSamplingMode samplingMode,
                int sampleY
        ) {
            // This example owns only its true surface projection. An underground-aware addon can
            // opt in to FIXED_Y and validate sampleY against its own vertical generation data.
            return samplingMode == BiomeSamplingMode.SURFACE;
        }

        @Override
        public Set<WorldgenCapability> capabilities() {
            return WorldgenResolver.capabilities(
                    WorldgenCapability.REGION_BIOMES,
                    WorldgenCapability.POINT_BIOMES,
                    WorldgenCapability.TERRAIN_COVERAGE,
                    WorldgenCapability.STRUCTURES
            );
        }

        @Override
        public ResolverResult<BiomeRegion> resolveBiomeRegion(BiomeRegionQuery query) {
            if (query.samplingMode() != BiomeSamplingMode.SURFACE) {
                return ResolverResult.unhandled();
            }
            int[] biomeIndices = new int[query.sampleCount()];
            byte[] terrainCoverage = new byte[query.sampleCount()];

            for (int z = 0; z < query.height(); z++) {
                query.cancellationToken().throwIfCancellationRequested();
                int row = z * query.width();
                for (int x = 0; x < query.width(); x++) {
                    // Replace this constant result with the addon's own regional engine.
                    biomeIndices[row + x] = 0;
                    terrainCoverage[row + x] = (byte) 0xFF;
                }
            }

            return ResolverResult.handled(new BiomeRegion(
                    query.width(),
                    query.height(),
                    List.of(CRYSTAL_FIELDS),
                    biomeIndices,
                    terrainCoverage,
                    TerrainCoverageResolution.SAMPLE_POINT
            ));
        }

        @Override
        public ResolverResult<String> resolveBiome(BiomeQuery query) {
            return ResolverResult.handled(CRYSTAL_FIELDS);
        }

        @Override
        public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
            if (!query.targetId().equals(CRYSTAL_SPIRE)) {
                return ResolverResult.unhandled();
            }

            StructureResult spawnSpire = new StructureResult(
                    CRYSTAL_SPIRE, "Crystal Spire", 0, 96, 0
            );
            List<StructureResult> matches = query.contains(0, 0)
                    ? List.of(spawnSpire)
                    : List.of();
            return ResolverResult.handled(matches);
        }
    }
}
```

The example imports no loader or Minecraft class. A real compatibility addon can compile against
its target mod separately, translate target-mod results to namespaced IDs, and keep all unstable
types out of the WorldFinder-facing signatures. The repository verifies the published Maven
metadata and resolves the API and Core as external consumers would.

## Threading, validation, and failure isolation

Map and structure queries run on background workers. The same resolver may be invoked
concurrently for multiple tiles, dimensions, or seeds. Resolver implementations must therefore
be thread-safe, must not touch client rendering state, and must not block on the render thread.
Prefer immutable engine state and seed/profile-scoped caches with explicit bounds.

WorldFinder validates addon IDs, resolver IDs, palette entries, region dimensions, declared
coverage, structure target IDs, and structure bounds. Ordinary failures are reported with the
addon or resolver ID and the operation that failed; the chain then continues to the next resolver
or vanilla fallback. Cancellation, thread interruption, and fatal JVM errors are not normal addon
failures and must not be caught or converted into an unhandled result.

Resolvers should return immutable values and never mutate a query. WorldFinder defensively copies
regional buffers, but that is a boundary guarantee rather than permission to reuse mutable output
while a query is running.

`StructureResult.blockY()` is optional. Supply it whenever the engine knows the meaningful
vertical marker position, especially for underground structures; otherwise WorldFinder must use a
surface-oriented action height. Results with the same X/Z but distinct known Y values remain
distinct markers.

## Optional waypoint integrations

Map integrations initialize independently from world-generation addons. Register a
loader-neutral `WaypointProvider` through `WorldFinderWaypoints.registerOwned(provider)` and keep
the returned `WaypointRegistration` handle. Close that handle when the integration unloads; it
removes only the exact provider that owns it. Provider IDs are namespaced and duplicates are
rejected. ID and display name are validated and snapshotted once at registration, so callbacks
cannot corrupt menu metadata later.

`canCreateWaypoint()` receives the same immutable `WaypointRequest` while WorldFinder constructs the
location menu and again immediately before creation. Return `false` for an unsupported dimension or
unavailable client session; the accepting default keeps providers compiled against the initial 0.2
contract compatible. `createWaypoint()` receives an immutable `WaypointRequest` only after that
check. Both callbacks run synchronously on the client UI thread and must return promptly.

`hasShareRecipients()` confirms a compatible recipient. WorldFinder may call that probe concurrently
from a background worker, so it must be thread-safe, fast, and non-blocking; its advisory result is
cached and refreshed asynchronously. `shareWaypoint()` runs synchronously on the client UI thread
and must also return promptly. Callback failures are isolated. `shareWaypoint()` should return
`false` if recipient state changed. These generic sharing callbacks remain available to addon
providers; WorldFinder's built-in JourneyMap adapter does not implement them because JourneyMap's
public addon API does not expose its native sharing flow.

WorldFinder's JourneyMap, Xaero, and FTB Chunks adapters remain optional platform code and are not
bundled with the API artifact or either loader JAR. JourneyMap and FTB Chunks create persistent
waypoints. Xaero uses only its official third-party waypoint API, so its markers are scoped to the
active dimension and current Xaero session; Xaero World Map alone has no creation backend.
