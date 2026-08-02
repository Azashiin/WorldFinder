package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.CancellationToken;

/** Generates immutable biome tiles for cache and rendering consumers. */
@FunctionalInterface
public interface BiomeTileGenerator {
    /**
     * Generates the complete tile identified by a key.
     *
     * <p>Implementations may be invoked concurrently and should therefore either be thread-safe
     * or explicitly coordinate access to mutable generation state.</p>
     *
     * @param key non-null tile coordinates and sampling parameters
     * @return non-null generated tile whose {@link BiomeTile#key()} equals {@code key}
     */
    BiomeTile generate(BiomeTileKey key);

    /**
     * Generates a tile while observing an asynchronous caller's cancellation signal.
     * Implementations with expensive regional work should override this method and check the
     * token at least once per output row or another bounded batch.
     *
     * @param key non-null tile coordinates and sampling parameters
     * @param cancellationToken cooperative cancellation signal
     * @return non-null generated tile
     */
    default BiomeTile generate(BiomeTileKey key, CancellationToken cancellationToken) {
        cancellationToken.throwIfCancellationRequested();
        BiomeTile tile = generate(key);
        cancellationToken.throwIfCancellationRequested();
        return tile;
    }
}
