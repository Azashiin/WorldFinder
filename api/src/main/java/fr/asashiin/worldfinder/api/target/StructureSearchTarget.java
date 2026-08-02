package fr.asashiin.worldfinder.api.target;

import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.Objects;

/**
 * Searchable structure metadata contributed by WorldFinder or an addon.
 *
 * @param id unique namespaced structure identifier
 * @param displayName non-blank user-facing name
 * @param dimension dimension in which the structure may be searched
 */
public record StructureSearchTarget(String id, String displayName, WorldDimension dimension) {
    /** Validates and creates immutable structure metadata. */
    public StructureSearchTarget {
        id = NamespacedId.requireValid(id);
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        Objects.requireNonNull(dimension, "dimension");
    }
}
