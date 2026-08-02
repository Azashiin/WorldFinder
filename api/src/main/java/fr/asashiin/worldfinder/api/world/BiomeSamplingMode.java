package fr.asashiin.worldfinder.api.world;

/** Declares how a biome query projects a horizontal map coordinate into the world. */
public enum BiomeSamplingMode {
    /** Resolve the biome at the query's exact {@code blockY} or {@code sampleY} coordinate. */
    FIXED_Y,
    /**
     * Resolve the world-generation surface biome for each horizontal coordinate.
     *
     * <p>The query's Y coordinate is nominal in this mode. A resolver which owns custom terrain
     * generation must derive the appropriate surface projection with its own engine. WorldFinder
     * 0.2 supports this mode only in the Overworld.</p>
     */
    SURFACE
}
