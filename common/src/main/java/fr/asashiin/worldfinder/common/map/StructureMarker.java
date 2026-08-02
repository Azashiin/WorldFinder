package fr.asashiin.worldfinder.common.map;

import java.util.Objects;

/**
 * Presentation-ready location of a discovered structure.
 *
 * @param structureId non-null structure identifier
 * @param label non-null display label
 * @param position horizontal structure position
 * @param blockY optional vertical block coordinate, or {@code null} when unknown
 */
public record StructureMarker(String structureId, String label, MapPosition position, Integer blockY) {
    /**
     * Creates and validates a structure marker.
     *
     * @throws NullPointerException if {@code structureId}, {@code label}, or {@code position} is {@code null}
     */
    public StructureMarker {
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(position, "position");
    }

    /**
     * Creates a structure marker without a known vertical coordinate.
     *
     * @param structureId non-null structure identifier
     * @param label non-null display label
     * @param position horizontal structure position
     * @throws NullPointerException if an argument is {@code null}
     */
    public StructureMarker(String structureId, String label, MapPosition position) {
        this(structureId, label, position, null);
    }
}
