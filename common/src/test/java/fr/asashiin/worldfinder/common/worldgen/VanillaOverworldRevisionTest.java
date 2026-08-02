package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.common.worldgen.PaleGardenPlacement.PlateauCell;
import fr.asashiin.worldfinder.common.worldgen.PaleGardenPlacement.PlateauTable;
import fr.asashiin.worldfinder.common.worldgen.SulfurCavesClimatePointFixture.ParameterListInsertion;
import fr.asashiin.worldfinder.common.worldgen.SulfurCavesClimatePointFixture.QuantizedClimatePoint;
import fr.asashiin.worldfinder.common.worldgen.SulfurCavesClimatePointFixture.QuantizedRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaOverworldRevisionTest {
    @Test
    void declaresTheFourExactOverworldBreakpointsAndCatalogGroups() {
        assertEquals(List.of(
                "1.21.1", "1.21.4", "1.21.5", "26.2"
        ), List.of(VanillaOverworldRevision.values()).stream()
                .map(VanillaOverworldRevision::firstMinecraftVersion)
                .toList());

        assertRevision(VanillaOverworldRevision.V1_21_1,
                "1.21.1", "1.21.2", "1.21.3");
        assertRevision(VanillaOverworldRevision.V1_21_4, "1.21.4");
        assertRevision(VanillaOverworldRevision.V1_21_5,
                "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10",
                "1.21.11", "26.1", "26.1.1", "26.1.2");
        assertRevision(VanillaOverworldRevision.V26_2, "26.2");
    }

    @Test
    void modelsTheTwoExactPaleGardenTableChanges() {
        assertTrue(VanillaOverworldRevision.V1_21_1
                .paleGardenPlacement().plateauCell().isEmpty());

        PlateauCell initial = VanillaOverworldRevision.V1_21_4
                .paleGardenPlacement().plateauCell().orElseThrow();
        assertEquals(PlateauTable.VARIANT, initial.table());
        assertEquals(2, initial.temperatureIndex());
        assertEquals(4, initial.humidityIndex());

        PlateauCell expanded = VanillaOverworldRevision.V1_21_5
                .paleGardenPlacement().plateauCell().orElseThrow();
        assertEquals(PlateauTable.BASE, expanded.table());
        assertEquals(2, expanded.temperatureIndex());
        assertEquals(4, expanded.humidityIndex());
        assertEquals(expanded, VanillaOverworldRevision.V26_2
                .paleGardenPlacement().plateauCell().orElseThrow());

        assertEquals(OverworldParameterTopology.BASELINE,
                VanillaOverworldRevision.V1_21_4.parameterTopology());
        assertEquals(OverworldParameterTopology.BASELINE,
                VanillaOverworldRevision.V1_21_5.parameterTopology());
        assertEquals(OverworldCaveDensityRevision.BASELINE,
                VanillaOverworldRevision.V1_21_5.caveDensityRevision());
    }

    @Test
    void capturesTheExactSulfurCavesPointBoundsAndOrder() {
        QuantizedClimatePoint point = SulfurCavesClimatePointFixture.POINT;
        assertEquals(10_000, SulfurCavesClimatePointFixture.QUANTIZATION_FACTOR);
        assertEquals(new QuantizedRange(-10_000L, 10_000L), point.temperature());
        assertEquals(new QuantizedRange(-10_000L, 10_000L), point.humidity());
        assertEquals(new QuantizedRange(-1_900L, 10_000L), point.continentalness());
        assertEquals(new QuantizedRange(4_500L, 10_000L), point.erosion());
        assertEquals(new QuantizedRange(2_000L, 9_000L), point.depth());
        assertEquals(new QuantizedRange(-11_000L, -8_500L), point.weirdness());
        assertEquals(0L, point.offset());
        assertEquals("minecraft:sulfur_caves", point.biomeId());

        ParameterListInsertion insertion = SulfurCavesClimatePointFixture.INSERTION;
        assertEquals("minecraft:lush_caves", insertion.afterBiomeId());
        assertEquals("minecraft:deep_dark", insertion.beforeBiomeId());

        VanillaOverworldRevision revision = VanillaOverworldRevision.V26_2;
        assertEquals(OverworldParameterTopology.SULFUR_CAVES_26_2,
                revision.parameterTopology());
        assertSame(point, revision.parameterTopology().addedPoint().orElseThrow());
        assertSame(insertion, revision.parameterTopology().insertion().orElseThrow());
        assertEquals(OverworldCaveDensityRevision.SULFUR_CAVES_26_2,
                revision.caveDensityRevision());
        assertEquals(Set.of(-51),
                revision.verticalProfile().relevantLevels("minecraft:sulfur_caves"));
    }

    @Test
    void retainsAnOpenEndedBiomeProfileConstructorForUnknownSemantics() {
        GenerationSemanticsId custom = new GenerationSemanticsId("example:overworld_future");
        BiomeGenerationProfile profile = new BiomeGenerationProfile(
                custom,
                new GenerationSemanticsId("example:nether"),
                new GenerationSemanticsId("example:end")
        );

        assertTrue(profile.knownOverworldRevision().isEmpty());
        assertFalse(VanillaOverworldRevision.find(custom).isPresent());
    }

    @Test
    void validatesQuantizedIntervalsAndFiveByFivePlateauBounds() {
        assertThrows(IllegalArgumentException.class, () -> new QuantizedRange(1L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new PlateauCell(PlateauTable.BASE, -1, 4));
        assertThrows(IllegalArgumentException.class,
                () -> new PlateauCell(PlateauTable.VARIANT, 2, 5));
    }

    private static void assertRevision(
            VanillaOverworldRevision expected,
            String... versions
    ) {
        for (String version : versions) {
            BiomeGenerationProfile biomes = VanillaGenerationProfiles.require(version).biomes();
            assertEquals(expected, biomes.knownOverworldRevision().orElseThrow(), version);
            assertEquals(expected.semanticsId(), biomes.overworld(), version);
        }
    }
}
