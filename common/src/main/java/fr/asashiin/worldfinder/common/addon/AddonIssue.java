package fr.asashiin.worldfinder.common.addon;

import java.util.Objects;

/**
 * Structured diagnostic emitted when third-party addon code fails at a runtime boundary.
 *
 * @param sourceId resolver identifier or provider class associated with the failure
 * @param operation isolated operation which failed
 * @param failure original non-fatal failure
 */
public record AddonIssue(String sourceId, Operation operation, Throwable failure) {
    /** Validates and creates an immutable diagnostic. */
    public AddonIssue {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(failure, "failure");
    }

    /**
     * Formats a concise message suitable for a platform logger.
     *
     * @return human-readable message without a stack trace
     */
    public String message() {
        String detail = failure.getMessage();
        return "WorldFinder addon " + sourceId + " failed during " + operation.serializedName()
                + (detail == null || detail.isBlank() ? "" : ": " + detail);
    }

    /** Runtime boundary at which addon code failed. */
    public enum Operation {
        /** Service descriptor iteration or provider type discovery. */
        SERVICE_DISCOVERY("service discovery"),
        /** Construction of one discovered service provider. */
        SERVICE_INSTANTIATION("service instantiation"),
        /** Transactional registration of an addon. */
        REGISTRATION("registration"),
        /** Coarse resolver applicability check. */
        SUPPORT_CHECK("support check"),
        /** Single-position biome resolution. */
        BIOME_QUERY("point biome query"),
        /** Batched regional biome resolution. */
        BIOME_REGION_QUERY("regional biome query"),
        /** Batched structure resolution. */
        STRUCTURE_QUERY("structure query");

        private final String serializedName;

        Operation(String serializedName) {
            this.serializedName = serializedName;
        }

        /**
         * Returns a stable, user-readable operation name.
         *
         * @return lowercase operation label
         */
        public String serializedName() {
            return serializedName;
        }
    }
}
