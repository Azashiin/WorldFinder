package fr.asashiin.worldfinder.common.map;

/** Selects stable map sampling and overlay grid levels from the current viewport scale. */
public final class BiomeMapLevelOfDetail {
    /** Finest supported distance, in blocks, between neighbouring biome samples. */
    public static final int MIN_SAMPLE_STEP = 4;
    /**
     * Coarsest base distance selected from viewport scale alone. A platform renderer may further
     * coarsen an internal target to keep an exceptionally large viewport inside its GPU budget.
     */
    public static final int MAX_SAMPLE_STEP = 128;
    private static final double LEVEL_SWITCH_RATIO = Math.sqrt(2.0D);

    private BiomeMapLevelOfDetail() {
    }

    /**
     * Selects a power-of-two biome sample step using geometric transition points.
     *
     * @param blocksPerPixel positive, finite viewport scale
     * @return a power of two between {@link #MIN_SAMPLE_STEP} and {@link #MAX_SAMPLE_STEP}, inclusive
     * @throws IllegalArgumentException if the scale is non-finite or not positive
     */
    public static int sampleStep(double blocksPerPixel) {
        requireValidScale(blocksPerPixel);
        int step = MIN_SAMPLE_STEP;
        while (step < MAX_SAMPLE_STEP && step * LEVEL_SWITCH_RATIO < blocksPerPixel) {
            step *= 2;
        }
        return step;
    }

    /**
     * Selects a power-of-two block interval that keeps visible grid lines at a useful spacing.
     *
     * @param blocksPerPixel positive, finite viewport scale
     * @return grid interval in blocks, at least one chunk ({@code 16} blocks)
     * @throws IllegalArgumentException if the scale is non-finite or not positive
     */
    public static int gridStepBlocks(double blocksPerPixel) {
        requireValidScale(blocksPerPixel);
        int step = 16;
        while (step < 1_048_576 && step / blocksPerPixel < 48.0D) {
            step *= 2;
        }
        while (step > 16 && step / blocksPerPixel > 96.0D) {
            step /= 2;
        }
        return step;
    }

    /**
     * Indicates whether a rendered tile should use linear texture filtering at a sample step.
     *
     * @param sampleStep biome sample distance in blocks
     * @return {@code true} for steps of {@code 16} blocks or greater
     */
    public static boolean useLinearFiltering(int sampleStep) {
        return sampleStep >= 16;
    }

    private static void requireValidScale(double blocksPerPixel) {
        if (!Double.isFinite(blocksPerPixel) || blocksPerPixel <= 0.0D) {
            throw new IllegalArgumentException("Map scale must be positive and finite");
        }
    }
}
