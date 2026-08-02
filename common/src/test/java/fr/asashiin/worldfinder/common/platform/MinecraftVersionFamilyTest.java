package fr.asashiin.worldfinder.common.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionFamilyTest {
    @Test
    void groupsCompatiblePatchVersions() {
        assertTrue(MinecraftVersionFamily.MC_1_21_8.contains("1.21.6"));
        assertTrue(MinecraftVersionFamily.MC_1_21_8.contains("1.21.8"));
        assertFalse(MinecraftVersionFamily.MC_1_21_8.contains("1.21.9"));
        assertTrue(MinecraftVersionFamily.MC_1_21_10.contains("1.21.9"));
        assertTrue(MinecraftVersionFamily.MC_1_21_10.contains("1.21.10"));
    }

    @Test
    void selectsTheRequiredJavaGeneration() {
        assertEquals(21, MinecraftVersionFamily.MC_1_21_11.javaVersion());
        assertEquals(25, MinecraftVersionFamily.MC_26_1.javaVersion());
    }

    @Test
    void separatesTechnicalApiEpochsFromGenerationProfiles() {
        assertEquals(MinecraftVersionFamily.MC_1_21_10,
                MinecraftVersionFamily.find("1.21.9").orElseThrow());
        assertEquals(MinecraftVersionFamily.MC_1_21_11,
                MinecraftVersionFamily.find("1.21.11").orElseThrow());
        assertTrue(MinecraftVersionFamily.MC_26_1.contains("26.1.2"));
        assertTrue(MinecraftVersionFamily.find("26.2").isPresent());
        assertTrue(MinecraftVersionFamily.find("26.3").isEmpty());
    }
}
