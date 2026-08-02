package fr.asashiin.worldfinder.common.map;

/** Reproduces the vanilla Java-edition slime-chunk predicate without Minecraft runtime classes. */
public final class SlimeChunkCalculator {
    /** Salt mixed into the legacy random seed by vanilla slime-chunk selection. */
    public static final long VANILLA_SALT = 987_234_911L;
    private static final long RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long RANDOM_ADDEND = 0xBL;
    private static final long RANDOM_MASK = (1L << 48) - 1;

    private SlimeChunkCalculator() {
    }

    /**
     * Tests whether a chunk is selected as a slime chunk for a world seed.
     *
     * @param worldSeed world seed
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return {@code true} when the vanilla selector accepts the chunk
     */
    public static boolean isSlimeChunk(long worldSeed, int chunkX, int chunkZ) {
        // The int overflows in these terms are intentional and match WorldgenRandom#seedSlimeChunk.
        long slimeSeed = worldSeed
                + (long)(chunkX * chunkX * 4_987_142)
                + (long)(chunkX * 5_947_611)
                + (long)(chunkZ * chunkZ) * 4_392_871L
                + (long)(chunkZ * 389_711)
                ^ VANILLA_SALT;
        return firstLegacyRandomInt(slimeSeed, 10) == 0;
    }

    private static int firstLegacyRandomInt(long seed, int bound) {
        long state = (seed ^ RANDOM_MULTIPLIER) & RANDOM_MASK;
        while (true) {
            state = (state * RANDOM_MULTIPLIER + RANDOM_ADDEND) & RANDOM_MASK;
            int bits = (int)(state >>> 17);
            int value = bits % bound;
            if (bits - value + bound - 1 >= 0) return value;
        }
    }
}
