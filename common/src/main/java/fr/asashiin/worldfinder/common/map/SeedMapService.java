package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous boundary for seed-based biome and structure map data.
 *
 * <p>Implementations decide how generation work is scheduled. Returned futures may complete
 * exceptionally when the requested world-generation profile is unavailable.</p>
 */
public interface SeedMapService {
    /**
     * Samples the biome at one block position.
     *
     * @param seed world seed
     * @param dimension dimension to sample
     * @param blockX block X coordinate
     * @param blockY block Y coordinate
     * @param blockZ block Z coordinate
     * @return future containing the sample, or an empty optional when it cannot be resolved
     */
    CompletableFuture<Optional<BiomeSample>> biomeAt(
            long seed,
            WorldDimension dimension,
            int blockX,
            int blockY,
            int blockZ
    );

    /**
     * Finds structure markers intersecting the visible map extent.
     *
     * @param seed world seed
     * @param dimension dimension to search
     * @param viewport viewport centre and scale
     * @param pixelWidth visible width in pixels
     * @param pixelHeight visible height in pixels
     * @return future containing structure markers for the requested extent
     */
    CompletableFuture<List<StructureMarker>> structuresIn(
            long seed,
            WorldDimension dimension,
            MapViewport viewport,
            int pixelWidth,
            int pixelHeight
    );

    /**
     * Generates a square, row-major array of biome colours.
     *
     * @param seed world seed
     * @param dimension dimension to sample
     * @param originBlockX block X coordinate of the first sample
     * @param originBlockZ block Z coordinate of the first sample
     * @param sampleStep positive distance in blocks between adjacent samples
     * @param tileSize positive width and height of the tile in samples
     * @return future containing {@code tileSize * tileSize} ARGB colours in row-major order
     */
    CompletableFuture<int[]> biomeTile(
            long seed,
            WorldDimension dimension,
            int originBlockX,
            int originBlockZ,
            int sampleStep,
            int tileSize
    );
}
