package fr.asashiin.worldfinder.common.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapViewportTest {
    @Test
    void panConvertsPixelsToWorldBlocks() {
        MapViewport viewport = new MapViewport(100.0, -50.0, 4.0);

        MapViewport moved = viewport.pan(5.0, -3.0);

        assertEquals(80.0, moved.centerX());
        assertEquals(-38.0, moved.centerZ());
    }

    @Test
    void zoomKeepsTheWorldPointUnderTheCursorStable() {
        MapViewport viewport = new MapViewport(100.0, 200.0, 4.0);

        MapViewport zoomed = viewport.zoomAt(0.5, 20.0, -10.0);

        assertEquals(2.0, zoomed.blocksPerPixel());
        assertEquals(140.0, zoomed.centerX());
        assertEquals(180.0, zoomed.centerZ());
    }

    @Test
    void zoomOutClampsAtTwentyFourBlocksPerPixelAndKeepsTheAnchorStable() {
        MapViewport viewport = new MapViewport(100.0, 200.0, 20.0);

        MapViewport zoomed = viewport.zoomAt(2.0, 3.0, -5.0);

        assertEquals(24.0, zoomed.blocksPerPixel());
        assertEquals(88.0, zoomed.centerX());
        assertEquals(220.0, zoomed.centerZ());
    }

    @Test
    void rejectsAScaleAboveTwentyFourBlocksPerPixel() {
        assertEquals(24.0, MapViewport.MAX_BLOCKS_PER_PIXEL);
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(0.0, 0.0, 24.0001));
    }

    @Test
    void rejectsAnInvalidScale() {
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(0.0, 0.0, 0.0));
    }
}
