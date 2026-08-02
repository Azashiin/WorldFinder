package fr.asashiin.worldfinder.common.map;

/**
 * Immutable transformation between map pixels and horizontal world coordinates.
 *
 * @param centerX world block X coordinate at the viewport centre
 * @param centerZ world block Z coordinate at the viewport centre
 * @param blocksPerPixel positive map scale in world blocks per screen pixel
 */
public record MapViewport(double centerX, double centerZ, double blocksPerPixel) {
    /** Closest supported zoom, in blocks per screen pixel. */
    public static final double MIN_BLOCKS_PER_PIXEL = 0.25;
    /** Farthest supported zoom, in blocks per screen pixel. */
    public static final double MAX_BLOCKS_PER_PIXEL = 24.0;

    /**
     * Creates and validates a viewport.
     *
     * @throws IllegalArgumentException if the centre is non-finite or the scale is non-finite or
     *         outside the inclusive supported range
     */
    public MapViewport {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("Viewport center must be finite");
        }
        if (!Double.isFinite(blocksPerPixel)
                || blocksPerPixel < MIN_BLOCKS_PER_PIXEL
                || blocksPerPixel > MAX_BLOCKS_PER_PIXEL) {
            throw new IllegalArgumentException("Invalid map scale: " + blocksPerPixel);
        }
    }

    /**
     * Returns a viewport translated by a screen-space drag distance.
     *
     * @param pixelsX horizontal drag distance; positive values move the world to the right
     * @param pixelsZ vertical drag distance; positive values move the world downward
     * @return translated viewport with the same scale
     * @throws IllegalArgumentException if the resulting centre is not finite
     */
    public MapViewport pan(double pixelsX, double pixelsZ) {
        return new MapViewport(
                centerX - pixelsX * blocksPerPixel,
                centerZ - pixelsZ * blocksPerPixel,
                blocksPerPixel
        );
    }

    /**
     * Returns a viewport zoomed around a screen-space anchor while preserving its world position.
     *
     * <p>The scale is clamped to {@link #MIN_BLOCKS_PER_PIXEL} and
     * {@link #MAX_BLOCKS_PER_PIXEL}.</p>
     *
     * @param factor scale multiplier; values below {@code 1} zoom in and values above {@code 1} zoom out
     * @param anchorPixelsX horizontal anchor offset from the viewport centre, in pixels
     * @param anchorPixelsZ vertical anchor offset from the viewport centre, in pixels
     * @return zoomed viewport
     * @throws IllegalArgumentException if {@code factor} is non-finite or not positive, or the
     *         resulting centre is not finite
     */
    public MapViewport zoomAt(double factor, double anchorPixelsX, double anchorPixelsZ) {
        if (!Double.isFinite(factor) || factor <= 0.0) {
            throw new IllegalArgumentException("Zoom factor must be positive");
        }
        double nextScale = Math.clamp(blocksPerPixel * factor, MIN_BLOCKS_PER_PIXEL, MAX_BLOCKS_PER_PIXEL);
        double worldAnchorX = centerX + anchorPixelsX * blocksPerPixel;
        double worldAnchorZ = centerZ + anchorPixelsZ * blocksPerPixel;
        return new MapViewport(
                worldAnchorX - anchorPixelsX * nextScale,
                worldAnchorZ - anchorPixelsZ * nextScale,
                nextScale
        );
    }
}
