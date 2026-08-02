package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.TerrainCoverageResolution;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable biome tile with palette-compressed biome identifiers. */
public final class BiomeTile {
    /** Unsigned coverage value representing sampled void. */
    public static final int VOID_COVERAGE = 0;
    /** Unsigned coverage value representing sampled terrain. */
    public static final int FULL_TERRAIN_COVERAGE = 255;

    private final BiomeTileKey key;
    private final int[] colors;
    private final String[] biomePalette;
    private final byte[] bytePaletteIndices;
    private final short[] shortPaletteIndices;
    private final byte[] terrainCoverage;
    private final TerrainCoverageResolution terrainCoverageResolution;

    /**
     * Creates a tile containing colours only, with unknown biome identifiers and terrain coverage.
     *
     * @param key tile coordinates and sampling parameters
     * @param colors one ARGB colour per tile pixel, in row-major order
     */
    public BiomeTile(BiomeTileKey key, int[] colors) {
        this(key, colors, new String[colors.length], null, TerrainCoverageResolution.UNKNOWN);
    }

    /**
     * Creates a tile with biome identifiers and no measured terrain-coverage channel.
     *
     * @param key tile coordinates and sampling parameters
     * @param colors one ARGB colour per tile pixel, in row-major order
     * @param biomeIds one biome identifier per tile pixel, in row-major order
     */
    public BiomeTile(BiomeTileKey key, int[] colors, String[] biomeIds) {
        this(key, colors, biomeIds, null, TerrainCoverageResolution.UNKNOWN);
    }

    /**
     * Creates a biome tile with a sample-point terrain-coverage channel.
     *
     * <p>The biome identifier remains meaningful when coverage is zero. Coverage is an unsigned
     * byte: {@code 0} is void, {@code 255} is fully covered terrain, and intermediate values are
     * reserved for area coverage at coarser levels of detail. Keeping both channels separate
     * prevents rendering choices such as a black void from becoming fake biome identifiers.</p>
     *
     * @param key tile coordinates and sampling parameters
     * @param colors one ARGB colour per tile pixel, in row-major order
     * @param biomeIds one biome identifier per tile pixel, in row-major order
     * @param terrainCoverage one unsigned-byte coverage value per pixel, in row-major order
     */
    public BiomeTile(BiomeTileKey key, int[] colors, String[] biomeIds, byte[] terrainCoverage) {
        this(key, colors, biomeIds, terrainCoverage, TerrainCoverageResolution.SAMPLE_POINT);
    }

    /**
     * Creates a tile with an explicitly described terrain-coverage channel.
     *
     * @param key tile coordinates and sampling parameters
     * @param colors one ARGB colour per tile pixel, in row-major order
     * @param biomeIds one biome identifier per tile pixel, in row-major order
     * @param terrainCoverage one unsigned-byte coverage value per pixel, or {@code null} when unknown
     * @param terrainCoverageResolution spatial meaning of {@code terrainCoverage}; must be
     *        {@link TerrainCoverageResolution#UNKNOWN} exactly when the array is {@code null}
     * @throws NullPointerException if a required argument is {@code null}
     * @throws IllegalArgumentException if array sizes do not match the tile or coverage metadata is inconsistent
     */
    public BiomeTile(BiomeTileKey key, int[] colors, String[] biomeIds, byte[] terrainCoverage,
                     TerrainCoverageResolution terrainCoverageResolution) {
        this.key = Objects.requireNonNull(key, "key");
        Objects.requireNonNull(colors, "colors");
        Objects.requireNonNull(biomeIds, "biomeIds");
        Objects.requireNonNull(terrainCoverageResolution, "terrainCoverageResolution");
        int expected = Math.multiplyExact(key.tileSize(), key.tileSize());
        if (colors.length != expected || biomeIds.length != expected
                || terrainCoverage != null && terrainCoverage.length != expected) {
            throw new IllegalArgumentException("Unexpected biome tile data count");
        }
        if ((terrainCoverage == null) != (terrainCoverageResolution == TerrainCoverageResolution.UNKNOWN)) {
            throw new IllegalArgumentException("Terrain coverage and its resolution must be declared together");
        }

        this.colors = Arrays.copyOf(colors, colors.length);
        this.terrainCoverage = terrainCoverage == null
                ? null
                : Arrays.copyOf(terrainCoverage, terrainCoverage.length);
        this.terrainCoverageResolution = terrainCoverageResolution;
        String[] biomeSnapshot = Arrays.copyOf(biomeIds, biomeIds.length);
        short[] paletteIndicesByPixel = new short[biomeSnapshot.length];
        Map<String, Integer> paletteIndices = new LinkedHashMap<>();
        for (int index = 0; index < biomeSnapshot.length; index++) {
            Integer paletteIndex = paletteIndices.get(biomeSnapshot[index]);
            if (paletteIndex == null && !paletteIndices.containsKey(biomeSnapshot[index])) {
                paletteIndex = paletteIndices.size();
                if (paletteIndex > 0xFFFF) {
                    throw new IllegalArgumentException("Too many distinct biomes in one tile");
                }
                paletteIndices.put(biomeSnapshot[index], paletteIndex);
            }
            paletteIndicesByPixel[index] = (short)(int)paletteIndex;
        }
        this.biomePalette = paletteIndices.keySet().toArray(String[]::new);
        if (biomePalette.length <= 256) {
            this.bytePaletteIndices = new byte[paletteIndicesByPixel.length];
            for (int index = 0; index < paletteIndicesByPixel.length; index++) {
                bytePaletteIndices[index] = (byte)paletteIndicesByPixel[index];
            }
            this.shortPaletteIndices = null;
        } else {
            this.bytePaletteIndices = null;
            this.shortPaletteIndices = paletteIndicesByPixel;
        }
    }

    /**
     * Returns the coordinates and sampling parameters represented by this tile.
     *
     * @return immutable tile key
     */
    public BiomeTileKey key() {
        return key;
    }

    /**
     * Returns all pixel colours in row-major order.
     *
     * @return defensive copy of the ARGB colour array
     */
    public int[] colors() {
        return Arrays.copyOf(colors, colors.length);
    }

    /**
     * Expands the internal biome palette into one identifier per pixel.
     *
     * @return newly allocated row-major biome identifier array
     */
    public String[] biomeIds() {
        String[] biomeIds = new String[colors.length];
        for (int index = 0; index < biomeIds.length; index++) {
            biomeIds[index] = biomeId(index);
        }
        return biomeIds;
    }

    /**
     * Returns whether this tile carries a terrain/void sample for every map pixel.
     *
     * @return {@code true} when the coverage resolution is
     *         {@link TerrainCoverageResolution#SAMPLE_POINT}
     */
    public boolean hasExactTerrainCoverage() {
        return terrainCoverageResolution == TerrainCoverageResolution.SAMPLE_POINT;
    }

    /**
     * Returns whether any measured terrain/void channel is available.
     *
     * @return {@code true} when coverage data is present
     */
    public boolean hasTerrainCoverage() {
        return terrainCoverageResolution != TerrainCoverageResolution.UNKNOWN;
    }

    /**
     * Returns the spatial interpretation of the terrain-coverage channel.
     *
     * @return non-null coverage resolution
     */
    public TerrainCoverageResolution terrainCoverageResolution() {
        return terrainCoverageResolution;
    }

    /**
     * Returns a defensive copy of the unsigned-byte terrain coverage.
     *
     * <p>Tiles produced by legacy or non-terrain-aware generators return full coverage while
     * {@link #hasExactTerrainCoverage()} remains {@code false}. Renderers can therefore remain
     * backwards compatible without mistaking the fallback for measured data.</p>
     *
     * @return defensive copy of coverage values, or a full-coverage fallback when no channel exists
     */
    public byte[] terrainCoverage() {
        if (terrainCoverage != null) return Arrays.copyOf(terrainCoverage, terrainCoverage.length);
        byte[] fullCoverage = new byte[colors.length];
        Arrays.fill(fullCoverage, (byte)FULL_TERRAIN_COVERAGE);
        return fullCoverage;
    }

    /**
     * Returns the ARGB colour at tile-local coordinates.
     *
     * @param x horizontal tile-local sample coordinate
     * @param z vertical tile-local sample coordinate
     * @return ARGB colour at the requested pixel
     * @throws IndexOutOfBoundsException if either coordinate lies outside the tile
     */
    public int color(int x, int z) {
        return color(index(x, z));
    }

    /**
     * Returns the biome identifier at tile-local coordinates.
     *
     * @param x horizontal tile-local sample coordinate
     * @param z vertical tile-local sample coordinate
     * @return biome identifier, potentially {@code null} for a colour-only tile
     * @throws IndexOutOfBoundsException if either coordinate lies outside the tile
     */
    public String biomeId(int x, int z) {
        return biomeId(index(x, z));
    }

    /**
     * Returns unsigned terrain coverage at tile-local coordinates.
     *
     * @param x horizontal tile-local sample coordinate
     * @param z vertical tile-local sample coordinate
     * @return value in the inclusive range {@code 0..255}; full coverage when no channel exists
     * @throws IndexOutOfBoundsException if either coordinate lies outside the tile
     */
    public int terrainCoverage(int x, int z) {
        return terrainCoverage(index(x, z));
    }

    /**
     * Returns a pixel colour by row-major index.
     *
     * @param index pixel index
     * @return ARGB colour at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} lies outside the tile data
     */
    public int color(int index) {
        return colors[index];
    }

    /**
     * Returns a biome identifier by row-major index.
     *
     * @param index pixel index
     * @return biome identifier, potentially {@code null} for a colour-only tile
     * @throws IndexOutOfBoundsException if {@code index} lies outside the tile data
     */
    public String biomeId(int index) {
        int paletteIndex = bytePaletteIndices == null
                ? Short.toUnsignedInt(shortPaletteIndices[index])
                : Byte.toUnsignedInt(bytePaletteIndices[index]);
        return biomePalette[paletteIndex];
    }

    /**
     * Returns terrain coverage as an unsigned value in the inclusive range {@code 0..255}.
     *
     * @param index row-major pixel index
     * @return measured coverage, or {@link #FULL_TERRAIN_COVERAGE} when no channel exists
     * @throws IndexOutOfBoundsException if {@code index} lies outside the tile data
     */
    public int terrainCoverage(int index) {
        return terrainCoverage == null
                ? FULL_TERRAIN_COVERAGE
                : Byte.toUnsignedInt(terrainCoverage[index]);
    }

    private int index(int x, int z) {
        if (x < 0 || x >= key.tileSize() || z < 0 || z >= key.tileSize()) {
            throw new IndexOutOfBoundsException("Biome tile coordinate outside tile");
        }
        return z * key.tileSize() + x;
    }
}
