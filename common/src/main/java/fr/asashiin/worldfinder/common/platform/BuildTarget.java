package fr.asashiin.worldfinder.common.platform;

import java.util.Objects;

/**
 * One supported Minecraft-version family and mod-loader build combination.
 *
 * @param versionFamily Minecraft version family compiled by the target
 * @param loader mod loader compiled by the target
 */
public record BuildTarget(MinecraftVersionFamily versionFamily, ModLoader loader) {
    /**
     * Creates and validates a build target.
     *
     * @throws NullPointerException if either component is {@code null}
     */
    public BuildTarget {
        Objects.requireNonNull(versionFamily, "versionFamily");
        Objects.requireNonNull(loader, "loader");
    }

    /**
     * Returns a deterministic lowercase identifier derived from the enum constant names.
     *
     * @return identifier in {@code version-family-loader} form
     */
    public String id() {
        return versionFamily.name().toLowerCase() + "-" + loader.name().toLowerCase();
    }
}
