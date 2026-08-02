package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Immutable vertical projection selected for an Overworld biome map.
 *
 * <p>A surface projection deliberately has no fixed Y value. The nominal coordinate required by
 * the query protocol is supplied separately by the active generation profile and never turns the
 * surface mode into a fixed-height lookup.</p>
 */
public final class BiomeVerticalSelection {
    private static final BiomeVerticalSelection SURFACE =
            new BiomeVerticalSelection(BiomeSamplingMode.SURFACE, null);

    private final BiomeSamplingMode samplingMode;
    private final Integer fixedY;

    private BiomeVerticalSelection(BiomeSamplingMode samplingMode, Integer fixedY) {
        this.samplingMode = Objects.requireNonNull(samplingMode, "samplingMode");
        this.fixedY = fixedY;
        if ((samplingMode == BiomeSamplingMode.FIXED_Y) != (fixedY != null)) {
            throw new IllegalArgumentException("FIXED_Y requires one exact height and SURFACE forbids it");
        }
    }

    /** Returns the canonical surface projection. */
    public static BiomeVerticalSelection surface() {
        return SURFACE;
    }

    /** Creates an exact fixed-height projection. */
    public static BiomeVerticalSelection fixedY(int blockY) {
        return new BiomeVerticalSelection(BiomeSamplingMode.FIXED_Y, blockY);
    }

    /** Returns the projection sent to native and addon biome engines. */
    public BiomeSamplingMode samplingMode() {
        return samplingMode;
    }

    /** Returns the exact height only when this is a fixed-height projection. */
    public OptionalInt fixedY() {
        return fixedY == null ? OptionalInt.empty() : OptionalInt.of(fixedY);
    }

    /**
     * Returns the query coordinate while preserving the distinct projection mode.
     * Surface engines receive the profile's nominal coordinate but must derive the real surface.
     */
    public int queryY(int nominalSurfaceY) {
        return fixedY == null ? nominalSurfaceY : fixedY;
    }

    /** Returns the concise label displayed by the layer selector. */
    public String displayName() {
        return fixedY == null ? "Surface" : "Y: " + fixedY;
    }

    public boolean isSurface() {
        return samplingMode == BiomeSamplingMode.SURFACE;
    }

    /** Returns whether this projection samples one exact block height. */
    public boolean isFixedY() {
        return samplingMode == BiomeSamplingMode.FIXED_Y;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BiomeVerticalSelection that
                && samplingMode == that.samplingMode
                && Objects.equals(fixedY, that.fixedY);
    }

    @Override
    public int hashCode() {
        return Objects.hash(samplingMode, fixedY);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
