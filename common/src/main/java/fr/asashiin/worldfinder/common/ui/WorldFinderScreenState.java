package fr.asashiin.worldfinder.common.ui;

import fr.asashiin.worldfinder.common.map.MapViewport;
import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable user-visible state of the WorldFinder map screen.
 *
 * @param seed selected world seed
 * @param dimension selected map dimension
 * @param viewport map centre and zoom
 * @param selectedBiomes immutable identifiers selected for biome filtering
 * @param selectedStructures immutable identifiers selected for structure filtering
 * @param showBiomeColors whether biome colours are visible
 * @param showStructures whether structure markers are visible
 * @param showChunkBorders whether chunk/grid borders are visible
 * @param showSlimeChunks whether slime-chunk highlighting is visible
 */
public record WorldFinderScreenState(
        long seed,
        WorldDimension dimension,
        MapViewport viewport,
        Set<String> selectedBiomes,
        Set<String> selectedStructures,
        boolean showBiomeColors,
        boolean showStructures,
        boolean showChunkBorders,
        boolean showSlimeChunks
) {
    /**
     * Creates a screen state and snapshots both selection sets.
     *
     * @throws NullPointerException if the dimension, viewport, either selection set, or a selected
     *         identifier is {@code null}
     */
    public WorldFinderScreenState {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(viewport, "viewport");
        selectedBiomes = Set.copyOf(selectedBiomes);
        selectedStructures = Set.copyOf(selectedStructures);
    }

    /**
     * Creates the initial Overworld screen state centred on the world origin.
     *
     * @return default state with biome colours and structures enabled
     */
    public static WorldFinderScreenState defaults() {
        return new WorldFinderScreenState(
                0L,
                WorldDimension.OVERWORLD,
                new MapViewport(0.0, 0.0, 4.0),
                Set.of(),
                Set.of(),
                true,
                true,
                false,
                false
        );
    }
}
