package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.WorldFinderApi;
import fr.asashiin.worldfinder.api.target.NamespacedId;
import fr.asashiin.worldfinder.api.target.WorldFinderStructureTargets;
import fr.asashiin.worldfinder.api.waypoint.WaypointProvider;
import fr.asashiin.worldfinder.api.waypoint.WaypointRequest;
import fr.asashiin.worldfinder.api.waypoint.WaypointRegistration;
import fr.asashiin.worldfinder.api.waypoint.WorldFinderWaypoints;
import fr.asashiin.worldfinder.api.world.BiomeQuery;
import fr.asashiin.worldfinder.api.world.BiomeRegion;
import fr.asashiin.worldfinder.api.world.BiomeRegionQuery;
import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import fr.asashiin.worldfinder.api.world.CancellationToken;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.StructureResult;
import fr.asashiin.worldfinder.api.world.TerrainCoverageResolution;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenProfile;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiContractTest {
    @Test
    void validatesNamespacedIdsWithoutNormalization() {
        assertTrue(NamespacedId.isValid("worldfinder:path/to.biome-name"));
        assertFalse(NamespacedId.isValid(null));
        assertFalse(NamespacedId.isValid("minecraft"));
        assertFalse(NamespacedId.isValid(":plains"));
        assertFalse(NamespacedId.isValid("Minecraft:plains"));
        assertFalse(NamespacedId.isValid("minecraft:bad value"));
        assertFalse(NamespacedId.isValid("minecraft:plains:extra"));
        assertThrows(IllegalArgumentException.class, () -> NamespacedId.requireValid("MINECRAFT:plains"));
    }

    @Test
    void contextCarriesVersionedProfileAndDeterministicImmutableProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("z", "last");
        properties.put("a", "first");
        WorldgenProfile profile = new WorldgenProfile(
                "terralith:default",
                "1.21.1",
                "minecraft:normal",
                properties
        );
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("server", "local");
        WorldgenContext context = new WorldgenContext(
                42L,
                WorldDimension.OVERWORLD,
                profile,
                true,
                attributes
        );

        properties.put("later", "ignored");
        attributes.put("later", "ignored");

        assertEquals("1.21.1", context.minecraftVersion());
        assertEquals("terralith:default", context.profileId());
        assertEquals("minecraft:normal", context.worldPresetId());
        assertEquals(List.of("a", "z"), new ArrayList<>(profile.properties().keySet()));
        assertEquals(Map.of("server", "local"), context.attributes());
        assertThrows(UnsupportedOperationException.class,
                () -> context.attributes().put("invalid", "mutation"));
    }

    @Test
    void legacyContextHasAnExplicitUnspecifiedProfile() {
        WorldgenContext context = new WorldgenContext(
                7L,
                WorldDimension.END,
                "minecraft:normal",
                false,
                Map.of()
        );

        assertEquals(WorldgenProfile.UNKNOWN_MINECRAFT_VERSION, context.minecraftVersion());
        assertEquals(WorldgenProfile.UNSPECIFIED_PROFILE_ID, context.profileId());
    }

    @Test
    void regionalResultIsPaletteCompressedImmutableAndKeepsCoverageSeparate() {
        int[] indices = {0, 1, 1, 0};
        byte[] coverage = {0, (byte) 255, 64, (byte) 128};
        BiomeRegion region = new BiomeRegion(
                2,
                2,
                List.of("minecraft:the_end", "minecraft:end_highlands"),
                indices,
                coverage
        );
        indices[0] = 1;
        coverage[0] = 100;

        assertEquals("minecraft:the_end", region.biomeIdAt(0, 0));
        assertEquals("minecraft:end_highlands", region.biomeIdAt(1, 0));
        assertEquals(0, region.terrainCoverageAt(0, 0));
        assertEquals(255, region.terrainCoverageAt(1, 0));
        assertEquals(TerrainCoverageResolution.SAMPLE_POINT, region.terrainCoverageResolution());
        assertArrayEquals(new int[]{0, 1, 1, 0}, region.copyBiomeIndices());
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeRegion(1, 1, List.of("minecraft:the_end"), new int[]{1}));
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeRegion(1, 1,
                        List.of("minecraft:the_end", "minecraft:the_end"), new int[]{0}));

        BiomeRegion coarseRegion = new BiomeRegion(
                1,
                1,
                List.of("minecraft:the_end"),
                new int[]{0},
                new byte[]{64},
                TerrainCoverageResolution.END_ISLAND_TOPOLOGY
        );
        assertEquals(TerrainCoverageResolution.END_ISLAND_TOPOLOGY,
                coarseRegion.terrainCoverageResolution());
        assertEquals(TerrainCoverageResolution.UNKNOWN,
                new BiomeRegion(1, 1, List.of("minecraft:the_end"), new int[]{0})
                        .terrainCoverageResolution());
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeRegion(1, 1, List.of("minecraft:the_end"), new int[]{0},
                        null, TerrainCoverageResolution.SAMPLE_POINT));
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeRegion(1, 1, List.of("minecraft:the_end"), new int[]{0},
                        new byte[]{0}, TerrainCoverageResolution.UNKNOWN));
        assertThrows(NullPointerException.class,
                () -> new BiomeRegion(1, 1, List.of("minecraft:the_end"), new int[]{0},
                        new byte[]{0}, null));
    }

    @Test
    void regionQueryMapsSamplesAndRejectsCoordinateOverflow() {
        WorldgenContext context = context();
        BiomeRegionQuery query = new BiomeRegionQuery(context, -16, 32, 3, 2, 4, 64);

        assertEquals(6, query.sampleCount());
        assertEquals(BiomeSamplingMode.FIXED_Y, query.samplingMode());
        assertEquals(-8, query.blockX(2));
        assertEquals(36, query.blockZ(1));
        assertThrows(IndexOutOfBoundsException.class, () -> query.blockX(3));
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeRegionQuery(context, Integer.MAX_VALUE, 0, 2, 1, 1, 64));
    }

    @Test
    void biomeQueriesDeclareFixedYOrOverworldSurfaceProjection() {
        WorldgenContext overworld = new WorldgenContext(
                42L,
                WorldDimension.OVERWORLD,
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"),
                false,
                Map.of()
        );
        BiomeQuery legacyPoint = new BiomeQuery(overworld, 1, 64, 2);
        BiomeQuery surfacePoint = new BiomeQuery(
                overworld, 1, 64, 2, BiomeSamplingMode.SURFACE);
        BiomeRegionQuery surfaceRegion = new BiomeRegionQuery(
                overworld, 0, 0, 2, 2, 4, 64,
                BiomeSamplingMode.SURFACE, CancellationToken.NONE);

        assertEquals(BiomeSamplingMode.FIXED_Y, legacyPoint.samplingMode());
        assertEquals(BiomeSamplingMode.SURFACE, surfacePoint.samplingMode());
        assertEquals(BiomeSamplingMode.SURFACE, surfaceRegion.samplingMode());
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeQuery(context(), 1, 64, 2, BiomeSamplingMode.SURFACE));
        assertThrows(IllegalArgumentException.class,
                () -> new BiomeRegionQuery(
                        context(), 0, 0, 1, 1, 4, 64,
                        BiomeSamplingMode.SURFACE, CancellationToken.NONE));
        assertThrows(NullPointerException.class,
                () -> new BiomeQuery(overworld, 1, 64, 2, null));
    }

    @Test
    void legacyResolverDefaultsRejectOnlyNewOverworldFixedHeightRequests() {
        WorldgenResolver legacy = new WorldgenResolver() {
            @Override public String id() { return "test:legacy"; }
            @Override public boolean supports(WorldgenContext context) { return true; }
        };
        WorldgenContext overworld = new WorldgenContext(
                42L, WorldDimension.OVERWORLD,
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"), false, Map.of());

        assertTrue(legacy.supportsBiomeSampling(overworld, BiomeSamplingMode.SURFACE, 64));
        assertFalse(legacy.supportsBiomeSampling(overworld, BiomeSamplingMode.FIXED_Y, -32));
        assertTrue(legacy.supportsBiomeSampling(context(), BiomeSamplingMode.FIXED_Y, 64));
    }

    @Test
    void regionQueryCarriesCooperativeCancellation() {
        AtomicBoolean cancelled = new AtomicBoolean();
        CancellationToken token = cancelled::get;
        BiomeRegionQuery query = new BiomeRegionQuery(
                context(), 0, 0, 1, 1, 4, 64, token
        );

        assertEquals(BiomeSamplingMode.FIXED_Y, query.samplingMode());
        query.cancellationToken().throwIfCancellationRequested();
        cancelled.set(true);

        assertThrows(CancellationException.class,
                query.cancellationToken()::throwIfCancellationRequested);
    }

    @Test
    void resolverResultDistinguishesHandledEmptyFromUnhandled() {
        ResolverResult<List<String>> empty = ResolverResult.handled(List.of());
        ResolverResult<List<String>> unhandled = ResolverResult.unhandled();

        assertTrue(empty.handled());
        assertEquals(List.of(), empty.value());
        assertFalse(unhandled.handled());
        assertEquals(ResolverResult.unhandled(), unhandled);
        assertEquals(ResolverResult.handled(List.of()), empty);
        assertThrows(NoSuchElementException.class, unhandled::value);
    }

    @Test
    void structureResultKeepsOptionalVerticalPositionAndLegacyConstruction() {
        StructureResult legacy = new StructureResult("example:ruin", "Ruin", 12, -30);
        Map<String, String> mutableAttributes = new LinkedHashMap<>();
        mutableAttributes.put("variant", "tower");
        StructureResult positioned = new StructureResult(
                "example:ruin", "Ruin", 12, 87, -30, mutableAttributes);
        mutableAttributes.put("late", "ignored");

        assertNull(legacy.blockY());
        assertEquals(Integer.valueOf(87), positioned.blockY());
        assertEquals(Map.of("variant", "tower"), positioned.attributes());
        assertThrows(UnsupportedOperationException.class,
                () -> positioned.attributes().put("invalid", "mutation"));
    }

    @Test
    void apiVersionIsAvailableThroughRuntimeMethod() {
        assertEquals(2, WorldFinderApi.apiVersion());
    }

    @Test
    void publicBuiltinTargetCatalogIsNamespacedAndComplete() {
        assertEquals(18, WorldFinderStructureTargets.Overworld.ALL.size());
        assertEquals(9, WorldFinderStructureTargets.Nether.ALL.size());
        assertEquals(4, WorldFinderStructureTargets.End.ALL.size());
        assertEquals(31, WorldFinderStructureTargets.ALL.size());
        assertTrue(WorldFinderStructureTargets.ALL.stream().allMatch(NamespacedId::isValid));
        assertEquals(WorldgenProfile.VANILLA_PROFILE_ID,
                WorldgenProfile.vanilla("26.2", WorldgenProfile.VANILLA_NORMAL_PRESET_ID).id());
    }

    @Test
    void legacyWaypointProvidersAcceptEveryDestinationByDefault() {
        WaypointProvider provider = provider("test:legacy_waypoint", "Legacy waypoint provider");
        WaypointRequest request = new WaypointRequest(
                "Village", WorldDimension.OVERWORLD, 12, 64, -30, 0xFF55AAFF);

        assertTrue(provider.canCreateWaypoint(request));
    }

    @Test
    void waypointRegistryRejectsDuplicateProvidersWithoutReplacingTheFirst() {
        String id = "test:waypoint_" + System.nanoTime();
        WaypointProvider first = provider(id, "First");
        WaypointProvider duplicate = provider(id, "Duplicate");
        WaypointRegistration registration = null;
        try {
            registration = WorldFinderWaypoints.registerOwned(first);

            var descriptor = WorldFinderWaypoints.descriptors().stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst().orElseThrow();
            assertEquals("First", descriptor.displayName());
            assertSame(first, descriptor.provider());
            assertTrue(WorldFinderWaypoints.isActive(descriptor));
            assertThrows(IllegalArgumentException.class,
                    () -> WorldFinderWaypoints.registerOwned(duplicate));
            assertSame(first, WorldFinderWaypoints.provider(id).orElseThrow());
        } finally {
            if (registration != null) {
                registration.close();
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void staleWaypointHandleCannotRemoveANewerProvider() {
        String id = "test:waypoint_owner_" + System.nanoTime();
        WaypointProvider first = provider(id, "First");
        WaypointProvider second = provider(id, "Second");
        WaypointRegistration firstHandle = WorldFinderWaypoints.registerOwned(first);
        WaypointRegistration secondHandle = null;
        try {
            var staleDescriptor = WorldFinderWaypoints.descriptors().stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst().orElseThrow();
            WorldFinderWaypoints.unregisterProvider(id);
            secondHandle = WorldFinderWaypoints.registerOwned(second);

            firstHandle.close();

            assertSame(second, WorldFinderWaypoints.provider(id).orElseThrow());
            assertFalse(WorldFinderWaypoints.isActive(staleDescriptor));
        } finally {
            firstHandle.close();
            if (secondHandle != null) {
                secondHandle.close();
            }
        }
    }

    @Test
    void waypointMetadataCallbacksDoNotHoldTheRegistryMonitor() throws InterruptedException {
        String id = "test:waypoint_nonblocking_" + System.nanoTime();
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        AtomicReference<WaypointRegistration> registration = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        WaypointProvider provider = new WaypointProvider() {
            @Override
            public String id() {
                callbackEntered.countDown();
                try {
                    if (!releaseCallback.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test callback was not released");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return id;
            }

            @Override
            public String displayName() {
                return "Non-blocking registration";
            }

            @Override
            public boolean createWaypoint(WaypointRequest request) {
                return true;
            }
        };
        Thread registeringThread = new Thread(() -> {
            try {
                registration.set(WorldFinderWaypoints.registerOwned(provider));
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        }, "WorldFinder waypoint registry test");
        registeringThread.start();

        assertTrue(callbackEntered.await(1, TimeUnit.SECONDS));
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(1), WorldFinderWaypoints::descriptors);
        } finally {
            releaseCallback.countDown();
            registeringThread.join(2_000L);
            WaypointRegistration owned = registration.get();
            if (owned != null) owned.close();
        }
        assertFalse(registeringThread.isAlive());
        assertNull(failure.get());
    }

    private static WaypointProvider provider(String id, String name) {
        return new WaypointProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String displayName() {
                return name;
            }

            @Override
            public boolean createWaypoint(WaypointRequest request) {
                return true;
            }
        };
    }

    static WorldgenContext context() {
        return new WorldgenContext(
                42L,
                WorldDimension.END,
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"),
                true,
                Map.of()
        );
    }
}
