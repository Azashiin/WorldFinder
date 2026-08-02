package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenProfile;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.BiomePolicy;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.JigsawPolicy;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.MansionLayout;
import fr.asashiin.worldfinder.common.worldgen.StructureBehaviorProfile.TrialChamberData;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaGenerationProfilesTest {
    @Test
    void declaresEveryStableReleaseFrom1_21_1Through26_2() {
        assertEquals(List.of(
                "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6",
                "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
                "26.1", "26.1.1", "26.1.2", "26.2"
        ), VanillaGenerationProfiles.supportedVersions());
    }

    @Test
    void usesExactlyFourOverworldBiomeProfiles() {
        Set<GenerationSemanticsId> profiles = new HashSet<>();
        VanillaGenerationProfiles.profiles().forEach(profile ->
                profiles.add(profile.biomes().forDimension(WorldDimension.OVERWORLD)));

        assertEquals(4, profiles.size());
        assertEquals(profile("1.21.1").biomes(), profile("1.21.3").biomes());
        assertNotEquals(profile("1.21.3").biomes(), profile("1.21.4").biomes());
        assertNotEquals(profile("1.21.4").biomes(), profile("1.21.5").biomes());
        assertEquals(profile("1.21.5").biomes(), profile("26.1.2").biomes());
        assertNotEquals(profile("26.1.2").biomes(), profile("26.2").biomes());
    }

    @Test
    void sharesNetherEndAndStructurePlacementAcrossTheWholeRange() {
        Set<GenerationSemanticsId> nether = new HashSet<>();
        Set<GenerationSemanticsId> end = new HashSet<>();
        Set<GenerationSemanticsId> placement = new HashSet<>();
        VanillaGenerationProfiles.profiles().forEach(profile -> {
            nether.add(profile.biomes().nether());
            end.add(profile.biomes().end());
            placement.add(profile.structures().placementSemantics());
        });

        assertEquals(1, nether.size());
        assertEquals(1, end.size());
        assertEquals(1, placement.size());
    }

    @Test
    void composesIndependentStructureBreakpoints() {
        assertEquals(TrialChamberData.RELEASE_1_21_1,
                profile("1.21.1").structures().trialChamberData());
        assertEquals(TrialChamberData.POST_1_21_2,
                profile("1.21.2").structures().trialChamberData());
        assertEquals(JigsawPolicy.DIMENSION_PADDING,
                profile("1.21.4").structures().jigsawPolicy());
        assertEquals(BiomePolicy.PALE_GARDEN_MANSIONS,
                profile("1.21.5").structures().biomePolicy());
        assertEquals(MansionLayout.FIVE_SECOND_FLOOR_ROOMS,
                profile("1.21.6").structures().mansionLayout());
        assertEquals(JigsawPolicy.AXIS_SPECIFIC_MAX_DISTANCE,
                profile("1.21.9").structures().jigsawPolicy());
        assertEquals(BiomePolicy.SULFUR_CAVES,
                profile("26.2").structures().biomePolicy());
        assertTrue(profile("1.21.6").structures().netherFossilDriedGhast());
        assertFalse(profile("26.1.2").structures().blockRotUsesProcessedState());
        assertTrue(profile("26.2").structures().blockRotUsesProcessedState());
    }

    @Test
    void keepsExactVersionAndInternalSemanticsSeparateFromTheAddonContract() {
        WorldgenProfile apiProfile = profile("1.21.7").toApiProfile();

        assertEquals("1.21.7", apiProfile.minecraftVersion());
        assertEquals("worldfinder:vanilla", apiProfile.id());
        assertEquals("minecraft:normal", apiProfile.worldPresetId());
        assertTrue(apiProfile.properties().isEmpty());
        assertEquals("worldfinder:vanilla_normal/1_21_6",
                profile("1.21.7").semanticsId().value());
        assertEquals("worldfinder:biomes/overworld_1_21_5",
                profile("1.21.7").biomes().overworld().value());
        assertEquals("worldfinder:structures/placement_1_21_1",
                profile("1.21.7").structures().placementSemantics().value());
    }

    @Test
    void refusesUndeclaredOrSnapshotVersions() {
        assertTrue(VanillaGenerationProfiles.find("26.2").isPresent());
        assertFalse(VanillaGenerationProfiles.find("26.2.1").isPresent());
        assertThrows(NoSuchElementException.class,
                () -> VanillaGenerationProfiles.require("26.3-snapshot-1"));
    }

    private static VanillaGenerationProfile profile(String version) {
        return VanillaGenerationProfiles.require(version);
    }
}
