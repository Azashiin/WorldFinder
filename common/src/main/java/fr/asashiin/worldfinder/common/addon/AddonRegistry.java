package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.WorldFinderAddon;
import fr.asashiin.worldfinder.api.WorldFinderRegistrar;
import fr.asashiin.worldfinder.api.target.BiomeSearchTarget;
import fr.asashiin.worldfinder.api.target.NamespacedId;
import fr.asashiin.worldfinder.api.target.StructureSearchTarget;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Thread-safe transactional registry for addon contributions.
 * Registration is mutable until {@link #freeze()}, after which accessors return stable snapshots.
 */
public final class AddonRegistry implements WorldFinderRegistrar {
    private static final String MINECRAFT_NAMESPACE_PREFIX = "minecraft:";
    private static final String WORLDFINDER_NAMESPACE_PREFIX = "worldfinder:";
    private final Map<String, WorldFinderAddon> addons = new LinkedHashMap<>();
    private final Map<String, BiomeSearchTarget> biomes = new LinkedHashMap<>();
    private final Map<String, StructureSearchTarget> structures = new LinkedHashMap<>();
    private final Map<String, WorldgenResolver> resolvers = new LinkedHashMap<>();
    private final Map<String, Integer> resolverPriorities = new LinkedHashMap<>();
    private List<WorldFinderAddon> frozenAddons;
    private List<BiomeSearchTarget> frozenBiomes;
    private List<StructureSearchTarget> frozenStructures;
    private List<WorldgenResolver> frozenResolvers;
    private boolean frozen;

    /** Creates an empty mutable registry. */
    public AddonRegistry() {
    }

    /**
     * Registers one addon and all of its contributions atomically.
     *
     * @param addon non-null addon with a unique valid identifier
     * @throws IllegalStateException if registration is frozen or the addon registration fails
     * @throws IllegalArgumentException if identifiers or display metadata are invalid
     */
    public synchronized void registerAddon(WorldFinderAddon addon) {
        requireMutable();
        Objects.requireNonNull(addon, "addon");
        String id = requireAddonId(addon.id());
        String displayName = Objects.requireNonNull(addon.displayName(), "addon displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("WorldFinder addon displayName must not be blank: " + id);
        }
        Map<String, WorldFinderAddon> previousAddons = new LinkedHashMap<>(addons);
        if (addons.putIfAbsent(id, addon) != null) {
            throw new IllegalArgumentException("Duplicate WorldFinder addon: " + id);
        }
        Map<String, BiomeSearchTarget> previousBiomes = new LinkedHashMap<>(biomes);
        Map<String, StructureSearchTarget> previousStructures = new LinkedHashMap<>(structures);
        Map<String, WorldgenResolver> previousResolvers = new LinkedHashMap<>(resolvers);
        Map<String, Integer> previousResolverPriorities = new LinkedHashMap<>(resolverPriorities);
        try {
            addon.register(this);
        } catch (Throwable exception) {
            restore(addons, previousAddons);
            restore(biomes, previousBiomes);
            restore(structures, previousStructures);
            restore(resolvers, previousResolvers);
            restore(resolverPriorities, previousResolverPriorities);
            AddonIsolation.rethrowFatal(exception);
            throw new IllegalStateException("WorldFinder addon registration failed: " + id, exception);
        }
    }

    @Override
    public synchronized void registerBiome(BiomeSearchTarget target) {
        requireMutable();
        Objects.requireNonNull(target, "biome");
        if (target.id().startsWith(MINECRAFT_NAMESPACE_PREFIX)
                || target.id().startsWith(WORLDFINDER_NAMESPACE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Native biome target namespace is reserved: " + target.id()
            );
        }
        putUnique(biomes, target.id(), target, "biome");
    }

    @Override
    public synchronized void registerStructure(StructureSearchTarget target) {
        requireMutable();
        Objects.requireNonNull(target, "structure");
        if (target.id().startsWith(MINECRAFT_NAMESPACE_PREFIX)
                || target.id().startsWith(WORLDFINDER_NAMESPACE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Native structure target namespace is reserved: " + target.id()
            );
        }
        putUnique(structures, target.id(), target, "structure");
    }

    @Override
    public synchronized void registerResolver(WorldgenResolver resolver) {
        requireMutable();
        Objects.requireNonNull(resolver, "resolver");
        String id = NamespacedId.requireValid(resolver.id());
        if (id.startsWith(WORLDFINDER_NAMESPACE_PREFIX)) {
            throw new IllegalArgumentException("WorldFinder resolver namespace is reserved: " + id);
        }
        int priority = resolver.priority();
        putUnique(resolvers, id, resolver, "resolver");
        resolverPriorities.put(id, priority);
    }

    /** Freezes registration and materializes immutable, deterministically ordered snapshots. */
    public synchronized void freeze() {
        if (frozen) {
            return;
        }
        frozenAddons = List.copyOf(addons.values());
        frozenBiomes = List.copyOf(biomes.values());
        frozenStructures = List.copyOf(structures.values());
        frozenResolvers = orderedResolvers();
        frozen = true;
    }

    /**
     * Returns registered addons in registration order.
     *
     * @return immutable snapshot
     */
    public synchronized List<WorldFinderAddon> addons() {
        return frozen ? frozenAddons : List.copyOf(addons.values());
    }

    /**
     * Returns registered biome targets in registration order.
     *
     * @return immutable snapshot
     */
    public synchronized List<BiomeSearchTarget> biomes() {
        return frozen ? frozenBiomes : List.copyOf(biomes.values());
    }

    /**
     * Returns registered structure targets in registration order.
     *
     * @return immutable snapshot
     */
    public synchronized List<StructureSearchTarget> structures() {
        return frozen ? frozenStructures : List.copyOf(structures.values());
    }

    /**
     * Returns resolvers by descending snapshotted priority, then identifier.
     *
     * @return immutable deterministic snapshot
     */
    public synchronized List<WorldgenResolver> resolvers() {
        return frozen ? frozenResolvers : orderedResolvers();
    }

    private List<WorldgenResolver> orderedResolvers() {
        List<Map.Entry<String, WorldgenResolver>> ordered = new ArrayList<>(resolvers.entrySet());
        ordered.sort(Comparator
                .<Map.Entry<String, WorldgenResolver>>comparingInt(
                        entry -> resolverPriorities.get(entry.getKey())
                ).reversed()
                .thenComparing(Map.Entry::getKey));
        return ordered.stream().map(Map.Entry::getValue).toList();
    }

    private void requireMutable() {
        if (frozen) {
            throw new IllegalStateException("WorldFinder addon registration is closed");
        }
    }

    private static String requireAddonId(String id) {
        Objects.requireNonNull(id, "addon id");
        if (id.length() < 2 || id.length() > 64 || id.charAt(0) < 'a' || id.charAt(0) > 'z') {
            throw new IllegalArgumentException("Invalid WorldFinder addon id: " + id);
        }
        for (int index = 1; index < id.length(); index++) {
            char character = id.charAt(index);
            boolean valid = character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '_' || character == '-';
            if (!valid) {
                throw new IllegalArgumentException("Invalid WorldFinder addon id: " + id);
            }
        }
        return id;
    }

    private static <T> void putUnique(Map<String, T> values, String id, T value, String type) {
        Objects.requireNonNull(value, type);
        Objects.requireNonNull(id, type + " id");
        if (values.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("Duplicate WorldFinder " + type + ": " + id);
        }
    }

    private static <T> void restore(Map<String, T> destination, Map<String, T> snapshot) {
        destination.clear();
        destination.putAll(snapshot);
    }
}
