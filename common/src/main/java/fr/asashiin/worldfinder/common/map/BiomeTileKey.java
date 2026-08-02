package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenProfile;

import java.util.Map;
import java.util.Objects;

/**
 * Stable identity and sampling geometry for one square biome tile.
 *
 * @param seed world seed
 * @param dimension dimension to sample
 * @param sampleY exact block Y for fixed-Y lookup, or nominal Y for a surface projection
 * @param samplingMode vertical projection represented by this cache key
 * @param sampleStep distance in blocks between adjacent samples
 * @param tileX horizontal tile coordinate on the sampling grid
 * @param tileZ vertical tile coordinate on the sampling grid
 * @param tileSize width and height of the square tile in samples
 * @param worldgenProfile versioned identity of the generation rules
 * @param integratedServer whether the query comes from a local integrated server
 * @param resolverAttributes immutable runtime attributes visible to addon resolvers
 */
public record BiomeTileKey(
        long seed,
        WorldDimension dimension,
        int sampleY,
        BiomeSamplingMode samplingMode,
        int sampleStep,
        int tileX,
        int tileZ,
        int tileSize,
        WorldgenProfile worldgenProfile,
        boolean integratedServer,
        Map<String, String> resolverAttributes
) {
    /**
     * Creates and validates a tile key.
     *
     * @throws NullPointerException if a required reference is {@code null}
     * @throws IllegalArgumentException if the projection is unsupported in the dimension, or if
     *         {@code sampleStep} or {@code tileSize} is not positive
     */
    public BiomeTileKey {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(samplingMode, "samplingMode");
        Objects.requireNonNull(worldgenProfile, "worldgenProfile");
        resolverAttributes = Map.copyOf(resolverAttributes);
        if (samplingMode == BiomeSamplingMode.SURFACE && dimension != WorldDimension.OVERWORLD) {
            throw new IllegalArgumentException("SURFACE biome sampling is supported only in the Overworld");
        }
        if (sampleStep <= 0 || tileSize <= 0) {
            throw new IllegalArgumentException("Tile sample step and size must be positive");
        }
    }

    /**
     * Creates a compatibility key for callers which do not yet identify their generation profile.
     * Platform adapters should use the canonical constructor to avoid cross-profile cache reuse.
     *
     * @param seed world seed
     * @param dimension dimension to sample
     * @param sampleY exact block Y coordinate used for biome lookup
     * @param sampleStep distance in blocks between adjacent samples
     * @param tileX horizontal tile coordinate
     * @param tileZ vertical tile coordinate
     * @param tileSize square tile size in samples
     */
    public BiomeTileKey(
            long seed,
            WorldDimension dimension,
            int sampleY,
            int sampleStep,
            int tileX,
            int tileZ,
            int tileSize
    ) {
        this(seed, dimension, sampleY, BiomeSamplingMode.FIXED_Y, sampleStep, tileX, tileZ, tileSize,
                WorldgenProfile.unspecified("minecraft:normal"), false, Map.of());
    }

    /**
     * Creates a compatibility key with an explicit vertical projection and an unspecified
     * generation profile.
     *
     * @param seed world seed
     * @param dimension dimension to sample
     * @param sampleY exact or nominal block Y according to {@code samplingMode}
     * @param samplingMode vertical projection represented by this key
     * @param sampleStep distance in blocks between adjacent samples
     * @param tileX horizontal tile coordinate
     * @param tileZ vertical tile coordinate
     * @param tileSize square tile size in samples
     */
    public BiomeTileKey(
            long seed,
            WorldDimension dimension,
            int sampleY,
            BiomeSamplingMode samplingMode,
            int sampleStep,
            int tileX,
            int tileZ,
            int tileSize
    ) {
        this(seed, dimension, sampleY, samplingMode, sampleStep, tileX, tileZ, tileSize,
                WorldgenProfile.unspecified("minecraft:normal"), false, Map.of());
    }

    /**
     * Creates a fixed-Y key using the complete legacy cache identity.
     *
     * @param seed world seed
     * @param dimension dimension to sample
     * @param sampleY exact block Y coordinate used for biome lookup
     * @param sampleStep distance in blocks between adjacent samples
     * @param tileX horizontal tile coordinate
     * @param tileZ vertical tile coordinate
     * @param tileSize square tile size in samples
     * @param worldgenProfile versioned identity of the generation rules
     * @param integratedServer whether the query comes from a local integrated server
     * @param resolverAttributes immutable runtime attributes visible to addon resolvers
     */
    public BiomeTileKey(
            long seed,
            WorldDimension dimension,
            int sampleY,
            int sampleStep,
            int tileX,
            int tileZ,
            int tileSize,
            WorldgenProfile worldgenProfile,
            boolean integratedServer,
            Map<String, String> resolverAttributes
    ) {
        this(seed, dimension, sampleY, BiomeSamplingMode.FIXED_Y, sampleStep, tileX, tileZ,
                tileSize, worldgenProfile, integratedServer, resolverAttributes);
    }

    /**
     * Computes the block X coordinate of the tile's first sample.
     *
     * @return tile origin in block coordinates
     * @throws ArithmeticException if the coordinate does not fit in a signed integer
     */
    public int originBlockX() {
        return Math.multiplyExact(Math.multiplyExact(tileX, tileSize), sampleStep);
    }

    /**
     * Computes the block Z coordinate of the tile's first sample.
     *
     * @return tile origin in block coordinates
     * @throws ArithmeticException if the coordinate does not fit in a signed integer
     */
    public int originBlockZ() {
        return Math.multiplyExact(Math.multiplyExact(tileZ, tileSize), sampleStep);
    }
}
