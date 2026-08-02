package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.world.WorldgenProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeWorldgenContractTest {
    private static final NativeWorldgenContract CONTRACT =
            NativeWorldgenContract.exactVanilla("1.21.1");

    @Test
    void acceptsOnlyTheExactVerifiedVanillaRuntime() {
        GenerationSemanticsId semantics = CONTRACT.requireSemantics(
                WorldgenProfile.vanilla("1.21.1", "minecraft:normal"));

        assertEquals(VanillaGenerationProfiles.require("1.21.1").semanticsId(), semantics);
        assertTrue(CONTRACT.supports(WorldgenProfile.vanilla("1.21.1", "minecraft:normal")));
        assertFalse(CONTRACT.supports(WorldgenProfile.vanilla("1.21.2", "minecraft:normal")));
    }

    @Test
    void keepsTheLegacyUnspecifiedProfileAsACompiledTargetAlias() {
        assertEquals(
                VanillaGenerationProfiles.require("1.21.1").semanticsId(),
                CONTRACT.requireSemantics(WorldgenProfile.unspecified("minecraft:normal"))
        );
    }

    @Test
    void refusesCustomPresetIdentityAndProperties() {
        assertThrows(UnsupportedWorldgenProfileException.class,
                () -> CONTRACT.requireSemantics(
                        WorldgenProfile.vanilla("1.21.1", "minecraft:large_biomes")));
        assertThrows(UnsupportedWorldgenProfileException.class,
                () -> CONTRACT.requireSemantics(new WorldgenProfile(
                        "terralith:default", "1.21.1", "minecraft:normal", Map.of())));
        assertThrows(UnsupportedWorldgenProfileException.class,
                () -> CONTRACT.requireSemantics(new WorldgenProfile(
                        "worldfinder:vanilla", "1.21.1", "minecraft:normal",
                        Map.of("worldfinder.test", "changed"))));
    }

    @Test
    void requiresTheCompiledVersionToBeVerified() {
        GenerationSemanticsId semantics =
                VanillaGenerationProfiles.require("1.21.1").semanticsId();
        assertThrows(IllegalArgumentException.class,
                () -> new NativeWorldgenContract("1.21.1", Map.of("1.21.2", semantics)));
    }
}
