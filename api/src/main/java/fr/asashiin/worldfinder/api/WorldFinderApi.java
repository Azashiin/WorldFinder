package fr.asashiin.worldfinder.api;

/** Stable entry point exposed to world-generation compatibility addons. */
public final class WorldFinderApi {
    private static final int CURRENT_API_VERSION = 2;

    /**
     * Compile-time API generation retained for source compatibility with 0.2 previews.
     * Java compilers inline this field, so integrations must use {@link #apiVersion()} for
     * runtime negotiation.
     *
     * @deprecated use {@link #apiVersion()} whenever the installed runtime version matters
     */
    @Deprecated(forRemoval = false)
    public static final int API_VERSION = CURRENT_API_VERSION;

    private WorldFinderApi() {
    }

    /**
     * Returns the API contract generation implemented by the installed WorldFinder runtime.
     * Unlike a public compile-time constant, this method remains valid across binary upgrades.
     *
     * @return current API generation
     */
    public static int apiVersion() {
        return CURRENT_API_VERSION;
    }
}
