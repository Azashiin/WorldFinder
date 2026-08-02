package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.world.BiomeQuery;
import fr.asashiin.worldfinder.api.world.BiomeRegion;
import fr.asashiin.worldfinder.api.world.BiomeRegionQuery;
import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.TerrainCoverageResolution;
import fr.asashiin.worldfinder.api.world.WorldgenCapability;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonBiomeResolverChainTest {
    @Test
    void legacySurfaceAddonNeverReceivesAnOverworldFixedHeightRequest() {
        WorldgenContext context = new WorldgenContext(
                42L, WorldDimension.OVERWORLD,
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"), false, Map.of());
        AtomicInteger legacyCalls = new AtomicInteger();
        AtomicInteger undergroundCalls = new AtomicInteger();
        WorldgenResolver legacySurface = new WorldgenResolver() {
            @Override public String id() { return "test:legacy_surface"; }
            @Override public boolean supports(WorldgenContext candidate) { return true; }
            @Override public ResolverResult<String> resolveBiome(BiomeQuery query) {
                legacyCalls.incrementAndGet();
                return ResolverResult.handled("minecraft:plains");
            }
        };
        WorldgenResolver underground = new WorldgenResolver() {
            @Override public String id() { return "test:underground"; }
            @Override public boolean supports(WorldgenContext candidate) { return true; }
            @Override public boolean supportsBiomeSampling(
                    WorldgenContext candidate, BiomeSamplingMode mode, int sampleY) {
                return mode == BiomeSamplingMode.FIXED_Y && sampleY <= 0;
            }
            @Override public ResolverResult<String> resolveBiome(BiomeQuery query) {
                undergroundCalls.incrementAndGet();
                return ResolverResult.handled("minecraft:lush_caves");
            }
        };

        ResolverResult<String> result = new AddonBiomeResolverChain(
                List.of(legacySurface, underground)).resolveBiome(
                new BiomeQuery(context, 0, -32, 0, BiomeSamplingMode.FIXED_Y));

        assertTrue(result.handled());
        assertEquals("minecraft:lush_caves", result.value());
        assertEquals(0, legacyCalls.get());
        assertEquals(1, undergroundCalls.get());
    }

    @Test
    void cancellationDoesNotQuarantinePointResolverOrEmitADiagnostic() {
        AtomicInteger calls = new AtomicInteger();
        List<AddonIssue> issues = new ArrayList<>();
        WorldgenContext context = ApiContractTest.context();
        WorldgenResolver resolver = new WorldgenResolver() {
            @Override public String id() { return "test:cancelled_point"; }
            @Override public boolean supports(WorldgenContext candidate) { return true; }
            @Override public ResolverResult<String> resolveBiome(BiomeQuery query) {
                if (calls.incrementAndGet() == 1) throw new CancellationException("cancelled tile");
                return ResolverResult.handled("minecraft:plains");
            }
        };
        AddonBiomeResolverChain.TileSession session = new AddonBiomeResolverChain(
                List.of(resolver), issues::add).openTileSession(context);

        assertThrows(CancellationException.class,
                () -> session.resolveBiome(new BiomeQuery(context, 0, 64, 0)));
        ResolverResult<String> retry = session.resolveBiome(
                new BiomeQuery(context, 4, 64, 0));

        assertTrue(retry.handled());
        assertEquals("minecraft:plains", retry.value());
        assertEquals(2, calls.get());
        assertTrue(issues.isEmpty());
    }

    @Test
    void tileSessionChecksSupportOnceAndQuarantinesBrokenPointResolver() {
        List<AddonIssue> issues = new ArrayList<>();
        AtomicInteger brokenSupportCalls = new AtomicInteger();
        AtomicInteger brokenPointCalls = new AtomicInteger();
        AtomicInteger ownerSupportCalls = new AtomicInteger();
        AtomicInteger ownerPointCalls = new AtomicInteger();
        WorldgenContext context = ApiContractTest.context();
        WorldgenResolver broken = new WorldgenResolver() {
            @Override
            public String id() {
                return "test:broken";
            }

            @Override
            public boolean supports(WorldgenContext candidate) {
                brokenSupportCalls.incrementAndGet();
                return candidate.equals(context);
            }

            @Override
            public ResolverResult<String> resolveBiome(BiomeQuery query) {
                brokenPointCalls.incrementAndGet();
                throw new IllegalStateException("broken point engine");
            }
        };
        WorldgenResolver owner = new WorldgenResolver() {
            @Override
            public String id() {
                return "test:owner";
            }

            @Override
            public boolean supports(WorldgenContext candidate) {
                ownerSupportCalls.incrementAndGet();
                return candidate.equals(context);
            }

            @Override
            public ResolverResult<String> resolveBiome(BiomeQuery query) {
                ownerPointCalls.incrementAndGet();
                return ResolverResult.handled("minecraft:plains");
            }
        };
        AddonBiomeResolverChain.TileSession session = new AddonBiomeResolverChain(
                List.of(broken, owner), issues::add
        ).openTileSession(context);

        ResolverResult<BiomeRegion> region = session.resolveBiomeRegion(
                new BiomeRegionQuery(context, 0, 0, 2, 2, 4, 64));
        for (int index = 0; index < 4; index++) {
            ResolverResult<String> point = session.resolveBiome(
                    new BiomeQuery(context, index * 4, 64, 0));
            assertTrue(point.handled());
            assertEquals("minecraft:plains", point.value());
        }

        assertFalse(region.handled());
        assertEquals(1, brokenSupportCalls.get());
        assertEquals(1, ownerSupportCalls.get());
        assertEquals(1, brokenPointCalls.get());
        assertEquals(4, ownerPointCalls.get());
        assertEquals(1, issues.size());
        assertEquals("test:broken", issues.getFirst().sourceId());
        assertEquals(AddonIssue.Operation.BIOME_QUERY, issues.getFirst().operation());
    }

    @Test
    void usesFirstValidHandledPointResultAndReportsBrokenResolvers() {
        List<AddonIssue> issues = new ArrayList<>();
        WorldgenResolver broken = pointResolver("test:broken", () -> {
            throw new LinkageError("missing optional dependency");
        });
        WorldgenResolver invalid = pointResolver("test:invalid",
                () -> ResolverResult.handled("INVALID"));
        WorldgenResolver owner = pointResolver("test:owner",
                () -> ResolverResult.handled("minecraft:end_highlands"));
        WorldgenResolver ignored = pointResolver("test:ignored",
                () -> ResolverResult.handled("minecraft:end_barrens"));

        ResolverResult<String> result = new AddonBiomeResolverChain(
                List.of(broken, invalid, owner, ignored), issues::add
        ).resolveBiome(new BiomeQuery(ApiContractTest.context(), 0, 64, 0));

        assertTrue(result.handled());
        assertEquals("minecraft:end_highlands", result.value());
        assertEquals(List.of(
                        AddonIssue.Operation.BIOME_QUERY,
                        AddonIssue.Operation.BIOME_QUERY
                ),
                issues.stream().map(AddonIssue::operation).toList());
    }

    @Test
    void regionalResolverCanReturnBiomesAndTerrainCoverageInOneBatch() {
        WorldgenContext context = ApiContractTest.context();
        BiomeRegionQuery query = new BiomeRegionQuery(context, -8, -8, 2, 2, 8, 64);
        BiomeRegion region = new BiomeRegion(
                2,
                2,
                List.of("minecraft:the_end"),
                new int[]{0, 0, 0, 0},
                new byte[]{0, 10, (byte) 200, (byte) 255},
                TerrainCoverageResolution.CHUNK_CENTER
        );
        WorldgenResolver resolver = new WorldgenResolver() {
            @Override
            public String id() {
                return "test:regional";
            }

            @Override
            public boolean supports(WorldgenContext candidate) {
                return candidate.equals(context);
            }

            @Override
            public Set<WorldgenCapability> capabilities() {
                return WorldgenResolver.capabilities(
                        WorldgenCapability.REGION_BIOMES,
                        WorldgenCapability.TERRAIN_COVERAGE
                );
            }

            @Override
            public ResolverResult<BiomeRegion> resolveBiomeRegion(BiomeRegionQuery candidate) {
                return ResolverResult.handled(region);
            }
        };

        ResolverResult<BiomeRegion> result = new AddonBiomeResolverChain(List.of(resolver))
                .resolveBiomeRegion(query);

        assertTrue(result.handled());
        assertEquals(200, result.value().terrainCoverageAt(0, 1));
        assertEquals(TerrainCoverageResolution.CHUNK_CENTER,
                result.value().terrainCoverageResolution());
    }

    @Test
    void invalidRegionalResultFallsThroughAndIsReported() {
        List<AddonIssue> issues = new ArrayList<>();
        WorldgenResolver wrongSize = regionalResolver(
                "test:wrong_size",
                Set.of(WorldgenCapability.REGION_BIOMES),
                new BiomeRegion(1, 1, List.of("minecraft:the_end"), new int[]{0})
        );
        WorldgenResolver missingCoverage = regionalResolver(
                "test:missing_coverage",
                Set.of(WorldgenCapability.REGION_BIOMES, WorldgenCapability.TERRAIN_COVERAGE),
                new BiomeRegion(2, 2, List.of("minecraft:the_end"), new int[]{0, 0, 0, 0})
        );
        BiomeRegionQuery query = new BiomeRegionQuery(ApiContractTest.context(), 0, 0, 2, 2, 4, 64);

        ResolverResult<BiomeRegion> result = new AddonBiomeResolverChain(
                List.of(wrongSize, missingCoverage), issues::add
        ).resolveBiomeRegion(query);

        assertFalse(result.handled());
        assertEquals(2, issues.size());
        assertTrue(issues.stream().allMatch(
                issue -> issue.operation() == AddonIssue.Operation.BIOME_REGION_QUERY
        ));
    }

    private static WorldgenResolver pointResolver(
            String id,
            java.util.function.Supplier<ResolverResult<String>> result
    ) {
        return new WorldgenResolver() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public boolean supports(WorldgenContext context) {
                return true;
            }

            @Override
            public ResolverResult<String> resolveBiome(BiomeQuery query) {
                return result.get();
            }
        };
    }

    private static WorldgenResolver regionalResolver(
            String id,
            Set<WorldgenCapability> capabilities,
            BiomeRegion region
    ) {
        return new WorldgenResolver() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public boolean supports(WorldgenContext context) {
                return true;
            }

            @Override
            public Set<WorldgenCapability> capabilities() {
                return capabilities;
            }

            @Override
            public ResolverResult<BiomeRegion> resolveBiomeRegion(BiomeRegionQuery query) {
                return ResolverResult.handled(region);
            }
        };
    }
}
