package fr.asashiin.worldfinder.api.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Loader-neutral, immutable information supplied to addon resolvers. */
public final class WorldgenContext {
    private final long seed;
    private final WorldDimension dimension;
    private final WorldgenProfile profile;
    private final boolean integratedServer;
    private final Map<String, String> attributes;

    /**
     * Legacy constructor retained for source and binary compatibility with the 0.2 previews.
     * New platform code should use {@link #WorldgenContext(long, WorldDimension, WorldgenProfile,
     * boolean, Map)} so resolvers can select the correct versioned generation rules.
     *
     * @param seed complete world seed
     * @param dimension queried dimension
     * @param worldPresetId namespaced world-preset identifier
     * @param integratedServer whether the context comes from a local integrated server
     * @param attributes runtime query metadata copied into deterministic key order
     */
    public WorldgenContext(
            long seed,
            WorldDimension dimension,
            String worldPresetId,
            boolean integratedServer,
            Map<String, String> attributes
    ) {
        this(seed, dimension, WorldgenProfile.unspecified(worldPresetId), integratedServer, attributes);
    }

    /**
     * Creates a fully identified resolver context.
     *
     * @param seed complete world seed
     * @param dimension queried dimension
     * @param profile immutable versioned world-generation profile
     * @param integratedServer whether the context comes from a local integrated server
     * @param attributes runtime query metadata copied into deterministic key order
     */
    public WorldgenContext(
            long seed,
            WorldDimension dimension,
            WorldgenProfile profile,
            boolean integratedServer,
            Map<String, String> attributes
    ) {
        this.seed = seed;
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.integratedServer = integratedServer;
        this.attributes = immutableSortedMap(attributes);
    }

    /**
     * Returns the seed.
     *
     * @return complete world seed
     */
    public long seed() {
        return seed;
    }

    /**
     * Returns the dimension.
     *
     * @return non-null queried dimension
     */
    public WorldDimension dimension() {
        return dimension;
    }

    /**
     * Returns the versioned generation profile.
     *
     * @return immutable versioned world-generation profile
     */
    public WorldgenProfile profile() {
        return profile;
    }

    /**
     * Compatibility accessor equivalent to {@code profile().worldPresetId()}.
     *
     * @return namespaced preset identifier
     */
    public String worldPresetId() {
        return profile.worldPresetId();
    }

    /**
     * Returns the Minecraft version.
     *
     * @return Minecraft version from the profile
     */
    public String minecraftVersion() {
        return profile.minecraftVersion();
    }

    /**
     * Returns the profile identifier.
     *
     * @return namespaced profile identifier
     */
    public String profileId() {
        return profile.id();
    }

    /**
     * Reports the source server type.
     *
     * @return whether the context represents a local integrated server
     */
    public boolean integratedServer() {
        return integratedServer;
    }

    /**
     * Returns runtime-only attributes.
     *
     * @return unmodifiable runtime attributes in deterministic key order
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorldgenContext that)) {
            return false;
        }
        return seed == that.seed
                && integratedServer == that.integratedServer
                && dimension == that.dimension
                && profile.equals(that.profile)
                && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seed, dimension, profile, integratedServer, attributes);
    }

    @Override
    public String toString() {
        return "WorldgenContext[seed=" + seed
                + ", dimension=" + dimension
                + ", profile=" + profile
                + ", integratedServer=" + integratedServer
                + ", attributes=" + attributes + ']';
    }

    private static Map<String, String> immutableSortedMap(Map<String, String> values) {
        Objects.requireNonNull(values, "attributes");
        TreeMap<String, String> copy = new TreeMap<>();
        values.forEach((key, value) -> {
            Objects.requireNonNull(key, "attribute key");
            Objects.requireNonNull(value, "attribute value");
            if (key.isBlank()) {
                throw new IllegalArgumentException("Attribute keys must not be blank");
            }
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
