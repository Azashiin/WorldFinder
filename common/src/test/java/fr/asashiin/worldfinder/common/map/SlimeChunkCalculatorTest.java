package fr.asashiin.worldfinder.common.map;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlimeChunkCalculatorTest {
    @Test
    void matchesTheVanillaLegacyRandomFormula() {
        long[] seeds = {0L, 1L, -1L, 123_456_789L, Long.MAX_VALUE};
        int[] chunks = {-10_000, -257, -1, 0, 1, 31, 256, 10_000};
        for (long seed : seeds) {
            for (int chunkX : chunks) {
                for (int chunkZ : chunks) {
                    long slimeSeed = seed
                            + (long)(chunkX * chunkX * 4_987_142)
                            + (long)(chunkX * 5_947_611)
                            + (long)(chunkZ * chunkZ) * 4_392_871L
                            + (long)(chunkZ * 389_711)
                            ^ SlimeChunkCalculator.VANILLA_SALT;
                    assertEquals(new Random(slimeSeed).nextInt(10) == 0,
                            SlimeChunkCalculator.isSlimeChunk(seed, chunkX, chunkZ));
                }
            }
        }
    }
}
