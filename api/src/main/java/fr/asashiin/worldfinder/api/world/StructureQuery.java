package fr.asashiin.worldfinder.api.world;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.Objects;

/**
 * Inclusive block-space structure search area requested from a compatibility addon.
 *
 * @param context immutable world-generation context
 * @param targetId namespaced identifier of the requested structure
 * @param minBlockX inclusive minimum block-space X coordinate
 * @param maxBlockX inclusive maximum block-space X coordinate
 * @param minBlockZ inclusive minimum block-space Z coordinate
 * @param maxBlockZ inclusive maximum block-space Z coordinate
 * @param cancellationToken cooperative cancellation signal for this potentially expensive search
 */
public record StructureQuery(
        WorldgenContext context,
        String targetId,
        int minBlockX,
        int maxBlockX,
        int minBlockZ,
        int maxBlockZ,
        CancellationToken cancellationToken
) {
    /** Validates and creates a structure query. */
    public StructureQuery {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(cancellationToken, "cancellationToken");
        targetId = NamespacedId.requireValid(targetId);
        if (minBlockX > maxBlockX || minBlockZ > maxBlockZ) {
            throw new IllegalArgumentException("Structure query bounds must be ordered");
        }
    }

    /**
     * Creates a non-cancellable query for source compatibility with API 0.2 consumers.
     *
     * @param context immutable world-generation context
     * @param targetId namespaced identifier of the requested structure
     * @param minBlockX inclusive minimum block-space X coordinate
     * @param maxBlockX inclusive maximum block-space X coordinate
     * @param minBlockZ inclusive minimum block-space Z coordinate
     * @param maxBlockZ inclusive maximum block-space Z coordinate
     */
    public StructureQuery(
            WorldgenContext context,
            String targetId,
            int minBlockX,
            int maxBlockX,
            int minBlockZ,
            int maxBlockZ
    ) {
        this(context, targetId, minBlockX, maxBlockX, minBlockZ, maxBlockZ, CancellationToken.NONE);
    }

    /**
     * Tests whether a marker is inside this query's inclusive bounds.
     *
     * @param blockX marker block-space X coordinate
     * @param blockZ marker block-space Z coordinate
     * @return {@code true} when both coordinates fall inside the query
     */
    public boolean contains(int blockX, int blockZ) {
        return blockX >= minBlockX && blockX <= maxBlockX
                && blockZ >= minBlockZ && blockZ <= maxBlockZ;
    }
}
