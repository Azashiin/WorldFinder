package fr.asashiin.worldfinder.common.platform;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Build-compatible groups of Minecraft releases and the Java toolchain required by each group.
 */
public enum MinecraftVersionFamily {
    /** Minecraft 1.21.1, using Java 21. */
    MC_1_21_1("A0", "1.21.1", 21, List.of("1.21.1")),
    /** Minecraft 1.21.2 through 1.21.3, using Java 21. */
    MC_1_21_3("A1", "1.21.2-1.21.3", 21, List.of("1.21.2", "1.21.3")),
    /** Minecraft 1.21.4, using Java 21. */
    MC_1_21_4("A2", "1.21.4", 21, List.of("1.21.4")),
    /** Minecraft 1.21.5, using Java 21. */
    MC_1_21_5("A3", "1.21.5", 21, List.of("1.21.5")),
    /** Minecraft 1.21.6 through 1.21.8, using Java 21. */
    MC_1_21_8("A4", "1.21.6-1.21.8", 21,
            List.of("1.21.6", "1.21.7", "1.21.8")),
    /** Minecraft 1.21.9 through 1.21.10, using Java 21 and event-based GUI input. */
    MC_1_21_10("A5", "1.21.9-1.21.10", 21,
            List.of("1.21.9", "1.21.10")),
    /** Minecraft 1.21.11, using Java 21 and the Identifier API epoch. */
    MC_1_21_11("A6", "1.21.11", 21, List.of("1.21.11")),
    /** Minecraft 26.1 through 26.1.2, using Java 25. */
    MC_26_1("A7", "26.1-26.1.2", 25, List.of("26.1", "26.1.1", "26.1.2")),
    /** Minecraft 26.2, using Java 25. */
    MC_26_2("A8", "26.2", 25, List.of("26.2"));

    private final String catalogId;
    private final String displayName;
    private final int javaVersion;
    private final List<String> minecraftVersions;

    MinecraftVersionFamily(
            String catalogId,
            String displayName,
            int javaVersion,
            List<String> minecraftVersions
    ) {
        this.catalogId = catalogId;
        this.displayName = displayName;
        this.javaVersion = javaVersion;
        this.minecraftVersions = List.copyOf(minecraftVersions);
    }

    /**
     * Returns the stable API-family identifier used by the build target catalog.
     *
     * @return non-null catalog family identifier
     */
    public String catalogId() {
        return catalogId;
    }

    /**
     * Returns the compact version range shown to users and build tooling.
     *
     * @return non-null display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the Java language/runtime version required by this family.
     *
     * @return Java feature version
     */
    public int javaVersion() {
        return javaVersion;
    }

    /**
     * Returns the exact Minecraft versions represented by this family.
     *
     * @return immutable, non-empty version list
     */
    public List<String> minecraftVersions() {
        return minecraftVersions;
    }

    /**
     * Tests whether an exact Minecraft version belongs to this family.
     *
     * @param minecraftVersion exact version string to test
     * @return {@code true} when the version is a member of this family
     */
    public boolean contains(String minecraftVersion) {
        return minecraftVersions.contains(minecraftVersion);
    }

    /**
     * Finds the technical build family containing an exact Minecraft release.
     *
     * @param minecraftVersion exact release string
     * @return matching family, or empty when no adapter family is declared
     */
    public static Optional<MinecraftVersionFamily> find(String minecraftVersion) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        for (MinecraftVersionFamily family : values()) {
            if (family.contains(minecraftVersion)) {
                return Optional.of(family);
            }
        }
        return Optional.empty();
    }
}
