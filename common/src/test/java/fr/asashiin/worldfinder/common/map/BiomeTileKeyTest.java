package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BiomeTileKeyTest {
    @Test
    void generationProfileAndRuntimeScopeParticipateInCacheIdentity() {
        BiomeTileKey vanilla = key(
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"), false,
                Map.of("loader", "Fabric", "session", "one"));
        BiomeTileKey anotherVersion = key(
                WorldgenProfile.vanilla("1.21.2", "minecraft:normal"), false,
                Map.of("loader", "Fabric", "session", "one"));
        BiomeTileKey integrated = key(
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"), true,
                Map.of("loader", "Fabric", "session", "one"));
        BiomeTileKey anotherLoader = key(
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"), false,
                Map.of("loader", "NeoForge", "session", "one"));
        BiomeTileKey anotherSession = key(
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"), false,
                Map.of("loader", "Fabric", "session", "two"));

        assertNotEquals(vanilla, anotherVersion);
        assertNotEquals(vanilla, integrated);
        assertNotEquals(vanilla, anotherLoader);
        assertNotEquals(vanilla, anotherSession);
    }

    @Test
    void samplingProjectionParticipatesInCacheIdentityAndLegacyKeysRemainFixedY() {
        WorldgenProfile profile = WorldgenProfile.vanilla("1.21.1", "minecraft:normal");
        BiomeTileKey fixed = key(profile, false, Map.of("loader", "Fabric"));
        BiomeTileKey surface = new BiomeTileKey(
                fixed.seed(), fixed.dimension(), fixed.sampleY(), BiomeSamplingMode.SURFACE,
                fixed.sampleStep(), fixed.tileX(), fixed.tileZ(), fixed.tileSize(),
                fixed.worldgenProfile(), fixed.integratedServer(), fixed.resolverAttributes()
        );
        BiomeTileKey fixedAtAnotherHeight = new BiomeTileKey(
                fixed.seed(), fixed.dimension(), 32, BiomeSamplingMode.FIXED_Y,
                fixed.sampleStep(), fixed.tileX(), fixed.tileZ(), fixed.tileSize(),
                fixed.worldgenProfile(), fixed.integratedServer(), fixed.resolverAttributes()
        );

        assertEquals(BiomeSamplingMode.FIXED_Y, fixed.samplingMode());
        assertEquals(BiomeSamplingMode.SURFACE, surface.samplingMode());
        assertNotEquals(fixed, surface);
        assertNotEquals(fixed, fixedAtAnotherHeight);
        assertThrows(IllegalArgumentException.class, () -> new BiomeTileKey(
                42L, WorldDimension.END, 64, BiomeSamplingMode.SURFACE,
                4, 0, 0, 32, profile, false, Map.of()
        ));
    }

    private static BiomeTileKey key(
            WorldgenProfile profile,
            boolean integratedServer,
            Map<String, String> attributes
    ) {
        return new BiomeTileKey(
                42L, WorldDimension.OVERWORLD, 64, 4, 0, 0, 32,
                profile, integratedServer, attributes
        );
    }
}
