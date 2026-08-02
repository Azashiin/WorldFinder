package fr.asashiin.worldfinder.api.world;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves generation-mod-specific results. Higher priority resolvers are queried first.
 * Implementations must be safe to call concurrently from background map workers.
 */
public interface WorldgenResolver {
    /**
     * Returns the stable resolver identifier used for ordering and diagnostics.
     *
     * @return non-null namespaced identifier
     */
    String id();

    /**
     * Returns selection priority; larger values are queried first.
     *
     * @return resolver priority, normally zero
     */
    default int priority() {
        return 0;
    }

    /**
     * Performs a cheap coarse-grained applicability check before a query method is invoked.
     *
     * @param context immutable query context
     * @return {@code true} when this resolver may handle queries for the context
     */
    boolean supports(WorldgenContext context);

    /**
     * Declares whether this resolver understands one vertical biome projection.
     *
     * <p>The additive default protects existing surface-oriented addons: they continue receiving
     * Overworld {@link BiomeSamplingMode#SURFACE} requests, but an Overworld fixed-height request
     * is delegated unless the addon opts in explicitly. Existing Nether and End point behavior is
     * retained because those dimensions have historically used fixed-Y queries.</p>
     *
     * @param context immutable world-generation context
     * @param samplingMode requested surface or fixed-height projection
     * @param sampleY exact Y for {@code FIXED_Y}, or the profile's nominal surface coordinate
     * @return {@code true} only when biome query methods may safely receive this request
     */
    default boolean supportsBiomeSampling(
            WorldgenContext context,
            BiomeSamplingMode samplingMode,
            int sampleY
    ) {
        return Objects.requireNonNull(context, "context").dimension() != WorldDimension.OVERWORLD
                || Objects.requireNonNull(samplingMode, "samplingMode") == BiomeSamplingMode.SURFACE;
    }

    /**
     * Announces optimized operations without affecting correctness or fallback behavior.
     *
     * @return immutable, non-null capability set
     */
    default Set<WorldgenCapability> capabilities() {
        return Set.of();
    }

    /**
     * Explicit point-biome contract. New resolvers should override this method and honor
     * {@link BiomeQuery#samplingMode()}.
     * Returning {@link ResolverResult#unhandled()} delegates to the next resolver.
     *
     * @param query non-null point query
     * @return non-null handled or unhandled resolution
     */
    default ResolverResult<String> resolveBiome(BiomeQuery query) {
        Optional<String> legacyResult = biomeAt(query);
        return legacyResult.isPresent()
                ? ResolverResult.handled(legacyResult.get())
                : ResolverResult.unhandled();
    }

    /**
     * Optimized batch contract used by PixelBiomes and addon engines. The returned region must
     * exactly match the requested width and height. A resolver announcing
     * {@link WorldgenCapability#TERRAIN_COVERAGE} must supply coverage for every sample and state
     * its non-unknown {@link TerrainCoverageResolution}. Implementations must honor
     * {@link BiomeRegionQuery#samplingMode()}; a surface-aware addon derives its own terrain
     * projection when {@link BiomeSamplingMode#SURFACE} is requested.
     * Implementations should check {@link BiomeRegionQuery#cancellationToken()} at least once per
     * output row or another bounded batch so obsolete map work stops promptly.
     *
     * @param query non-null regional query
     * @return non-null handled or unhandled resolution
     */
    default ResolverResult<BiomeRegion> resolveBiomeRegion(BiomeRegionQuery query) {
        return ResolverResult.unhandled();
    }

    /**
     * Legacy point lookup retained for 0.2 preview addons.
     *
     * @param query non-null point query
     * @return biome identifier when handled, otherwise empty
     */
    @Deprecated(forRemoval = false)
    default Optional<String> biomeAt(BiomeQuery query) {
        return Optional.empty();
    }

    /**
     * Finds structures supplied by a world-generation addon inside an inclusive block area.
     * Implementations must only return results matching {@link StructureQuery#targetId()}.
     * An explicitly handled empty list prevents lower-priority or vanilla fallback.
     * Long-running implementations should check {@link StructureQuery#cancellationToken()} at
     * least once per bounded candidate batch so obsolete scans stop promptly.
     *
     * @param query non-null structure query
     * @return non-null handled or unhandled resolution
     */
    default ResolverResult<List<StructureResult>> resolveStructures(StructureQuery query) {
        List<StructureResult> legacyResults = structuresIn(query);
        return legacyResults.isEmpty()
                ? ResolverResult.unhandled()
                : ResolverResult.handled(legacyResults);
    }

    /**
     * Legacy structure lookup retained for 0.2 preview addons.
     * Expensive implementations remain responsible for observing
     * {@link StructureQuery#cancellationToken()}.
     *
     * @param query non-null structure query
     * @return non-null result list; empty delegates in the explicit bridge
     */
    @Deprecated(forRemoval = false)
    default List<StructureResult> structuresIn(StructureQuery query) {
        return List.of();
    }

    /**
     * Creates an immutable capability set without exposing a mutable {@link EnumSet}.
     *
     * @param first first required capability
     * @param remaining additional capabilities
     * @return immutable non-empty capability set
     */
    static Set<WorldgenCapability> capabilities(WorldgenCapability first, WorldgenCapability... remaining) {
        EnumSet<WorldgenCapability> capabilities = EnumSet.of(first, remaining);
        return Set.copyOf(capabilities);
    }
}
