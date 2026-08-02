package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.TerrainCoverageResolution;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeTileTest {
    @Test
    void remainsImmutableWhileUsingPaletteCompressedBiomeIds() {
        BiomeTileKey key = new BiomeTileKey(1L, WorldDimension.OVERWORLD, 64, 4, 0, 0, 2);
        int[] colors = {1, 2, 3, 4};
        String[] biomeIds = {"plains", "forest", "plains", "forest"};
        BiomeTile tile = new BiomeTile(key, colors, biomeIds);

        colors[0] = 99;
        biomeIds[0] = "desert";
        assertEquals(1, tile.color(0, 0));
        assertEquals("plains", tile.biomeId(0, 0));

        int[] exportedColors = tile.colors();
        String[] exportedBiomes = tile.biomeIds();
        exportedColors[1] = 99;
        exportedBiomes[1] = "desert";
        assertNotEquals(99, tile.color(1, 0));
        assertEquals("forest", tile.biomeId(1, 0));
    }

    @Test
    void supportsMoreThanTwoHundredAndFiftySixDistinctBiomeIds() {
        int tileSize = 17;
        int entries = tileSize * tileSize;
        int[] colors = new int[entries];
        String[] biomeIds = new String[entries];
        for (int index = 0; index < entries; index++) biomeIds[index] = "test:biome_" + index;

        BiomeTile tile = new BiomeTile(
                new BiomeTileKey(1L, WorldDimension.OVERWORLD, 64, 4, 0, 0, tileSize),
                colors,
                biomeIds);

        assertEquals("test:biome_288", tile.biomeId(288));
    }

    @Test
    void keepsExactTerrainCoverageSeparateFromBiomeIdentity() {
        BiomeTileKey key = new BiomeTileKey(1L, WorldDimension.END, 64, 4, 0, 0, 2);
        byte[] coverage = {0, (byte)255, 64, (byte)128};
        BiomeTile tile = new BiomeTile(
                key,
                new int[]{1, 1, 2, 2},
                new String[]{"minecraft:the_end", "minecraft:the_end",
                        "minecraft:end_highlands", "minecraft:end_highlands"},
                coverage
        );

        coverage[0] = (byte)255;
        assertTrue(tile.hasExactTerrainCoverage());
        assertTrue(tile.hasTerrainCoverage());
        assertEquals(TerrainCoverageResolution.SAMPLE_POINT, tile.terrainCoverageResolution());
        assertEquals(0, tile.terrainCoverage(0, 0));
        assertEquals(255, tile.terrainCoverage(1, 0));
        assertEquals("minecraft:the_end", tile.biomeId(0, 0));

        byte[] exported = tile.terrainCoverage();
        exported[1] = 0;
        assertEquals(255, tile.terrainCoverage(1, 0));
    }

    @Test
    void legacyTilesExposeAnExplicitlyUnknownFullCoverageFallback() {
        BiomeTile tile = new BiomeTile(
                new BiomeTileKey(1L, WorldDimension.OVERWORLD, 64, 4, 0, 0, 1),
                new int[]{1},
                new String[]{"minecraft:plains"}
        );

        assertFalse(tile.hasExactTerrainCoverage());
        assertFalse(tile.hasTerrainCoverage());
        assertEquals(TerrainCoverageResolution.UNKNOWN, tile.terrainCoverageResolution());
        assertEquals(255, tile.terrainCoverage(0));
    }

    @Test
    void declaresChunkCenterCoverageWithoutClaimingPerPixelExactness() {
        BiomeTile tile = new BiomeTile(
                new BiomeTileKey(1L, WorldDimension.END, 64, 4, 0, 0, 1),
                new int[]{1},
                new String[]{"minecraft:the_end"},
                new byte[]{(byte)255},
                TerrainCoverageResolution.CHUNK_CENTER
        );

        assertTrue(tile.hasTerrainCoverage());
        assertFalse(tile.hasExactTerrainCoverage());
        assertEquals(TerrainCoverageResolution.CHUNK_CENTER, tile.terrainCoverageResolution());
    }
}
