package fr.asashiin.worldfinder.api.waypoint;

import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.Objects;

/**
 * Loader-neutral waypoint requested from an optional map-mod integration.
 *
 * @param name non-blank user-facing waypoint name
 * @param dimension destination dimension
 * @param blockX block-space X coordinate
 * @param blockY block-space Y coordinate
 * @param blockZ block-space Z coordinate
 * @param color packed, non-premultiplied ARGB color ({@code 0xAARRGGBB})
 */
public record WaypointRequest(
        String name,
        WorldDimension dimension,
        int blockX,
        int blockY,
        int blockZ,
        int color
) {
    /** Validates and creates an immutable waypoint request. */
    public WaypointRequest {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(dimension, "dimension");
    }
}
