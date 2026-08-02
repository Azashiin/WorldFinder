package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPositionProjectionTest {
    @Test
    void keepsCoordinatesInThePlayersCurrentDimension() {
        PlayerPositionProjection.Position position = PlayerPositionProjection.project(
                128.0D, -64.0D, WorldDimension.NETHER, WorldDimension.NETHER).orElseThrow();

        assertEquals(128.0D, position.blockX());
        assertEquals(-64.0D, position.blockZ());
    }

    @Test
    void projectsTheOverworldPlayerPositionOntoTheNetherMap() {
        PlayerPositionProjection.Position position = PlayerPositionProjection.project(
                800.0D, -1_600.0D, WorldDimension.OVERWORLD, WorldDimension.NETHER).orElseThrow();

        assertEquals(100.0D, position.blockX());
        assertEquals(-200.0D, position.blockZ());
    }

    @Test
    void projectsTheNetherPlayerPositionOntoTheOverworldMap() {
        PlayerPositionProjection.Position position = PlayerPositionProjection.project(
                100.0D, -200.0D, WorldDimension.NETHER, WorldDimension.OVERWORLD).orElseThrow();

        assertEquals(800.0D, position.blockX());
        assertEquals(-1_600.0D, position.blockZ());
    }

    @Test
    void doesNotInventAProjectionForTheEnd() {
        assertTrue(PlayerPositionProjection.project(
                800.0D, -1_600.0D, WorldDimension.OVERWORLD, WorldDimension.END).isEmpty());
    }
}
