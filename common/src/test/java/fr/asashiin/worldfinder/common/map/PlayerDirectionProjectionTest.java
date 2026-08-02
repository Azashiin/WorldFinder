package fr.asashiin.worldfinder.common.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDirectionProjectionTest {
    @Test
    void convertsMinecraftYawToMapArrowRotation() {
        assertEquals(180.0F, PlayerDirectionProjection.arrowRotationDegrees(0.0F));
        assertEquals(270.0F, PlayerDirectionProjection.arrowRotationDegrees(90.0F));
        assertEquals(90.0F, PlayerDirectionProjection.arrowRotationDegrees(-90.0F));
        assertEquals(0.0F, PlayerDirectionProjection.arrowRotationDegrees(180.0F));
    }

    @Test
    void normalizesYawOutsideOneFullTurn() {
        assertEquals(270.0F, PlayerDirectionProjection.arrowRotationDegrees(450.0F));
        assertEquals(90.0F, PlayerDirectionProjection.arrowRotationDegrees(-450.0F));
    }
}
