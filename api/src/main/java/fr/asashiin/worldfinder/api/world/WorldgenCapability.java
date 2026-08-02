package fr.asashiin.worldfinder.api.world;

/** Optional capabilities announced by a {@link WorldgenResolver}. */
public enum WorldgenCapability {
    /** Optimized or exact single-position biome queries. */
    POINT_BIOMES,
    /** Batch biome queries that avoid per-pixel resolver overhead. */
    REGION_BIOMES,
    /**
     * The resolver supplies a 0..255 terrain coverage value and a non-unknown
     * {@link TerrainCoverageResolution} for every regional sample.
     */
    TERRAIN_COVERAGE,
    /** Batched structure searches. */
    STRUCTURES
}
