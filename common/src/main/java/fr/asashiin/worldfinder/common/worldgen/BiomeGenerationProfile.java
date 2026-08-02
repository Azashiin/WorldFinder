package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.world.WorldDimension;

import java.util.Objects;
import java.util.Optional;

/**
 * Dimension-specific identities of the effective vanilla biome-selection data.
 *
 * @param overworld Overworld climate-table and wired-noise behavior
 * @param nether Nether climate-table and wired-noise behavior
 * @param end End biome-classification behavior
 */
public record BiomeGenerationProfile(
        GenerationSemanticsId overworld,
        GenerationSemanticsId nether,
        GenerationSemanticsId end
) {
    /** Validates all dimension profiles. */
    public BiomeGenerationProfile {
        Objects.requireNonNull(overworld, "overworld");
        Objects.requireNonNull(nether, "nether");
        Objects.requireNonNull(end, "end");
    }

    /**
     * Returns the effective biome semantics for one vanilla dimension.
     *
     * @param dimension queried dimension
     * @return non-null semantics identity
     */
    public GenerationSemanticsId forDimension(WorldDimension dimension) {
        return switch (Objects.requireNonNull(dimension, "dimension")) {
            case OVERWORLD -> overworld;
            case NETHER -> nether;
            case END -> end;
        };
    }

    /**
     * Resolves the structured vanilla Overworld revision represented by this profile.
     *
     * <p>Custom and future semantic identities remain valid record values and return an empty
     * result, preserving the existing open-ended constructor contract.</p>
     *
     * @return known vanilla revision, or empty for an unrecognized Overworld identity
     */
    public Optional<VanillaOverworldRevision> knownOverworldRevision() {
        return VanillaOverworldRevision.find(overworld);
    }
}
