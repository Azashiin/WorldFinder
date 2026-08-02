package fr.asashiin.worldfinder.common.platform;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Source-compatible native world-generation binding families. */
public enum MinecraftWorldgenBindingFamily {
    /** Minecraft 1.21.1 bootstrap and registry APIs. */
    W0(List.of("1.21.1")),
    /** Minecraft 1.21.2 through 1.21.8 bootstrap, registry, and End-noise APIs. */
    W1(List.of(
            "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
            "1.21.7", "1.21.8")),
    /** Minecraft 1.21.9 through 1.21.10, with the immutable empty Beardifier API. */
    W2(List.of("1.21.9", "1.21.10")),
    /** Minecraft 1.21.11 bootstrap and registry APIs. */
    W3(List.of("1.21.11")),
    /** Minecraft 26.1 through 26.1.2 bootstrap and registry APIs. */
    W4(List.of("26.1", "26.1.1", "26.1.2")),
    /** Minecraft 26.2 bootstrap and registry APIs. */
    W5(List.of("26.2"));

    private final List<String> minecraftVersions;

    MinecraftWorldgenBindingFamily(List<String> minecraftVersions) {
        this.minecraftVersions = List.copyOf(minecraftVersions);
    }

    /**
     * Returns the exact Minecraft releases represented by this binding source.
     *
     * @return immutable, non-empty release list
     */
    public List<String> minecraftVersions() {
        return minecraftVersions;
    }

    /**
     * Finds the binding source family for one exact release.
     *
     * @param minecraftVersion exact release string
     * @return matching binding family, or empty when unsupported
     */
    public static Optional<MinecraftWorldgenBindingFamily> find(String minecraftVersion) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        for (MinecraftWorldgenBindingFamily family : values()) {
            if (family.minecraftVersions.contains(minecraftVersion)) {
                return Optional.of(family);
            }
        }
        return Optional.empty();
    }
}
