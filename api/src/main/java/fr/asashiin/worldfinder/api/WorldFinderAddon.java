package fr.asashiin.worldfinder.api;

/**
 * Implemented by a compatibility addon and instantiated by the loader adapter.
 * Implementations must not perform registration from a static initializer.
 */
public interface WorldFinderAddon {
    /**
     * Returns the stable addon identifier used for diagnostics and duplicate detection.
     *
     * @return a non-null lowercase identifier matching {@code [a-z][a-z0-9_-]{1,63}}
     */
    String id();

    /**
     * Returns the human-readable addon name.
     *
     * @return a non-null, non-blank display name
     */
    default String displayName() {
        return id();
    }

    /**
     * Registers every target and resolver owned by this addon. Registration is transactional:
     * implementations may throw to reject their complete contribution.
     *
     * @param registrar non-null registrar valid only during addon initialization
     */
    void register(WorldFinderRegistrar registrar);
}
