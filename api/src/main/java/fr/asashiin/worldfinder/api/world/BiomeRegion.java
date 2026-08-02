package fr.asashiin.worldfinder.api.world;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, palette-indexed result for a {@link BiomeRegionQuery}.
 *
 * <p>Biome indices and optional terrain coverage are row-major ({@code z * width + x}).
 * Coverage is interpreted as an unsigned byte: 0 is void, 255 is fully covered terrain, and
 * intermediate values preserve small islands and edges at distant zoom levels. Biome identity
 * remains independent from coverage, so a void sample still has its true Minecraft biome. The
 * accompanying {@link TerrainCoverageResolution} declares whether those values are point, chunk,
 * or coarse topology observations.</p>
 */
public final class BiomeRegion {
    private final int width;
    private final int height;
    private final List<String> biomePalette;
    private final int[] biomeIndices;
    private final byte[] terrainCoverage;
    private final TerrainCoverageResolution terrainCoverageResolution;

    /**
     * Creates a region without a terrain-coverage layer.
     *
     * @param width positive sample width
     * @param height positive sample height
     * @param biomePalette non-empty, duplicate-free namespaced biome palette
     * @param biomeIndices row-major palette indices with exactly {@code width * height} entries
     */
    public BiomeRegion(int width, int height, List<String> biomePalette, int[] biomeIndices) {
        this(width, height, biomePalette, biomeIndices, null, TerrainCoverageResolution.UNKNOWN);
    }

    /**
     * Creates a region with optional terrain coverage using the legacy resolution convention.
     * Every mutable input is defensively copied.
     *
     * <p>A non-null coverage channel is conservatively declared as
     * {@link TerrainCoverageResolution#SAMPLE_POINT}, matching the interpretation used by
     * WorldFinder before resolution metadata was exposed. New regional engines should use
     * {@link #BiomeRegion(int, int, List, int[], byte[], TerrainCoverageResolution)} to state
     * their coverage semantics explicitly.</p>
     *
     * @param width positive sample width
     * @param height positive sample height
     * @param biomePalette non-empty, duplicate-free namespaced biome palette
     * @param biomeIndices row-major palette indices with exactly {@code width * height} entries
     * @param terrainCoverage row-major unsigned coverage bytes, or {@code null} when unavailable
     */
    public BiomeRegion(
            int width,
            int height,
            List<String> biomePalette,
            int[] biomeIndices,
            byte[] terrainCoverage
    ) {
        this(width, height, biomePalette, biomeIndices, terrainCoverage,
                terrainCoverage == null
                        ? TerrainCoverageResolution.UNKNOWN
                        : TerrainCoverageResolution.SAMPLE_POINT);
    }

    /**
     * Creates a region with explicitly described optional terrain coverage. Every mutable input
     * is defensively copied.
     *
     * @param width positive sample width
     * @param height positive sample height
     * @param biomePalette non-empty, duplicate-free namespaced biome palette
     * @param biomeIndices row-major palette indices with exactly {@code width * height} entries
     * @param terrainCoverage row-major unsigned coverage bytes, or {@code null} when unavailable
     * @param terrainCoverageResolution spatial meaning of {@code terrainCoverage}; must be
     *        {@link TerrainCoverageResolution#UNKNOWN} exactly when coverage is {@code null}
     * @throws NullPointerException if a required argument is {@code null}
     * @throws IllegalArgumentException if dimensions, buffers, palette entries, or coverage
     *         metadata are invalid
     */
    public BiomeRegion(
            int width,
            int height,
            List<String> biomePalette,
            int[] biomeIndices,
            byte[] terrainCoverage,
            TerrainCoverageResolution terrainCoverageResolution
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Biome region dimensions must be positive");
        }
        long expectedLength = (long) width * height;
        if (expectedLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Biome region contains too many samples: " + expectedLength);
        }
        this.width = width;
        this.height = height;
        this.biomePalette = validatePalette(biomePalette);
        this.biomeIndices = Objects.requireNonNull(biomeIndices, "biomeIndices").clone();
        if (this.biomeIndices.length != (int) expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength + " biome indices, got "
                    + this.biomeIndices.length);
        }
        for (int index : this.biomeIndices) {
            if (index < 0 || index >= this.biomePalette.size()) {
                throw new IllegalArgumentException("Biome palette index out of range: " + index);
            }
        }
        this.terrainCoverage = terrainCoverage == null ? null : terrainCoverage.clone();
        if (this.terrainCoverage != null && this.terrainCoverage.length != (int) expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength + " terrain coverage values, got "
                    + this.terrainCoverage.length);
        }
        this.terrainCoverageResolution = Objects.requireNonNull(
                terrainCoverageResolution, "terrainCoverageResolution");
        if ((this.terrainCoverage == null)
                != (this.terrainCoverageResolution == TerrainCoverageResolution.UNKNOWN)) {
            throw new IllegalArgumentException(
                    "Terrain coverage and its resolution must be declared together");
        }
    }

    /**
     * Returns the sample width.
     *
     * @return positive sample width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the sample height.
     *
     * @return positive sample height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the compact biome palette.
     *
     * @return immutable biome palette
     */
    public List<String> biomePalette() {
        return biomePalette;
    }

    /**
     * Reads the compact palette index at a sample.
     *
     * @param sampleX zero-based sample column
     * @param sampleZ zero-based sample row
     * @return index into {@link #biomePalette()}
     */
    public int biomePaletteIndexAt(int sampleX, int sampleZ) {
        return biomeIndices[index(sampleX, sampleZ)];
    }

    /**
     * Reads the biome identifier at a sample.
     *
     * @param sampleX zero-based sample column
     * @param sampleZ zero-based sample row
     * @return namespaced biome identifier
     */
    public String biomeIdAt(int sampleX, int sampleZ) {
        return biomePalette.get(biomePaletteIndexAt(sampleX, sampleZ));
    }

    /**
     * Copies the palette-index buffer.
     *
     * @return defensive copy of the row-major palette-index buffer
     */
    public int[] copyBiomeIndices() {
        return biomeIndices.clone();
    }

    /**
     * Reports whether coverage can be queried.
     *
     * @return whether a complete terrain-coverage layer is available
     */
    public boolean hasTerrainCoverage() {
        return terrainCoverage != null;
    }

    /**
     * Returns unsigned coverage in the inclusive range 0..255.
     *
     * @param sampleX zero-based sample column
     * @param sampleZ zero-based sample row
     * @return unsigned terrain coverage
     * @throws IllegalStateException when this region has no coverage layer
     */
    public int terrainCoverageAt(int sampleX, int sampleZ) {
        if (terrainCoverage == null) {
            throw new IllegalStateException("This biome region does not contain terrain coverage");
        }
        return Byte.toUnsignedInt(terrainCoverage[index(sampleX, sampleZ)]);
    }

    /**
     * Returns a defensive copy of terrain coverage.
     *
     * @return copied row-major coverage buffer, or {@code null} when coverage was not supplied
     */
    public byte[] copyTerrainCoverage() {
        return terrainCoverage == null ? null : terrainCoverage.clone();
    }

    /**
     * Returns the spatial interpretation of the optional terrain-coverage channel.
     *
     * @return {@link TerrainCoverageResolution#UNKNOWN} when coverage is absent; otherwise the
     *         non-unknown resolution supplied by the regional engine
     */
    public TerrainCoverageResolution terrainCoverageResolution() {
        return terrainCoverageResolution;
    }

    private int index(int sampleX, int sampleZ) {
        if (sampleX < 0 || sampleX >= width || sampleZ < 0 || sampleZ >= height) {
            throw new IndexOutOfBoundsException("Sample (" + sampleX + ", " + sampleZ
                    + ") is outside " + width + "x" + height + " region");
        }
        return sampleZ * width + sampleX;
    }

    private static List<String> validatePalette(List<String> palette) {
        Objects.requireNonNull(palette, "biomePalette");
        if (palette.isEmpty()) {
            throw new IllegalArgumentException("Biome palette must not be empty");
        }
        Set<String> seen = new HashSet<>();
        for (String biomeId : palette) {
            NamespacedId.requireValid(biomeId);
            if (!seen.add(biomeId)) {
                throw new IllegalArgumentException("Duplicate biome palette entry: " + biomeId);
            }
        }
        return List.copyOf(palette);
    }
}
