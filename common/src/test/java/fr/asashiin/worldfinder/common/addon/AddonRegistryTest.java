package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.WorldFinderAddon;
import fr.asashiin.worldfinder.api.WorldFinderRegistrar;
import fr.asashiin.worldfinder.api.target.BiomeSearchTarget;
import fr.asashiin.worldfinder.api.target.StructureSearchTarget;
import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddonRegistryTest {
    @Test
    void addonCanRegisterAWorldgenBiome() {
        AddonRegistry registry = new AddonRegistry();
        registry.registerAddon(new TestAddon());
        registry.freeze();

        assertEquals(1, registry.addons().size());
        assertEquals("terralith:alpha_islands", registry.biomes().getFirst().id());
        assertSame(registry.addons(), registry.addons());
        assertSame(registry.biomes(), registry.biomes());
        assertThrows(IllegalStateException.class, () -> registry.registerAddon(new TestAddon()));
    }

    @Test
    void failedAddonRegistrationIsRolledBack() {
        AddonRegistry registry = new AddonRegistry();

        assertThrows(IllegalStateException.class, () -> registry.registerAddon(new FailingAddon()));
        assertEquals(0, registry.addons().size());
        assertEquals(0, registry.biomes().size());
    }

    @Test
    void resolversUseSnapshottedPriorityAndDeterministicIdTieBreak() {
        AddonRegistry registry = new AddonRegistry();
        MutablePriorityResolver middle = new MutablePriorityResolver("test:middle", 10);
        MutablePriorityResolver beta = new MutablePriorityResolver("test:beta", 20);
        MutablePriorityResolver alpha = new MutablePriorityResolver("test:alpha", 20);
        registry.registerResolver(middle);
        registry.registerResolver(beta);
        registry.registerResolver(alpha);
        middle.priority = 100;

        assertEquals(List.of("test:alpha", "test:beta", "test:middle"), registry.resolvers().stream()
                .map(WorldgenResolver::id).toList());
    }

    @Test
    void duplicateAndInvalidResolverIdsAreRejected() {
        AddonRegistry registry = new AddonRegistry();
        registry.registerResolver(new MutablePriorityResolver("test:valid", 0));

        assertThrows(IllegalArgumentException.class,
                () -> registry.registerResolver(new MutablePriorityResolver("test:valid", 1)));
        assertThrows(IllegalArgumentException.class,
                () -> registry.registerResolver(new MutablePriorityResolver("INVALID", 1)));
    }

    @Test
    void customStructureIdsMustBeUniqueAndCannotUseTheReservedNamespace() {
        AddonRegistry registry = new AddonRegistry();
        StructureSearchTarget target = new StructureSearchTarget(
                "example:ruin", "Example Ruin", WorldDimension.OVERWORLD
        );
        registry.registerStructure(target);

        assertThrows(IllegalArgumentException.class, () -> registry.registerStructure(target));
        assertThrows(IllegalArgumentException.class, () -> registry.registerStructure(
                new StructureSearchTarget(
                        "worldfinder:village", "Reserved Override", WorldDimension.OVERWORLD
                )
        ));
        assertThrows(IllegalArgumentException.class, () -> registry.registerStructure(
                new StructureSearchTarget(
                        "minecraft:village", "Native Collision", WorldDimension.OVERWORLD
                )
        ));
        assertEquals(List.of(target), registry.structures());
    }

    @Test
    void addonMetadataCannotReplaceNativeBiomeTargetsOrWorldFinderResolvers() {
        AddonRegistry registry = new AddonRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.registerBiome(
                new BiomeSearchTarget(
                        "minecraft:plains", "Misleading Plains", WorldDimension.OVERWORLD,
                        BiomeSearchTarget.Layer.SURFACE, 0xFF000000
                )
        ));
        assertThrows(IllegalArgumentException.class, () -> registry.registerBiome(
                new BiomeSearchTarget(
                        "worldfinder:internal", "Reserved", WorldDimension.END,
                        BiomeSearchTarget.Layer.SURFACE, 0xFF000000
                )
        ));
        assertThrows(IllegalArgumentException.class, () -> registry.registerResolver(
                new MutablePriorityResolver("worldfinder:internal", 0)
        ));
        assertEquals(List.of(), registry.biomes());
        assertEquals(List.of(), registry.resolvers());
    }

    @Test
    void cancellationRollsBackPartialRegistrationBeforeEscaping() {
        AddonRegistry registry = new AddonRegistry();
        WorldFinderAddon cancelling = new WorldFinderAddon() {
            @Override
            public String id() {
                return "cancelling_addon";
            }

            @Override
            public void register(WorldFinderRegistrar registrar) {
                registrar.registerBiome(new BiomeSearchTarget(
                        "example:partial_cancelled",
                        "Partial",
                        WorldDimension.OVERWORLD,
                        BiomeSearchTarget.Layer.SURFACE,
                        0xFF000000
                ));
                throw new CancellationException("cancel bootstrap");
            }
        };

        assertThrows(CancellationException.class, () -> registry.registerAddon(cancelling));
        assertEquals(List.of(), registry.addons());
        assertEquals(List.of(), registry.biomes());
    }

    private static final class TestAddon implements WorldFinderAddon {
        @Override
        public String id() {
            return "terralith_compat";
        }

        @Override
        public void register(WorldFinderRegistrar registrar) {
            registrar.registerBiome(new BiomeSearchTarget(
                    "terralith:alpha_islands",
                    "Alpha Islands",
                    WorldDimension.OVERWORLD,
                    BiomeSearchTarget.Layer.SURFACE,
                    0xFF638B6B
            ));
        }
    }

    private static final class FailingAddon implements WorldFinderAddon {
        @Override
        public String id() {
            return "broken_compat";
        }

        @Override
        public void register(WorldFinderRegistrar registrar) {
            registrar.registerBiome(new BiomeSearchTarget(
                    "example:partial",
                    "Partial",
                    WorldDimension.OVERWORLD,
                    BiomeSearchTarget.Layer.SURFACE,
                    0xFF000000
            ));
            throw new IllegalStateException("Simulated addon failure");
        }
    }

    private static final class MutablePriorityResolver implements WorldgenResolver {
        private final String id;
        private int priority;

        private MutablePriorityResolver(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean supports(WorldgenContext context) {
            return true;
        }
    }
}
