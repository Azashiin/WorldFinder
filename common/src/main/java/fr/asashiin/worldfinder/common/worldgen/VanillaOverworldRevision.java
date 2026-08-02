package fr.asashiin.worldfinder.common.worldgen;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Effective vanilla Normal Overworld biome-generation revisions from Minecraft 1.21.1 onward.
 *
 * <p>A revision combines value changes in the plateau biome tables, ordered climate-parameter
 * topology, and cave-density data. It remains independent from mappings, Java versions, loader
 * APIs, and whether a WorldFinder binary has been compiled and verified for a Minecraft release.</p>
 */
public enum VanillaOverworldRevision {
    /** Baseline Minecraft 1.21.1 behavior, retained by the Normal preset through 1.21.3. */
    V1_21_1(
            "worldfinder:biomes/overworld_1_21_1",
            "1.21.1",
            PaleGardenPlacement.ABSENT,
            OverworldParameterTopology.BASELINE,
            OverworldCaveDensityRevision.BASELINE,
            VanillaOverworldVerticalProfiles.BASELINE
    ),
    /** Minecraft 1.21.4 narrow Pale Garden placement in the plateau variant table. */
    V1_21_4(
            "worldfinder:biomes/overworld_1_21_4",
            "1.21.4",
            PaleGardenPlacement.PLATEAU_VARIANT_T2_H4,
            OverworldParameterTopology.BASELINE,
            OverworldCaveDensityRevision.BASELINE,
            VanillaOverworldVerticalProfiles.BASELINE
    ),
    /** Minecraft 1.21.5 expanded Pale Garden placement, retained through 26.1.2. */
    V1_21_5(
            "worldfinder:biomes/overworld_1_21_5",
            "1.21.5",
            PaleGardenPlacement.PLATEAU_BASE_T2_H4,
            OverworldParameterTopology.BASELINE,
            OverworldCaveDensityRevision.BASELINE,
            VanillaOverworldVerticalProfiles.BASELINE
    ),
    /** Minecraft 26.2 Sulfur Caves point, ordered index topology, and cave-density data. */
    V26_2(
            "worldfinder:biomes/overworld_26_2",
            "26.2",
            PaleGardenPlacement.PLATEAU_BASE_T2_H4,
            OverworldParameterTopology.SULFUR_CAVES_26_2,
            OverworldCaveDensityRevision.SULFUR_CAVES_26_2,
            VanillaOverworldVerticalProfiles.SULFUR_CAVES_26_2
    );

    private static final Map<GenerationSemanticsId, VanillaOverworldRevision> BY_SEMANTICS = Map.of(
            V1_21_1.semanticsId, V1_21_1,
            V1_21_4.semanticsId, V1_21_4,
            V1_21_5.semanticsId, V1_21_5,
            V26_2.semanticsId, V26_2
    );

    private final GenerationSemanticsId semanticsId;
    private final String firstMinecraftVersion;
    private final PaleGardenPlacement paleGardenPlacement;
    private final OverworldParameterTopology parameterTopology;
    private final OverworldCaveDensityRevision caveDensityRevision;
    private final OverworldBiomeVerticalProfile verticalProfile;

    VanillaOverworldRevision(
            String semanticsId,
            String firstMinecraftVersion,
            PaleGardenPlacement paleGardenPlacement,
            OverworldParameterTopology parameterTopology,
            OverworldCaveDensityRevision caveDensityRevision,
            OverworldBiomeVerticalProfile verticalProfile
    ) {
        this.semanticsId = new GenerationSemanticsId(semanticsId);
        this.firstMinecraftVersion = firstMinecraftVersion;
        this.paleGardenPlacement = paleGardenPlacement;
        this.parameterTopology = parameterTopology;
        this.caveDensityRevision = caveDensityRevision;
        this.verticalProfile = verticalProfile;
    }

    /**
     * Returns the existing stable Overworld semantics identity.
     *
     * @return non-null semantics identity used by biome generation profiles
     */
    public GenerationSemanticsId semanticsId() {
        return semanticsId;
    }

    /**
     * Returns the first exact stable release introducing this revision.
     *
     * @return exact Minecraft version
     */
    public String firstMinecraftVersion() {
        return firstMinecraftVersion;
    }

    /**
     * Returns the Pale Garden plateau-table rule.
     *
     * @return non-null placement rule
     */
    public PaleGardenPlacement paleGardenPlacement() {
        return paleGardenPlacement;
    }

    /**
     * Returns the ordered climate-parameter topology.
     *
     * @return non-null topology revision
     */
    public OverworldParameterTopology parameterTopology() {
        return parameterTopology;
    }

    /**
     * Returns the effective Overworld cave-density data revision.
     *
     * @return non-null cave-density revision
     */
    public OverworldCaveDensityRevision caveDensityRevision() {
        return caveDensityRevision;
    }

    /** Returns the versioned build limits and useful underground map layers. */
    public OverworldBiomeVerticalProfile verticalProfile() {
        return verticalProfile;
    }

    /**
     * Resolves a known Overworld revision from an existing semantic identity.
     *
     * @param semanticsId Overworld semantic identity
     * @return matching revision, or empty for an addon or future identity
     */
    public static Optional<VanillaOverworldRevision> find(
            GenerationSemanticsId semanticsId
    ) {
        return Optional.ofNullable(BY_SEMANTICS.get(
                Objects.requireNonNull(semanticsId, "semanticsId")));
    }
}
