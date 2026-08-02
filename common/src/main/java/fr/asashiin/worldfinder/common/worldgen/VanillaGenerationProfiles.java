package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.BiomePolicy;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.JigsawPolicy;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.MansionLayout;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.TrialChamberData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Collections;

/** Catalog of effective vanilla Normal generation profiles supported by WorldFinder. */
public final class VanillaGenerationProfiles {
    private static final GenerationSemanticsId OW_1_21_1 =
            VanillaOverworldRevision.V1_21_1.semanticsId();
    private static final GenerationSemanticsId OW_1_21_4 =
            VanillaOverworldRevision.V1_21_4.semanticsId();
    private static final GenerationSemanticsId OW_1_21_5 =
            VanillaOverworldRevision.V1_21_5.semanticsId();
    private static final GenerationSemanticsId OW_26_2 =
            VanillaOverworldRevision.V26_2.semanticsId();
    private static final GenerationSemanticsId NETHER_SHARED = id("biomes/nether_1_21_1");
    private static final GenerationSemanticsId END_SHARED = id("biomes/end_1_21_1");
    private static final GenerationSemanticsId STRUCTURE_PLACEMENT_SHARED =
            id("structures/placement_1_21_1");

    private static final BiomeGenerationProfile BIOMES_1_21_1 = biomes(OW_1_21_1);
    private static final BiomeGenerationProfile BIOMES_1_21_4 = biomes(OW_1_21_4);
    private static final BiomeGenerationProfile BIOMES_1_21_5 = biomes(OW_1_21_5);
    private static final BiomeGenerationProfile BIOMES_26_2 = biomes(OW_26_2);

    private static final StructureBehaviorProfile STRUCTURES_1_21_1 = structures(
            "structures/behavior_1_21_1", JigsawPolicy.LEGACY,
            MansionLayout.FOUR_SECOND_FLOOR_ROOMS, TrialChamberData.RELEASE_1_21_1,
            BiomePolicy.RELEASE_1_21_1, false, false);
    private static final StructureBehaviorProfile STRUCTURES_1_21_2 = structures(
            "structures/behavior_1_21_2", JigsawPolicy.LEGACY,
            MansionLayout.FOUR_SECOND_FLOOR_ROOMS, TrialChamberData.POST_1_21_2,
            BiomePolicy.RELEASE_1_21_1, false, false);
    private static final StructureBehaviorProfile STRUCTURES_1_21_4 = structures(
            "structures/behavior_1_21_4", JigsawPolicy.DIMENSION_PADDING,
            MansionLayout.FOUR_SECOND_FLOOR_ROOMS, TrialChamberData.POST_1_21_2,
            BiomePolicy.PALE_GARDEN_INITIAL, false, false);
    private static final StructureBehaviorProfile STRUCTURES_1_21_5 = structures(
            "structures/behavior_1_21_5", JigsawPolicy.DIMENSION_PADDING,
            MansionLayout.FOUR_SECOND_FLOOR_ROOMS, TrialChamberData.POST_1_21_2,
            BiomePolicy.PALE_GARDEN_MANSIONS, false, false);
    private static final StructureBehaviorProfile STRUCTURES_1_21_6 = structures(
            "structures/behavior_1_21_6", JigsawPolicy.DIMENSION_PADDING,
            MansionLayout.FIVE_SECOND_FLOOR_ROOMS, TrialChamberData.POST_1_21_2,
            BiomePolicy.PALE_GARDEN_MANSIONS, true, false);
    private static final StructureBehaviorProfile STRUCTURES_1_21_9 = structures(
            "structures/behavior_1_21_9", JigsawPolicy.AXIS_SPECIFIC_MAX_DISTANCE,
            MansionLayout.FIVE_SECOND_FLOOR_ROOMS, TrialChamberData.POST_1_21_2,
            BiomePolicy.CHERRY_GROVE_STRONGHOLDS, true, false);
    private static final StructureBehaviorProfile STRUCTURES_26_2 = structures(
            "structures/behavior_26_2", JigsawPolicy.AXIS_SPECIFIC_MAX_DISTANCE,
            MansionLayout.FIVE_SECOND_FLOOR_ROOMS, TrialChamberData.POST_1_21_2,
            BiomePolicy.SULFUR_CAVES, true, true);

    private static final Map<String, VanillaGenerationProfile> BY_VERSION = createProfiles();

    private VanillaGenerationProfiles() {
    }

    /**
     * Looks up an exact stable Minecraft release.
     *
     * @param minecraftVersion exact release string
     * @return profile when the release is supported
     */
    public static Optional<VanillaGenerationProfile> find(String minecraftVersion) {
        return Optional.ofNullable(BY_VERSION.get(minecraftVersion));
    }

    /**
     * Requires an exact stable Minecraft release.
     *
     * @param minecraftVersion exact release string
     * @return supported generation profile
     * @throws NoSuchElementException when no profile is declared for the release
     */
    public static VanillaGenerationProfile require(String minecraftVersion) {
        VanillaGenerationProfile profile = BY_VERSION.get(minecraftVersion);
        if (profile == null) {
            throw new NoSuchElementException(
                    "Unsupported vanilla generation version: " + minecraftVersion);
        }
        return profile;
    }

    /**
     * Returns all exact stable releases in chronological declaration order.
     *
     * @return immutable version list
     */
    public static List<String> supportedVersions() {
        return List.copyOf(BY_VERSION.keySet());
    }

    /**
     * Returns all exact profiles in chronological declaration order.
     *
     * @return immutable profile list
     */
    public static List<VanillaGenerationProfile> profiles() {
        return List.copyOf(BY_VERSION.values());
    }

    private static Map<String, VanillaGenerationProfile> createProfiles() {
        LinkedHashMap<String, VanillaGenerationProfile> profiles = new LinkedHashMap<>();
        add(profiles, "1.21.1", "vanilla_normal/1_21_1", BIOMES_1_21_1, STRUCTURES_1_21_1);
        add(profiles, "1.21.2", "vanilla_normal/1_21_2", BIOMES_1_21_1, STRUCTURES_1_21_2);
        add(profiles, "1.21.3", "vanilla_normal/1_21_2", BIOMES_1_21_1, STRUCTURES_1_21_2);
        add(profiles, "1.21.4", "vanilla_normal/1_21_4", BIOMES_1_21_4, STRUCTURES_1_21_4);
        add(profiles, "1.21.5", "vanilla_normal/1_21_5", BIOMES_1_21_5, STRUCTURES_1_21_5);
        add(profiles, "1.21.6", "vanilla_normal/1_21_6", BIOMES_1_21_5, STRUCTURES_1_21_6);
        add(profiles, "1.21.7", "vanilla_normal/1_21_6", BIOMES_1_21_5, STRUCTURES_1_21_6);
        add(profiles, "1.21.8", "vanilla_normal/1_21_6", BIOMES_1_21_5, STRUCTURES_1_21_6);
        add(profiles, "1.21.9", "vanilla_normal/1_21_9", BIOMES_1_21_5, STRUCTURES_1_21_9);
        add(profiles, "1.21.10", "vanilla_normal/1_21_9", BIOMES_1_21_5, STRUCTURES_1_21_9);
        add(profiles, "1.21.11", "vanilla_normal/1_21_9", BIOMES_1_21_5, STRUCTURES_1_21_9);
        add(profiles, "26.1", "vanilla_normal/1_21_9", BIOMES_1_21_5, STRUCTURES_1_21_9);
        add(profiles, "26.1.1", "vanilla_normal/1_21_9", BIOMES_1_21_5, STRUCTURES_1_21_9);
        add(profiles, "26.1.2", "vanilla_normal/1_21_9", BIOMES_1_21_5, STRUCTURES_1_21_9);
        add(profiles, "26.2", "vanilla_normal/26_2", BIOMES_26_2, STRUCTURES_26_2);
        return Collections.unmodifiableMap(profiles);
    }

    private static void add(
            Map<String, VanillaGenerationProfile> profiles,
            String version,
            String semantics,
            BiomeGenerationProfile biomes,
            StructureBehaviorProfile structures
    ) {
        VanillaGenerationProfile previous = profiles.put(
                version,
                new VanillaGenerationProfile(version, id(semantics), biomes, structures)
        );
        if (previous != null) {
            throw new IllegalStateException("Duplicate vanilla generation profile: " + version);
        }
    }

    private static BiomeGenerationProfile biomes(GenerationSemanticsId overworld) {
        return new BiomeGenerationProfile(overworld, NETHER_SHARED, END_SHARED);
    }

    private static StructureBehaviorProfile structures(
            String semantics,
            JigsawPolicy jigsawPolicy,
            MansionLayout mansionLayout,
            TrialChamberData trialChamberData,
            BiomePolicy biomePolicy,
            boolean netherFossilDriedGhast,
            boolean blockRotUsesProcessedState
    ) {
        return new StructureBehaviorProfile(
                id(semantics), STRUCTURE_PLACEMENT_SHARED, jigsawPolicy, mansionLayout,
                trialChamberData, biomePolicy, netherFossilDriedGhast,
                blockRotUsesProcessedState
        );
    }

    private static GenerationSemanticsId id(String path) {
        return new GenerationSemanticsId("worldfinder:" + path);
    }
}
