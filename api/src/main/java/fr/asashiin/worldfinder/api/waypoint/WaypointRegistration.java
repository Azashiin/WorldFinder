package fr.asashiin.worldfinder.api.waypoint;

/**
 * Ownership handle for a dynamically registered waypoint provider.
 * Closing is idempotent and can remove only the exact provider associated with this handle.
 */
public interface WaypointRegistration extends AutoCloseable {
    /**
     * Returns the registered provider identifier.
     *
     * @return namespaced provider identifier
     */
    String providerId();

    /**
     * Reports whether this handle still owns the active registry entry.
     *
     * @return {@code true} only while the exact provider remains registered
     */
    boolean active();

    /** Removes the owned provider if it is still the active entry. */
    @Override
    void close();
}
