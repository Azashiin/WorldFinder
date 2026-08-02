package fr.asashiin.worldfinder.common.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeMapLevelOfDetailTest {
    @Test
    void selectsARealLevelOfDetailAtEveryMapScale() {
        assertEquals(4, BiomeMapLevelOfDetail.sampleStep(0.25D));
        assertEquals(4, BiomeMapLevelOfDetail.sampleStep(4.0D));
        assertEquals(8, BiomeMapLevelOfDetail.sampleStep(8.0D));
        assertEquals(32, BiomeMapLevelOfDetail.sampleStep(32.0D));
        assertEquals(64, BiomeMapLevelOfDetail.sampleStep(64.0D));
        assertEquals(128, BiomeMapLevelOfDetail.sampleStep(128.0D));
    }

    @Test
    void keepsOneSourceSampleCloseToOneScreenPixelWhenZoomedOut() {
        for (double blocksPerPixel = 4.0D; blocksPerPixel <= 128.0D; blocksPerPixel *= 1.1D) {
            double sourcePixels = BiomeMapLevelOfDetail.sampleStep(blocksPerPixel) / blocksPerPixel;
            assertTrue(sourcePixels >= 1.0D / Math.sqrt(2.0D));
            assertTrue(sourcePixels <= Math.sqrt(2.0D));
        }
    }

    @Test
    void adaptsTheWorldGridInsteadOfPackingLinesTogether() {
        assertEquals(16, BiomeMapLevelOfDetail.gridStepBlocks(0.25D));
        assertEquals(256, BiomeMapLevelOfDetail.gridStepBlocks(4.0D));
        assertEquals(8_192, BiomeMapLevelOfDetail.gridStepBlocks(128.0D));
    }
}
