package fr.asashiin.worldfinder.common.worldgen;

import java.util.Objects;

/**
 * Composable identities and policy switches for vanilla structure generation.
 *
 * <p>The candidate placement kernel is deliberately independent from structure data, biome tags,
 * piece expansion, and block-content rules. This prevents a template or tag change from forcing a
 * duplicate implementation of random-spread or concentric-ring placement.</p>
 *
 * @param semanticsId effective structure behavior represented by the complete profile
 * @param placementSemantics random-spread, frequency, exclusion, ring, and weighted-selection rules
 * @param jigsawPolicy active Jigsaw validation/codec behavior
 * @param mansionLayout active Woodland Mansion room-selection behavior
 * @param trialChamberData active Trial Chamber pool/template generation
 * @param biomePolicy active structure-biome tag bundle
 * @param netherFossilDriedGhast whether Nether Fossils may add the post-1.21.6 Dried Ghast content
 * @param blockRotUsesProcessedState whether block-rot observes the state produced by prior processors
 */
public record StructureBehaviorProfile(
        GenerationSemanticsId semanticsId,
        GenerationSemanticsId placementSemantics,
        JigsawPolicy jigsawPolicy,
        MansionLayout mansionLayout,
        TrialChamberData trialChamberData,
        BiomePolicy biomePolicy,
        boolean netherFossilDriedGhast,
        boolean blockRotUsesProcessedState
) {
    /** Validates every structure-profile component. */
    public StructureBehaviorProfile {
        Objects.requireNonNull(semanticsId, "semanticsId");
        Objects.requireNonNull(placementSemantics, "placementSemantics");
        Objects.requireNonNull(jigsawPolicy, "jigsawPolicy");
        Objects.requireNonNull(mansionLayout, "mansionLayout");
        Objects.requireNonNull(trialChamberData, "trialChamberData");
        Objects.requireNonNull(biomePolicy, "biomePolicy");
    }

    /** Jigsaw placement behavior epochs relevant to supported vanilla data. */
    public enum JigsawPolicy {
        /** Original scalar-distance behavior used through Minecraft 1.21.3. */
        LEGACY,
        /** Adds start-piece dimension-padding validation. */
        DIMENSION_PADDING,
        /** Supports independent horizontal and vertical maximum distances. */
        AXIS_SPECIFIC_MAX_DISTANCE
    }

    /** Woodland Mansion room-selection behavior epochs. */
    public enum MansionLayout {
        /** Four selectable second-floor 1x1 room variants. */
        FOUR_SECOND_FLOOR_ROOMS,
        /** Five selectable second-floor 1x1 room variants. */
        FIVE_SECOND_FLOOR_ROOMS
    }

    /** Trial Chamber pool/template data epochs. */
    public enum TrialChamberData {
        /** Templates shipped by Minecraft 1.21.1. */
        RELEASE_1_21_1,
        /** Encounter, bed, disposal, and connector updates introduced in Minecraft 1.21.2. */
        POST_1_21_2
    }

    /** Structure-biome eligibility and stronghold-bias tag epochs. */
    public enum BiomePolicy {
        /** Original Minecraft 1.21.1 tags. */
        RELEASE_1_21_1,
        /** Initial Pale Garden eligibility from Minecraft 1.21.4. */
        PALE_GARDEN_INITIAL,
        /** Woodland Mansion eligibility added in Minecraft 1.21.5. */
        PALE_GARDEN_MANSIONS,
        /** Cherry Grove stronghold bias added in Minecraft 1.21.9. */
        CHERRY_GROVE_STRONGHOLDS,
        /** Sulfur Caves eligibility added in Minecraft 26.2. */
        SULFUR_CAVES
    }
}
