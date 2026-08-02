package fr.asashiin.worldfinder.api.world;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Loader-neutral structure marker returned by a compatibility addon.
 *
 * @param structureId namespaced identifier matching the corresponding query target
 * @param displayName non-blank user-facing marker name
 * @param blockX block-space X coordinate
 * @param blockY optional block-space Y coordinate, or {@code null} when the resolver does not
 *               know a meaningful vertical position
 * @param blockZ block-space Z coordinate
 * @param attributes immutable string metadata copied into deterministic key order
 */
public record StructureResult(
        String structureId,
        String displayName,
        int blockX,
        Integer blockY,
        int blockZ,
        Map<String, String> attributes
) {
    /** Validates and creates an immutable structure result. */
    public StructureResult {
        structureId = NamespacedId.requireValid(structureId);
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        Objects.requireNonNull(attributes, "attributes");
        TreeMap<String, String> copy = new TreeMap<>();
        attributes.forEach((key, value) -> {
            Objects.requireNonNull(key, "attribute key");
            Objects.requireNonNull(value, "attribute value");
            if (key.isBlank()) {
                throw new IllegalArgumentException("Attribute keys must not be blank");
            }
            copy.put(key, value);
        });
        attributes = Collections.unmodifiableMap(copy);
    }

    /**
     * Creates a result without additional metadata.
     *
     * @param structureId namespaced structure identifier
     * @param displayName non-blank user-facing marker name
     * @param blockX block-space X coordinate
     * @param blockZ block-space Z coordinate
     */
    public StructureResult(String structureId, String displayName, int blockX, int blockZ) {
        this(structureId, displayName, blockX, null, blockZ, Map.of());
    }

    /**
     * Creates a result with a known vertical coordinate and without additional metadata.
     *
     * @param structureId namespaced structure identifier
     * @param displayName non-blank user-facing marker name
     * @param blockX block-space X coordinate
     * @param blockY optional block-space Y coordinate, or {@code null} when unknown
     * @param blockZ block-space Z coordinate
     */
    public StructureResult(
            String structureId,
            String displayName,
            int blockX,
            Integer blockY,
            int blockZ
    ) {
        this(structureId, displayName, blockX, blockY, blockZ, Map.of());
    }

    /**
     * Creates a result without a known vertical coordinate while retaining legacy argument order.
     *
     * @param structureId namespaced structure identifier
     * @param displayName non-blank user-facing marker name
     * @param blockX block-space X coordinate
     * @param blockZ block-space Z coordinate
     * @param attributes immutable string metadata
     */
    public StructureResult(
            String structureId,
            String displayName,
            int blockX,
            int blockZ,
            Map<String, String> attributes
    ) {
        this(structureId, displayName, blockX, null, blockZ, attributes);
    }
}
