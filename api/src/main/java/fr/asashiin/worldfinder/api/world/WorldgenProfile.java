package fr.asashiin.worldfinder.api.world;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Stable identity of the declared base world-generation rules for a query.
 *
 * <p>A profile is separate from a seed and dimension so a resolver can distinguish the Minecraft
 * version, base preset, and any generation properties explicitly identified by the platform.
 * It is not an inventory of every installed mod or remote-server datapack: addon resolvers compose
 * over this baseline and perform their own applicability checks. Properties must affect generation
 * and remain stable for the lifetime of the declared profile.</p>
 *
 * @param id namespaced identity of the declared generation baseline
 * @param minecraftVersion non-blank Minecraft version or snapshot identifier
 * @param worldPresetId namespaced world-preset identifier
 * @param properties generation-affecting properties; copied into deterministic key order
 */
public record WorldgenProfile(
        String id,
        String minecraftVersion,
        String worldPresetId,
        Map<String, String> properties
) {
    /** Stable profile identity used for WorldFinder's declared vanilla generation baseline. */
    public static final String VANILLA_PROFILE_ID = "worldfinder:vanilla";
    /** Vanilla Normal world-preset identity used by WorldFinder's built-in engine. */
    public static final String VANILLA_NORMAL_PRESET_ID = "minecraft:normal";
    /** Version marker used only when a legacy caller omitted the Minecraft version. */
    public static final String UNKNOWN_MINECRAFT_VERSION = "unknown";
    /** Profile marker used only when a legacy caller omitted the profile identity. */
    public static final String UNSPECIFIED_PROFILE_ID = "worldfinder:unspecified";

    /** Validates and creates an immutable generation profile. */
    public WorldgenProfile {
        id = NamespacedId.requireValid(id);
        minecraftVersion = requireVersion(minecraftVersion);
        worldPresetId = NamespacedId.requireValid(worldPresetId);
        properties = immutableSortedMap(properties, "profile properties");
    }

    /**
     * Creates a profile for a vanilla preset on a known Minecraft version.
     *
     * @param minecraftVersion Minecraft release or snapshot identifier
     * @param worldPresetId namespaced vanilla preset identifier
     * @return immutable vanilla profile
     */
    public static WorldgenProfile vanilla(String minecraftVersion, String worldPresetId) {
        return new WorldgenProfile(VANILLA_PROFILE_ID, minecraftVersion, worldPresetId, Map.of());
    }

    /**
     * Compatibility profile used by the legacy {@link WorldgenContext} constructor.
     * Platform adapters should provide a fully identified profile for new integrations.
     *
     * @param worldPresetId namespaced preset identifier
     * @return immutable profile marked as unspecified
     */
    public static WorldgenProfile unspecified(String worldPresetId) {
        return new WorldgenProfile(
                UNSPECIFIED_PROFILE_ID,
                UNKNOWN_MINECRAFT_VERSION,
                worldPresetId,
                Map.of()
        );
    }

    private static String requireVersion(String version) {
        Objects.requireNonNull(version, "minecraftVersion");
        if (version.isEmpty() || version.length() > 64) {
            throw new IllegalArgumentException("Minecraft version must contain between 1 and 64 characters");
        }
        for (int index = 0; index < version.length(); index++) {
            char character = version.charAt(index);
            boolean valid = character >= 'a' && character <= 'z'
                    || character >= 'A' && character <= 'Z'
                    || character >= '0' && character <= '9'
                    || character == '.' || character == '-' || character == '_' || character == '+';
            if (!valid) {
                throw new IllegalArgumentException("Invalid Minecraft version: " + version);
            }
        }
        return version;
    }

    private static Map<String, String> immutableSortedMap(Map<String, String> values, String name) {
        Objects.requireNonNull(values, name);
        TreeMap<String, String> copy = new TreeMap<>();
        values.forEach((key, value) -> {
            Objects.requireNonNull(key, name + " key");
            Objects.requireNonNull(value, name + " value");
            if (key.isBlank()) {
                throw new IllegalArgumentException(name + " keys must not be blank");
            }
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
