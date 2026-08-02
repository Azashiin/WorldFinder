package fr.asashiin.worldfinder.common.map;

/**
 * Immutable horizontal position in Minecraft block coordinates.
 *
 * @param blockX block coordinate on the east/west axis
 * @param blockZ block coordinate on the south/north axis
 */
public record MapPosition(int blockX, int blockZ) {
}
