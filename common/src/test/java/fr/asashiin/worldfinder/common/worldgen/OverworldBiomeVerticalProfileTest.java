package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.target.BiomeSearchTarget;
import fr.asashiin.worldfinder.common.map.BiomeVerticalSelection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverworldBiomeVerticalProfileTest {
    @Test
    void everySupportedVersionUsesLayersInsideItsDeclaredOverworldLimits() {
        for (String version : VanillaGenerationProfiles.supportedVersions()) {
            OverworldBiomeVerticalProfile profile = VanillaGenerationProfiles.require(version)
                    .biomes().knownOverworldRevision().orElseThrow().verticalProfile();

            assertEquals(-64, profile.minYInclusive(), version);
            assertEquals(320, profile.maxYExclusive(), version);
            assertEquals(List.of(64, -51), profile.fixedYLevels(), version);
            assertEquals(3, profile.selections().size(), version);
            assertTrue(profile.fixedYLevels().stream().allMatch(
                    y -> y >= profile.minYInclusive() && y < profile.maxYExclusive()), version);
        }
    }

    @Test
    void cyclesOnlyBetweenHighlightSurfaceAndUnderground() {
        OverworldBiomeVerticalProfile profile = VanillaOverworldRevision.V1_21_1.verticalProfile();
        BiomeVerticalSelection highlight = BiomeVerticalSelection.surface();
        BiomeVerticalSelection surface = BiomeVerticalSelection.fixedY(64);
        BiomeVerticalSelection underground = BiomeVerticalSelection.fixedY(-51);

        assertEquals(underground, profile.next(highlight));
        assertEquals(underground, profile.previous(surface));
        assertEquals(surface, profile.previous(highlight));
        assertEquals(surface, profile.next(underground));
        assertEquals("Highlight", profile.displayName(highlight));
        assertEquals("Surface", profile.displayName(surface));
        assertEquals("Underground", profile.displayName(underground));
        assertEquals(-51, profile.undergroundY());
    }

    @Test
    void undergroundCatalogAvailabilityFollowsTheSelectedProfileLayer() {
        OverworldBiomeVerticalProfile baseline = VanillaOverworldRevision.V1_21_1.verticalProfile();
        OverworldBiomeVerticalProfile sulfur = VanillaOverworldRevision.V26_2.verticalProfile();

        assertFalse(baseline.isBiomeAvailable(
                "minecraft:lush_caves", BiomeSearchTarget.Layer.UNDERGROUND,
                BiomeVerticalSelection.surface()));
        assertTrue(baseline.isBiomeAvailable(
                "minecraft:lush_caves", BiomeSearchTarget.Layer.UNDERGROUND,
                BiomeVerticalSelection.fixedY(64)));
        assertFalse(baseline.isBiomeAvailable(
                "minecraft:deep_dark", BiomeSearchTarget.Layer.UNDERGROUND,
                BiomeVerticalSelection.fixedY(64)));
        assertTrue(baseline.isBiomeAvailable(
                "minecraft:deep_dark", BiomeSearchTarget.Layer.UNDERGROUND,
                BiomeVerticalSelection.fixedY(-51)));

        assertEquals(List.of(), baseline.relevantLevels("minecraft:sulfur_caves").stream().toList());
        assertEquals(
                java.util.Set.of(-51),
                sulfur.relevantLevels("minecraft:sulfur_caves"));
        assertFalse(sulfur.isBiomeAvailable(
                "minecraft:sulfur_caves", BiomeSearchTarget.Layer.UNDERGROUND,
                BiomeVerticalSelection.surface()));
        assertTrue(sulfur.isBiomeAvailable(
                "minecraft:sulfur_caves", BiomeSearchTarget.Layer.UNDERGROUND,
                BiomeVerticalSelection.fixedY(-51)));
    }
}
