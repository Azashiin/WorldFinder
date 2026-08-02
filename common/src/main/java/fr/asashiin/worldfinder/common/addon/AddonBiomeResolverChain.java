package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.target.NamespacedId;
import fr.asashiin.worldfinder.api.world.BiomeQuery;
import fr.asashiin.worldfinder.api.world.BiomeRegion;
import fr.asashiin.worldfinder.api.world.BiomeRegionQuery;
import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.WorldgenCapability;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;

/**
 * Thread-safe immutable resolver chain for point and regional biome queries.
 * Third-party failures are isolated per resolver while cancellation and fatal JVM failures escape.
 */
public final class AddonBiomeResolverChain {
    private final List<WorldgenResolver> resolvers;
    private final AddonIssueReporter reporter;

    /**
     * Creates a chain which discards diagnostics.
     *
     * @param resolvers non-null resolver list already ordered by descending priority
     */
    public AddonBiomeResolverChain(List<WorldgenResolver> resolvers) {
        this(resolvers, AddonIssueReporter.ignoring());
    }

    /**
     * Creates a chain with structured failure reporting.
     *
     * @param resolvers non-null resolver list already ordered by descending priority
     * @param reporter non-null diagnostic sink
     */
    public AddonBiomeResolverChain(List<WorldgenResolver> resolvers, AddonIssueReporter reporter) {
        this.resolvers = List.copyOf(resolvers);
        this.reporter = Objects.requireNonNull(reporter, "reporter");
    }

    /**
     * Resolves one biome through a compatibility optional view.
     *
     * @param query non-null point query
     * @return resolved biome identifier, or empty when no resolver handled it
     */
    public Optional<String> biomeAt(BiomeQuery query) {
        return resolveBiome(query).optionalValue();
    }

    /**
     * Returns the first valid handled result in resolver-priority order.
     *
     * @param query non-null point query
     * @return explicit handled or unhandled result
     */
    public ResolverResult<String> resolveBiome(BiomeQuery query) {
        Objects.requireNonNull(query, "query");
        return openTileSession(query.context()).resolveBiome(query);
    }

    /**
     * Returns the first valid handled regional result in resolver-priority order.
     *
     * @param query non-null regional query carrying cooperative cancellation
     * @return explicit handled or unhandled regional result
     */
    public ResolverResult<BiomeRegion> resolveBiomeRegion(BiomeRegionQuery query) {
        Objects.requireNonNull(query, "query");
        return openTileSession(query.context()).resolveBiomeRegion(query);
    }

    /**
     * Opens a mutable, tile-scoped resolver session for one world-generation context.
     * Applicability is evaluated exactly once per resolver when the session is opened. The same
     * session should be used for the optional regional query and every point fallback in a tile.
     * A resolver which fails a point query is quarantined for the remainder of this session and
     * therefore emits at most one point-query diagnostic.
     * <p>
     * Sessions are intended to remain confined to one map worker. The immutable chain itself is
     * still safe to share between workers.
     *
     * @param context non-null context shared by every query in the tile
     * @return new tile-scoped session preserving resolver priority order
     */
    public TileSession openTileSession(WorldgenContext context) {
        Objects.requireNonNull(context, "context");
        List<ResolverState> supportedResolvers = new ArrayList<>(resolvers.size());
        for (WorldgenResolver resolver : resolvers) {
            String sourceId = sourceId(resolver);
            try {
                if (resolver.supports(context)) {
                    supportedResolvers.add(new ResolverState(resolver, sourceId));
                }
            } catch (Throwable failure) {
                AddonIsolation.report(reporter, sourceId, AddonIssue.Operation.SUPPORT_CHECK, failure);
            }
        }
        return new TileSession(context, supportedResolvers);
    }

    /**
     * Resolver state owned by one tile generation. This keeps coarse support checks and addon
     * failures out of the inner pixel loop.
     */
    public final class TileSession {
        private final WorldgenContext context;
        private final List<ResolverState> supportedResolvers;

        private TileSession(WorldgenContext context, List<ResolverState> supportedResolvers) {
            this.context = context;
            this.supportedResolvers = supportedResolvers;
        }

        /**
         * Returns the first valid handled point result in resolver-priority order.
         *
         * @param query non-null point query using this session's context
         * @return explicit handled or unhandled result
         * @throws IllegalArgumentException when the query belongs to another context
         */
        public ResolverResult<String> resolveBiome(BiomeQuery query) {
            Objects.requireNonNull(query, "query");
            requireSessionContext(query.context());
            for (ResolverState state : supportedResolvers) {
                if (!state.pointActive) {
                    continue;
                }
                try {
                    if (!supportsSampling(state, query.samplingMode(), query.blockY())) {
                        continue;
                    }
                    ResolverResult<String> result = Objects.requireNonNull(
                            state.resolver.resolveBiome(query), "Resolver returned null"
                    );
                    if (!result.handled()) {
                        continue;
                    }
                    return ResolverResult.handled(NamespacedId.requireValid(result.value()));
                } catch (CancellationException cancellation) {
                    throw cancellation;
                } catch (Throwable failure) {
                    state.pointActive = false;
                    AddonIsolation.report(
                            reporter, state.sourceId, AddonIssue.Operation.BIOME_QUERY, failure);
                }
            }
            return ResolverResult.unhandled();
        }

        /**
         * Returns the first valid handled regional result in resolver-priority order.
         * Regional failures are isolated from the point fallback: an addon may still implement
         * its legacy point contract when its optional batch path is unavailable.
         *
         * @param query non-null regional query using this session's context
         * @return explicit handled or unhandled regional result
         * @throws IllegalArgumentException when the query belongs to another context
         */
        public ResolverResult<BiomeRegion> resolveBiomeRegion(BiomeRegionQuery query) {
            Objects.requireNonNull(query, "query");
            requireSessionContext(query.context());
            query.cancellationToken().throwIfCancellationRequested();
            for (ResolverState state : supportedResolvers) {
                query.cancellationToken().throwIfCancellationRequested();
                if (!state.regionActive) {
                    continue;
                }
                try {
                    if (!supportsSampling(state, query.samplingMode(), query.sampleY())) {
                        continue;
                    }
                    ResolverResult<BiomeRegion> result = Objects.requireNonNull(
                            state.resolver.resolveBiomeRegion(query), "Resolver returned null"
                    );
                    if (!result.handled()) {
                        continue;
                    }
                    BiomeRegion region = Objects.requireNonNull(
                            result.value(), "Resolver returned null region");
                    if (region.width() != query.width() || region.height() != query.height()) {
                        throw new IllegalArgumentException(
                                "Resolver returned " + region.width() + 'x' + region.height()
                                        + " region for " + query.width() + 'x' + query.height()
                                        + " query");
                    }
                    Set<WorldgenCapability> capabilities = Objects.requireNonNull(
                            state.resolver.capabilities(), "Resolver returned null capabilities"
                    );
                    if (capabilities.contains(WorldgenCapability.TERRAIN_COVERAGE)
                            && !region.hasTerrainCoverage()) {
                        throw new IllegalArgumentException(
                                "Resolver announces terrain coverage but returned none");
                    }
                    return ResolverResult.handled(region);
                } catch (CancellationException cancellation) {
                    throw cancellation;
                } catch (Throwable failure) {
                    state.regionActive = false;
                    AddonIsolation.report(
                            reporter, state.sourceId, AddonIssue.Operation.BIOME_REGION_QUERY, failure);
                }
            }
            return ResolverResult.unhandled();
        }

        private boolean supportsSampling(
                ResolverState state,
                BiomeSamplingMode samplingMode,
                int sampleY
        ) {
            if (state.samplingMode == samplingMode && state.sampleY == sampleY
                    && state.samplingSupported != null) {
                return state.samplingSupported;
            }
            try {
                boolean supported = state.resolver.supportsBiomeSampling(
                        context, samplingMode, sampleY);
                state.samplingMode = samplingMode;
                state.sampleY = sampleY;
                state.samplingSupported = supported;
                return supported;
            } catch (Throwable failure) {
                state.pointActive = false;
                state.regionActive = false;
                AddonIsolation.report(
                        reporter, state.sourceId, AddonIssue.Operation.SUPPORT_CHECK, failure);
                return false;
            }
        }

        private void requireSessionContext(WorldgenContext queryContext) {
            if (context != queryContext && !context.equals(queryContext)) {
                throw new IllegalArgumentException(
                        "Resolver session cannot be reused with another world-generation context");
            }
        }
    }

    private static final class ResolverState {
        private final WorldgenResolver resolver;
        private final String sourceId;
        private boolean pointActive = true;
        private boolean regionActive = true;
        private BiomeSamplingMode samplingMode;
        private int sampleY;
        private Boolean samplingSupported;

        private ResolverState(WorldgenResolver resolver, String sourceId) {
            this.resolver = resolver;
            this.sourceId = sourceId;
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
}
