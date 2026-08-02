package fr.asashiin.worldfinder.api.target;

import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.Objects;

/**
 * Searchable biome metadata contributed by WorldFinder or an addon.
 *
 * @param id unique namespaced biome identifier
 * @param displayName non-blank user-facing name
 * @param dimension dimension in which the target may be searched
 * @param layer broad vertical layer used for filtering
 * @param argbColor packed, non-premultiplied ARGB map color ({@code 0xAARRGGBB})
 */
public record BiomeSearchTarget(
        String id,
        String displayName,
        WorldDimension dimension,
        Layer layer,
        int argbColor
) {
    /** Validates and creates immutable biome metadata. */
    public BiomeSearchTarget {
        id = NamespacedId.requireValid(id);
        displayName = requireDisplayName(displayName);
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(layer, "layer");
    }

    private static String requireDisplayName(String value) {
        Objects.requireNonNull(value, "displayName");
        if (value.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        return value;
    }

    /** Coarse vertical category used by the search UI. */
    public enum Layer {
        /** Biome represented on the primary surface map. */
        SURFACE,
        /** Biome found below or inside the primary surface. */
        UNDERGROUND
    }
}
