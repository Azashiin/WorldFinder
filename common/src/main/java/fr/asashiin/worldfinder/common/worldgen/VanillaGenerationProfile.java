package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.world.WorldgenProfile;

import java.util.Objects;

/**
 * Effective seed-map generation profile for one exact vanilla Minecraft release.
 *
 * @param minecraftVersion exact supported release
 * @param semanticsId combined seed-map semantics shared by output-compatible releases
 * @param biomes dimension-specific biome profile
 * @param structures structure policy/data profile
 */
public record VanillaGenerationProfile(
        String minecraftVersion,
        GenerationSemanticsId semanticsId,
        BiomeGenerationProfile biomes,
        StructureBehaviorProfile structures
) {
    /** Validates the immutable profile. */
    public VanillaGenerationProfile {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        if (minecraftVersion.isBlank()) {
            throw new IllegalArgumentException("minecraftVersion must not be blank");
        }
        Objects.requireNonNull(semanticsId, "semanticsId");
        Objects.requireNonNull(biomes, "biomes");
        Objects.requireNonNull(structures, "structures");
    }

    /**
     * Creates the public resolver profile while retaining the exact Minecraft version.
     *
     * <p>Internal semantic identities deliberately remain outside the public profile. Addons see
     * the exact Minecraft release and the historical empty vanilla property map; platform caches
     * and native engines use {@link #semanticsId()} separately.</p>
     *
     * @return immutable vanilla Normal API profile
     */
    public WorldgenProfile toApiProfile() {
        return WorldgenProfile.vanilla(minecraftVersion, "minecraft:normal");
    }
}
