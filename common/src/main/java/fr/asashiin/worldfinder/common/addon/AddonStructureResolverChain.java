package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.target.NamespacedId;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.StructureQuery;
import fr.asashiin.worldfinder.api.world.StructureResult;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;

/** Queries compatible addon resolvers in priority order and rejects invalid or duplicate markers. */
public final class AddonStructureResolverChain {
    private final List<WorldgenResolver> resolvers;
    private final AddonIssueReporter reporter;

    /**
     * Creates a chain which discards diagnostics.
     *
     * @param resolvers non-null resolver list already ordered by descending priority
     */
    public AddonStructureResolverChain(List<WorldgenResolver> resolvers) {
        this(resolvers, AddonIssueReporter.ignoring());
    }

    /**
     * Creates a chain with structured failure reporting.
     *
     * @param resolvers non-null resolver list already ordered by descending priority
     * @param reporter non-null diagnostic sink
     */
    public AddonStructureResolverChain(List<WorldgenResolver> resolvers, AddonIssueReporter reporter) {
        this.resolvers = List.copyOf(resolvers);
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    /**
     * Compatibility view which combines valid unique results from every handled resolver.
     *
     * <p>A one-query scan session is opened for compatibility. Multi-target callers should keep
     * one {@link ScanSession} for the complete cell so support checks and failure diagnostics are
     * bounded per cell.</p>
     *
     * @param query non-null structure query
     * @return immutable combined result list
     */
    public List<StructureResult> structuresIn(StructureQuery query) {
        Objects.requireNonNull(query, "query");
        query.cancellationToken().throwIfCancellationRequested();
        return openScanSession(query.context()).structuresIn(query);
    }

    /**
     * Returns the first valid handled result in resolver-priority order.
     *
     * <p>A one-query scan session is opened for compatibility. Multi-target callers should keep
     * one {@link ScanSession} for the complete cell.</p>
     *
     * @param query non-null structure query
     * @return explicit handled or unhandled result
     */
    public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
        Objects.requireNonNull(query, "query");
        query.cancellationToken().throwIfCancellationRequested();
        return openScanSession(query.context()).resolveStructures(query);
    }

    /**
     * Opens a mutable cell-scoped session for one world-generation context.
     *
     * <p>Each resolver's support predicate is evaluated exactly once. A resolver which fails a
     * structure query is quarantined for all remaining targets in this cell, so at most one
     * structure-query diagnostic is emitted for that resolver. Sessions are intended to remain
     * confined to one scan worker; the immutable chain remains safe to share.</p>
     *
     * @param context context shared by every target query in the scan cell
     * @return a new cell-scoped session preserving resolver priority order
     */
    public ScanSession openScanSession(WorldgenContext context) {
        Objects.requireNonNull(context, "context");
        List<ResolverState> supportedResolvers = new ArrayList<>(resolvers.size());
        boolean supportFailureObserved = false;
        for (WorldgenResolver resolver : resolvers) {
            String sourceId = sourceId(resolver);
            try {
                if (resolver.supports(context)) {
                    supportedResolvers.add(new ResolverState(resolver, sourceId));
                }
            } catch (Throwable failure) {
                AddonIsolation.report(reporter, sourceId, AddonIssue.Operation.SUPPORT_CHECK, failure);
                supportFailureObserved = true;
            }
        }
        return new ScanSession(context, supportedResolvers, supportFailureObserved);
    }

    /** Mutable resolver state owned by one structure scan cell. */
    public final class ScanSession {
        private final WorldgenContext context;
        private final List<ResolverState> supportedResolvers;
        private boolean failureObserved;

        private ScanSession(
                WorldgenContext context,
                List<ResolverState> supportedResolvers,
                boolean failureObserved
        ) {
            this.context = context;
            this.supportedResolvers = supportedResolvers;
            this.failureObserved = failureObserved;
        }

        /**
         * Reports whether a resolver support check or structure query failed in this cell.
         * Valid lower-priority results remain available, but callers must not describe them as an
         * exhaustive successful scan when this flag is set.
         *
         * @return {@code true} after at least one isolated resolver failure
         */
        public boolean failureObserved() {
            return failureObserved;
        }

        /**
         * Combines valid unique results from every still-active handled resolver.
         *
         * @param query query using this session's context
         * @return immutable combined result list
         * @throws IllegalArgumentException when the query belongs to another context
         */
        public List<StructureResult> structuresIn(StructureQuery query) {
            Objects.requireNonNull(query, "query");
            requireSessionContext(query.context());
            query.cancellationToken().throwIfCancellationRequested();
            List<StructureResult> combined = new ArrayList<>();
            Set<ResultKey> seen = new HashSet<>();
            for (ResolverState state : supportedResolvers) {
                query.cancellationToken().throwIfCancellationRequested();
                if (!state.structuresActive) continue;
                try {
                    ResolverResult<List<StructureResult>> resolution = Objects.requireNonNull(
                            state.resolver.resolveStructures(query), "Resolver returned null"
                    );
                    query.cancellationToken().throwIfCancellationRequested();
                    if (resolution.handled()) {
                        addValidResults(query, resolution.value(), combined, seen);
                    }
                } catch (CancellationException cancellation) {
                    throw cancellation;
                } catch (Throwable failure) {
                    quarantine(state, failure);
                }
            }
            query.cancellationToken().throwIfCancellationRequested();
            return List.copyOf(combined);
        }

        /**
         * Returns the first valid handled result in resolver-priority order.
         *
         * @param query query using this session's context
         * @return explicit handled or unhandled result
         * @throws IllegalArgumentException when the query belongs to another context
         */
        public ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
            Objects.requireNonNull(query, "query");
            requireSessionContext(query.context());
            query.cancellationToken().throwIfCancellationRequested();
            for (ResolverState state : supportedResolvers) {
                query.cancellationToken().throwIfCancellationRequested();
                if (!state.structuresActive) continue;
                try {
                    ResolverResult<List<StructureResult>> resolution = Objects.requireNonNull(
                            state.resolver.resolveStructures(query), "Resolver returned null"
                    );
                    query.cancellationToken().throwIfCancellationRequested();
                    if (!resolution.handled()) continue;
                    return ResolverResult.handled(validateHandledResults(query, resolution.value()));
                } catch (CancellationException cancellation) {
                    throw cancellation;
                } catch (Throwable failure) {
                    quarantine(state, failure);
                }
            }
            query.cancellationToken().throwIfCancellationRequested();
            return ResolverResult.unhandled();
        }

        private void quarantine(ResolverState state, Throwable failure) {
            state.structuresActive = false;
            AddonIsolation.report(
                    reporter, state.sourceId, AddonIssue.Operation.STRUCTURE_QUERY, failure);
            failureObserved = true;
        }

        private void requireSessionContext(WorldgenContext queryContext) {
            if (context != queryContext && !context.equals(queryContext)) {
                throw new IllegalArgumentException(
                        "Resolver session cannot be reused with another world-generation context");
            }
        }
    }

    private static String sourceId(WorldgenResolver resolver) {
        try {
            return NamespacedId.requireValid(resolver.id());
        } catch (Throwable failure) {
            AddonIsolation.rethrowFatal(failure);
            return AddonIsolation.sourceId(resolver);
        }
    }

    private static void addValidResults(
            StructureQuery query,
            List<StructureResult> candidates,
            List<StructureResult> destination,
            Set<ResultKey> seen
    ) {
        for (StructureResult result : Objects.requireNonNull(
                candidates, "Resolver returned null result list")) {
            query.cancellationToken().throwIfCancellationRequested();
            if (result == null || !result.structureId().equals(query.targetId())
                    || !query.contains(result.blockX(), result.blockZ())) {
                continue;
            }
            if (seen.add(new ResultKey(
                    result.structureId(), result.blockX(), result.blockY(), result.blockZ()))) {
                destination.add(result);
            }
        }
    }

    private static List<StructureResult> validateHandledResults(
            StructureQuery query,
            List<StructureResult> candidates
    ) {
        Objects.requireNonNull(candidates, "Resolver returned null result list");
        List<StructureResult> results = new ArrayList<>();
        Set<ResultKey> seen = new HashSet<>();
        for (StructureResult result : candidates) {
            query.cancellationToken().throwIfCancellationRequested();
            if (result == null) {
                throw new IllegalArgumentException("Resolver returned a null structure result");
            }
            if (!result.structureId().equals(query.targetId())) {
                throw new IllegalArgumentException("Resolver returned structure " + result.structureId()
                        + " for target " + query.targetId());
            }
            if (!query.contains(result.blockX(), result.blockZ())) {
                throw new IllegalArgumentException("Resolver returned structure outside query bounds at "
                        + result.blockX() + ", " + result.blockZ());
            }
            if (seen.add(new ResultKey(
                    result.structureId(), result.blockX(), result.blockY(), result.blockZ()))) {
                results.add(result);
            }
        }
        query.cancellationToken().throwIfCancellationRequested();
        return List.copyOf(results);
    }

    private static final class ResolverState {
        private final WorldgenResolver resolver;
        private final String sourceId;
        private boolean structuresActive = true;

        private ResolverState(WorldgenResolver resolver, String sourceId) {
            this.resolver = resolver;
            this.sourceId = sourceId;
        }
    }

    private record ResultKey(String structureId, int blockX, Integer blockY, int blockZ) {
    }
}
