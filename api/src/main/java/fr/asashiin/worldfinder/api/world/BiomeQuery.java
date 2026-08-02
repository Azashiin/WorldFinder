package fr.asashiin.worldfinder.api.world;

import java.util.Objects;

/**
 * Single block-space biome sample requested from a resolver.
 *
 * @param context immutable world-generation context
 * @param blockX block-space X coordinate
 * @param blockY exact block-space Y for {@link BiomeSamplingMode#FIXED_Y}, or a nominal Y for
 *               {@link BiomeSamplingMode#SURFACE}
 * @param blockZ block-space Z coordinate
 * @param samplingMode vertical projection requested by the map
 */
public record BiomeQuery(
        WorldgenContext context,
        int blockX,
        int blockY,
        int blockZ,
        BiomeSamplingMode samplingMode
) {
    /**
     * Validates and creates a point query.
     *
     * @throws NullPointerException if {@code context} or {@code samplingMode} is {@code null}
     * @throws IllegalArgumentException if surface sampling is requested outside the Overworld
     */
    public BiomeQuery {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(samplingMode, "samplingMode");
        if (samplingMode == BiomeSamplingMode.SURFACE
                && context.dimension() != WorldDimension.OVERWORLD) {
            throw new IllegalArgumentException("SURFACE biome sampling is supported only in the Overworld");
        }
    }

    /**
     * Creates a fixed-Y point query using the legacy coordinate contract.
     *
     * @param context immutable world-generation context
     * @param blockX block-space X coordinate
     * @param blockY exact block-space Y coordinate
     * @param blockZ block-space Z coordinate
     */
    public BiomeQuery(WorldgenContext context, int blockX, int blockY, int blockZ) {
        this(context, blockX, blockY, blockZ, BiomeSamplingMode.FIXED_Y);
    }
}
