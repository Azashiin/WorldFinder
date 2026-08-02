package fr.asashiin.worldfinder.api.world;

/**
 * Describes the spatial observation represented by a {@link BiomeRegion}'s optional
 * terrain-coverage channel.
 *
 * <p>The resolution is metadata, not a replacement for the unsigned coverage value. Consumers
 * must preserve both so exact observations are not confused with coarse terrain estimates.</p>
 */
public enum TerrainCoverageResolution {
    /** No terrain observation is present. This value is valid only without a coverage channel. */
    UNKNOWN,
    /** Every cell describes the block column at the corresponding query sample coordinate. */
    SAMPLE_POINT,
    /** Every cell describes an exact chunk-centre column observation expanded over that chunk. */
    CHUNK_CENTER,
    /**
     * Every cell is a seed-bound, coarse End-island topology estimate for the area represented by
     * that sample. It must not be presented as block-exact terrain coverage.
     */
    END_ISLAND_TOPOLOGY
}
