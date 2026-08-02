package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.world.CancellationToken;
import fr.asashiin.worldfinder.api.world.StructureQuery;
import fr.asashiin.worldfinder.api.world.StructureResult;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonStructureResolverChainTest {
    @Test
    void scanSessionChecksSupportOnceAndQuarantinesBrokenResolverAcrossTargets() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        AtomicInteger brokenSupportCalls = new AtomicInteger();
        AtomicInteger brokenStructureCalls = new AtomicInteger();
        AtomicInteger ownerSupportCalls = new AtomicInteger();
        AtomicInteger ownerStructureCalls = new AtomicInteger();
        List<AddonIssue> issues = new ArrayList<>();
        WorldgenResolver broken = new WorldgenResolver() {
            @Override public String id() { return "test:broken_session"; }
            @Override public boolean supports(WorldgenContext candidate) {
                brokenSupportCalls.incrementAndGet();
                return candidate.equals(context);
            }
            @Override public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                brokenStructureCalls.incrementAndGet();
                throw new IllegalStateException("broken structure engine");
            }
        };
        WorldgenResolver owner = new WorldgenResolver() {
            @Override public String id() { return "test:owner_session"; }
            @Override public boolean supports(WorldgenContext candidate) {
                ownerSupportCalls.incrementAndGet();
                return candidate.equals(context);
            }
            @Override public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                ownerStructureCalls.incrementAndGet();
                return ResolverResult.handled(List.of(new StructureResult(
                        query.targetId(), query.targetId(), 0, 0)));
            }
        };
        AddonStructureResolverChain.ScanSession session = new AddonStructureResolverChain(
                List.of(broken, owner), issues::add).openScanSession(context);

        for (String target : List.of("example:first", "example:second", "example:third")) {
            ResolverResult<List<StructureResult>> result = session.resolveStructures(
                    new StructureQuery(context, target, -10, 10, -10, 10));
            assertTrue(result.handled());
            assertEquals(target, result.value().getFirst().structureId());
        }

        assertEquals(1, brokenSupportCalls.get());
        assertEquals(1, ownerSupportCalls.get());
        assertEquals(1, brokenStructureCalls.get());
        assertEquals(3, ownerStructureCalls.get());
        assertEquals(1, issues.size());
        assertEquals("test:broken_session", issues.getFirst().sourceId());
        assertEquals(AddonIssue.Operation.STRUCTURE_QUERY, issues.getFirst().operation());
        assertTrue(session.failureObserved());
    }

    @Test
    void supportFailureIsExposedWithoutBlockingHealthyResolvers() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        List<AddonIssue> issues = new ArrayList<>();
        WorldgenResolver brokenSupport = new WorldgenResolver() {
            @Override public String id() { return "test:broken_support"; }
            @Override public boolean supports(WorldgenContext ignored) {
                throw new IllegalStateException("broken support predicate");
            }
        };
        WorldgenResolver owner = resolver(true, List.of(
                new StructureResult("example:ruin", "Healthy result", 2, 3)));
        AddonStructureResolverChain.ScanSession session = new AddonStructureResolverChain(
                List.of(brokenSupport, owner), issues::add).openScanSession(context);

        ResolverResult<List<StructureResult>> result = session.resolveStructures(
                new StructureQuery(context, "example:ruin", -10, 10, -10, 10));

        assertTrue(result.handled());
        assertEquals(1, result.value().size());
        assertTrue(session.failureObserved());
        assertEquals(List.of(AddonIssue.Operation.SUPPORT_CHECK),
                issues.stream().map(AddonIssue::operation).toList());
    }

    @Test
    void legacyQueryConstructorUsesTheNonCancellableToken() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());

        StructureQuery query = new StructureQuery(context, "example:ruin", -10, 10, -10, 10);

        assertSame(CancellationToken.NONE, query.cancellationToken());
    }

    @Test
    void preCancelledQueryDoesNotInvokeAnAddonResolver() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        AtomicInteger supportChecks = new AtomicInteger();
        WorldgenResolver resolver = new WorldgenResolver() {
            @Override public String id() { return "test:cancellation"; }
            @Override public boolean supports(WorldgenContext ignored) {
                supportChecks.incrementAndGet();
                return true;
            }
        };
        StructureQuery query = new StructureQuery(
                context, "example:ruin", -10, 10, -10, 10, () -> true);

        assertThrows(CancellationException.class,
                () -> new AddonStructureResolverChain(List.of(resolver)).resolveStructures(query));
        assertEquals(0, supportChecks.get());
    }

    @Test
    void cancellationRequestedByAResolverStopsPriorityFallback() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        AtomicBoolean cancellationRequested = new AtomicBoolean();
        AtomicInteger fallbackCalls = new AtomicInteger();
        WorldgenResolver cancelling = new WorldgenResolver() {
            @Override public String id() { return "test:cancelling"; }
            @Override public boolean supports(WorldgenContext ignored) { return true; }
            @Override public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                cancellationRequested.set(true);
                return ResolverResult.unhandled();
            }
        };
        WorldgenResolver fallback = new WorldgenResolver() {
            @Override public String id() { return "test:fallback"; }
            @Override public boolean supports(WorldgenContext ignored) { return true; }
            @Override public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                fallbackCalls.incrementAndGet();
                return ResolverResult.handled(List.of());
            }
        };
        StructureQuery query = new StructureQuery(
                context, "example:ruin", -10, 10, -10, 10, cancellationRequested::get);

        assertThrows(CancellationException.class, () -> new AddonStructureResolverChain(
                List.of(cancelling, fallback)).resolveStructures(query));
        assertEquals(0, fallbackCalls.get());
    }

    @Test
    void cancellationDoesNotQuarantineResolverOrEmitADiagnostic() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        AtomicBoolean cancellationRequested = new AtomicBoolean();
        AtomicInteger resolverCalls = new AtomicInteger();
        List<AddonIssue> issues = new ArrayList<>();
        WorldgenResolver resolver = new WorldgenResolver() {
            @Override public String id() { return "test:cancellation_session"; }
            @Override public boolean supports(WorldgenContext ignored) { return true; }
            @Override public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                if (resolverCalls.incrementAndGet() == 1) {
                    cancellationRequested.set(true);
                    return ResolverResult.unhandled();
                }
                return ResolverResult.handled(List.of());
            }
        };
        AddonStructureResolverChain.ScanSession session = new AddonStructureResolverChain(
                List.of(resolver), issues::add).openScanSession(context);

        assertThrows(CancellationException.class, () -> session.resolveStructures(
                new StructureQuery(context, "example:first", -10, 10, -10, 10,
                        cancellationRequested::get)));
        cancellationRequested.set(false);
        ResolverResult<List<StructureResult>> retry = session.resolveStructures(
                new StructureQuery(context, "example:second", -10, 10, -10, 10));

        assertTrue(retry.handled());
        assertEquals(2, resolverCalls.get());
        assertTrue(issues.isEmpty());
        assertFalse(session.failureObserved());
    }

    @Test
    void combinesValidUniqueMarkersFromSupportingResolvers() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of("terralith", "2.5"));
        StructureQuery query = new StructureQuery(context, "terralith:skylands_ruin", -100, 100, -100, 100);
        WorldgenResolver primary = resolver(true, List.of(
                new StructureResult("terralith:skylands_ruin", "Skylands Ruin", 12, 24),
                new StructureResult("terralith:skylands_ruin", "Outside", 200, 24),
                new StructureResult("example:wrong", "Wrong target", 10, 10)
        ));
        WorldgenResolver secondary = resolver(true, List.of(
                new StructureResult("terralith:skylands_ruin", "Duplicate", 12, 24),
                new StructureResult("terralith:skylands_ruin", "Skylands Ruin", -30, 8)
        ));
        WorldgenResolver unsupported = resolver(false, List.of(
                new StructureResult("terralith:skylands_ruin", "Unsupported", 1, 1)
        ));
        WorldgenResolver broken = new WorldgenResolver() {
            @Override public String id() { return "test:broken"; }
            @Override public boolean supports(WorldgenContext context) { return true; }
            @Override public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                throw new IllegalStateException("Simulated compatibility failure");
            }
        };

        List<StructureResult> results = new AddonStructureResolverChain(
                List.of(broken, primary, secondary, unsupported)).structuresIn(query);

        assertEquals(List.of("12:24", "-30:8"), results.stream()
                .map(result -> result.blockX() + ":" + result.blockZ()).toList());
    }

    @Test
    void explicitHandledEmptyListClaimsTheQuery() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        StructureQuery query = new StructureQuery(context, "example:ruin", -10, 10, -10, 10);
        WorldgenResolver owner = resolver(true, List.of());
        WorldgenResolver lowerPriority = resolver(true, List.of(
                new StructureResult("example:ruin", "Should not leak", 1, 1)
        ));

        ResolverResult<List<StructureResult>> result = new AddonStructureResolverChain(
                List.of(owner, lowerPriority)
        ).resolveStructures(query);

        assertTrue(result.handled());
        assertEquals(List.of(), result.value());
    }

    @Test
    void verticalCoordinateIsPreservedAndParticipatesInDeduplication() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        StructureQuery query = new StructureQuery(context, "example:ruin", -10, 10, -10, 10);
        WorldgenResolver resolver = resolver(true, List.of(
                new StructureResult("example:ruin", "Lower", 2, 40, 3),
                new StructureResult("example:ruin", "Duplicate lower", 2, 40, 3),
                new StructureResult("example:ruin", "Upper", 2, 96, 3),
                new StructureResult("example:ruin", "Unknown height", 2, 3)
        ));

        ResolverResult<List<StructureResult>> result =
                new AddonStructureResolverChain(List.of(resolver)).resolveStructures(query);

        assertTrue(result.handled());
        assertEquals(Arrays.asList(40, 96, null), result.value().stream()
                .map(StructureResult::blockY).toList());
    }

    @Test
    void invalidHandledResultsAreReportedAndFallThrough() {
        WorldgenContext context = new WorldgenContext(42L, WorldDimension.OVERWORLD,
                "minecraft:normal", true, Map.of());
        StructureQuery query = new StructureQuery(context, "example:ruin", -10, 10, -10, 10);
        WorldgenResolver invalid = resolver(true, List.of(
                new StructureResult("example:wrong", "Wrong", 0, 0)
        ));
        WorldgenResolver fallback = resolver(true, List.of(
                new StructureResult("example:ruin", "Valid", 2, 3)
        ));
        List<AddonIssue> issues = new ArrayList<>();

        ResolverResult<List<StructureResult>> result = new AddonStructureResolverChain(
                List.of(invalid, fallback), issues::add
        ).resolveStructures(query);

        assertTrue(result.handled());
        assertEquals(List.of("2:3"), result.value().stream()
                .map(marker -> marker.blockX() + ":" + marker.blockZ()).toList());
        assertEquals(List.of(AddonIssue.Operation.STRUCTURE_QUERY),
                issues.stream().map(AddonIssue::operation).toList());
    }

    private static WorldgenResolver resolver(boolean supports, List<StructureResult> results) {
        return new WorldgenResolver() {
            @Override
            public String id() {
                return "test:" + System.identityHashCode(this);
            }

            @Override
            public boolean supports(WorldgenContext context) {
                return supports;
            }

            @Override
            public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
                return ResolverResult.handled(results);
            }
        };
    }
}
