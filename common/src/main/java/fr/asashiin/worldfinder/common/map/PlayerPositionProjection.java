package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.Objects;
import java.util.Optional;

/** Projects the live player position onto a dimension map when vanilla coordinates are linked. */
public final class PlayerPositionProjection {
    private PlayerPositionProjection() {
    }

    /**
     * Projects a player position into a map dimension when the coordinate systems are linked.
     *
     * <p>Positions remain unchanged within the same dimension. Overworld positions are divided by
     * the Nether coordinate scale when projected onto a Nether map; Nether positions are multiplied
     * by the same scale for an Overworld map. Other cross-dimension pairs do not have a meaningful
     * vanilla projection.</p>
     *
     * @param blockX player block X coordinate
     * @param blockZ player block Z coordinate
     * @param playerDimension dimension containing the player
     * @param mapDimension dimension displayed by the map
     * @return projected position, or an empty optional when no projection is defined
     * @throws NullPointerException if either dimension is {@code null}
     */
    public static Optional<Position> project(double blockX, double blockZ,
                                             WorldDimension playerDimension, WorldDimension mapDimension) {
        Objects.requireNonNull(playerDimension, "playerDimension");
        Objects.requireNonNull(mapDimension, "mapDimension");
        if (playerDimension == mapDimension) {
            return Optional.of(new Position(blockX, blockZ));
        }
        if (playerDimension == WorldDimension.OVERWORLD && mapDimension == WorldDimension.NETHER) {
            double scale = WorldDimension.NETHER.overworldCoordinateScale();
            return Optional.of(new Position(blockX / scale, blockZ / scale));
        }
        if (playerDimension == WorldDimension.NETHER && mapDimension == WorldDimension.OVERWORLD) {
            double scale = WorldDimension.NETHER.overworldCoordinateScale();
            return Optional.of(new Position(blockX * scale, blockZ * scale));
        }
        return Optional.empty();
    }

    /**
     * Projected horizontal world position.
     *
     * @param blockX projected block X coordinate
     * @param blockZ projected block Z coordinate
     */
    public record Position(double blockX, double blockZ) {
    }
}
