package fr.asashiin.worldfinder.api;

import fr.asashiin.worldfinder.api.target.BiomeSearchTarget;
import fr.asashiin.worldfinder.api.target.StructureSearchTarget;
import fr.asashiin.worldfinder.api.target.WorldFinderStructureTargets;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;

/** Transactional registration surface supplied to a {@link WorldFinderAddon}. */
public interface WorldFinderRegistrar {
    /**
     * Adds a searchable biome definition.
     *
     * @param target non-null target with a unique addon-owned identifier; the
     *               {@code minecraft} and {@code worldfinder} namespaces are reserved
     * @throws IllegalArgumentException if the identifier is reserved or already registered
     */
    void registerBiome(BiomeSearchTarget target);

    /**
     * Adds a searchable structure definition.
     *
     * <p>The {@code minecraft} and {@code worldfinder} namespaces are reserved for native and
     * built-in targets. An addon may still resolve a WorldFinder built-in without registering it
     * by handling a target published in {@link WorldFinderStructureTargets}.</p>
     *
     * @param target non-null target with a unique namespaced identifier outside the reserved
     *               {@code minecraft} and {@code worldfinder} namespaces
     * @throws IllegalArgumentException if the identifier is reserved or already registered
     */
    void registerStructure(StructureSearchTarget target);

    /**
     * Adds a loader-neutral world-generation resolver.
     *
     * @param resolver non-null resolver with a unique addon-owned identifier outside the reserved
     *                 {@code worldfinder} namespace
     * @throws IllegalArgumentException if the identifier is reserved or already registered
     */
    void registerResolver(WorldgenResolver resolver);
}
