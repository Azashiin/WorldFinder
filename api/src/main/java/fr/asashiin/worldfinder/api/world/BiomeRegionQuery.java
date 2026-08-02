package fr.asashiin.worldfinder.api.world;

import java.util.Objects;

/**
 * Rectangular, row-major biome sampling request.
 * Sample {@code (x,z)} represents block coordinate
 * {@code (originBlockX + x * blockStep, originBlockZ + z * blockStep)}.
 *
 * @param context immutable world-generation context
 * @param originBlockX block-space X coordinate of sample {@code (0,0)}
 * @param originBlockZ block-space Z coordinate of sample {@code (0,0)}
 * @param width positive number of horizontal samples
 * @param height positive number of vertical samples
 * @param blockStep positive block distance between adjacent samples on both axes
 * @param sampleY exact block-space Y for {@link BiomeSamplingMode#FIXED_Y}, or a nominal Y for
 *                {@link BiomeSamplingMode#SURFACE}
 * @param samplingMode vertical projection requested for every regional sample
 * @param cancellationToken cooperative signal which regional engines should check at least once
 *                          per row or equivalent bounded batch
 */
public record BiomeRegionQuery(
        WorldgenContext context,
        int originBlockX,
        int originBlockZ,
        int width,
        int height,
        int blockStep,
        int sampleY,
        BiomeSamplingMode samplingMode,
        CancellationToken cancellationToken
) {
    /**
     * Validates projection, dimensions, step, sample count, and coordinate range.
     *
     * @throws NullPointerException if a required reference is {@code null}
     * @throws IllegalArgumentException if the projection is unsupported in the dimension or the
     *         sampling geometry is invalid
     */
    public BiomeRegionQuery {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(samplingMode, "samplingMode");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        if (samplingMode == BiomeSamplingMode.SURFACE
                && context.dimension() != WorldDimension.OVERWORLD) {
            throw new IllegalArgumentException("SURFACE biome sampling is supported only in the Overworld");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Biome region dimensions must be positive");
        }
        if (blockStep <= 0) {
            throw new IllegalArgumentException("Biome region blockStep must be positive");
        }
        long sampleCount = (long) width * height;
        if (sampleCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Biome region contains too many samples: " + sampleCount);
        }
        requireBlockCoordinate((long) originBlockX + (long) (width - 1) * blockStep, "last X sample");
        requireBlockCoordinate((long) originBlockZ + (long) (height - 1) * blockStep, "last Z sample");
    }

    /**
     * Creates a query which is never cancelled. Retained as a convenience and compatibility
     * overload for callers which do not own an asynchronous lifecycle.
     *
     * @param context immutable world-generation context
     * @param originBlockX block-space X coordinate of sample {@code (0,0)}
     * @param originBlockZ block-space Z coordinate of sample {@code (0,0)}
     * @param width positive number of horizontal samples
     * @param height positive number of vertical samples
     * @param blockStep positive block distance between adjacent samples on both axes
     * @param sampleY block-space Y coordinate sampled for every cell
     */
    public BiomeRegionQuery(
            WorldgenContext context,
            int originBlockX,
            int originBlockZ,
            int width,
            int height,
            int blockStep,
            int sampleY
    ) {
        this(context, originBlockX, originBlockZ, width, height, blockStep, sampleY,
                BiomeSamplingMode.FIXED_Y, CancellationToken.NONE);
    }

    /**
     * Creates a cancellable fixed-Y query using the legacy coordinate contract.
     *
     * @param context immutable world-generation context
     * @param originBlockX block-space X coordinate of sample {@code (0,0)}
     * @param originBlockZ block-space Z coordinate of sample {@code (0,0)}
     * @param width positive number of horizontal samples
     * @param height positive number of vertical samples
     * @param blockStep positive block distance between adjacent samples on both axes
     * @param sampleY exact block-space Y sampled for every cell
     * @param cancellationToken cooperative cancellation signal
     */
    public BiomeRegionQuery(
            WorldgenContext context,
            int originBlockX,
            int originBlockZ,
            int width,
            int height,
            int blockStep,
            int sampleY,
            CancellationToken cancellationToken
    ) {
        this(context, originBlockX, originBlockZ, width, height, blockStep, sampleY,
                BiomeSamplingMode.FIXED_Y, cancellationToken);
    }

    /**
     * Returns the region's total number of samples.
     *
     * @return sample count, guaranteed to fit in a signed integer
     */
    public int sampleCount() {
        return width * height;
    }

    /**
     * Converts a sample column to block space.
     *
     * @param sampleX zero-based sample column
     * @return corresponding block-space X coordinate
     */
    public int blockX(int sampleX) {
        requireSampleCoordinate(sampleX, width, "sampleX");
        return (int) ((long) originBlockX + (long) sampleX * blockStep);
    }

    /**
     * Converts a sample row to block space.
     *
     * @param sampleZ zero-based sample row
     * @return corresponding block-space Z coordinate
     */
    public int blockZ(int sampleZ) {
        requireSampleCoordinate(sampleZ, height, "sampleZ");
        return (int) ((long) originBlockZ + (long) sampleZ * blockStep);
    }

    private static void requireSampleCoordinate(int coordinate, int size, String name) {
        if (coordinate < 0 || coordinate >= size) {
            throw new IndexOutOfBoundsException(name + " must be in [0, " + size + ")");
        }
    }

    private static void requireBlockCoordinate(long coordinate, String name) {
        if (coordinate < Integer.MIN_VALUE || coordinate > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(name + " is outside block-coordinate range");
        }
    }
}
