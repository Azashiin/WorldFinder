package fr.asashiin.worldfinder.common.ui;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.common.map.BiomeVerticalSelection;
import fr.asashiin.worldfinder.common.worldgen.VanillaOverworldRevision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeVerticalControlTest {
    @Test
    void overworldButtonCyclesInBothDirectionsAndExplainsItsControls() {
        var profile = VanillaOverworldRevision.V1_21_1.verticalProfile();
        BiomeVerticalSelection surface = BiomeVerticalSelection.surface();

        assertEquals(BiomeVerticalSelection.fixedY(-51), BiomeVerticalControl.click(
                profile, WorldDimension.OVERWORLD, surface,
                BiomeVerticalControl.PointerButton.LEFT));
        assertEquals(BiomeVerticalSelection.fixedY(64), BiomeVerticalControl.click(
                profile, WorldDimension.OVERWORLD, surface,
                BiomeVerticalControl.PointerButton.RIGHT));

        BiomeVerticalControl.State state = BiomeVerticalControl.state(
                profile, WorldDimension.OVERWORLD, BiomeVerticalSelection.fixedY(-51));
        assertTrue(state.active());
        assertEquals("Underground", state.label());
        assertEquals(BiomeVerticalControl.OVERWORLD_TOOLTIP, state.tooltip());

        assertEquals("Surface", BiomeVerticalControl.state(
                profile, WorldDimension.OVERWORLD,
                BiomeVerticalSelection.fixedY(64)).label());
        assertEquals("Highlight", BiomeVerticalControl.state(
                profile, WorldDimension.OVERWORLD,
                BiomeVerticalSelection.surface()).label());
    }

    @Test
    void netherAndEndKeepTheRememberedValueButNeverReactToClicks() {
        var profile = VanillaOverworldRevision.V26_2.verticalProfile();
        BiomeVerticalSelection remembered = BiomeVerticalSelection.fixedY(64);

        for (WorldDimension dimension : new WorldDimension[]{WorldDimension.NETHER, WorldDimension.END}) {
            BiomeVerticalControl.State state = BiomeVerticalControl.state(
                    profile, dimension, remembered);
            assertFalse(state.active());
            assertEquals("Surface", state.label());
            assertEquals(BiomeVerticalControl.OTHER_DIMENSION_TOOLTIP, state.tooltip());
            assertEquals(remembered, BiomeVerticalControl.click(
                    profile, dimension, remembered, BiomeVerticalControl.PointerButton.LEFT));
            assertEquals(remembered, BiomeVerticalControl.click(
                    profile, dimension, remembered, BiomeVerticalControl.PointerButton.RIGHT));
        }
    }
}
