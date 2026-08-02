package fr.asashiin.worldfinder.api.waypoint;

/** Runtime bridge implemented by an optional client map mod or its compatibility addon. */
public interface WaypointProvider {
    /**
     * Returns the stable integration identifier.
     *
     * @return a non-null namespaced identifier
     */
    String id();

    /**
     * Returns the map integration's user-facing name.
     *
     * @return a non-null, non-blank name
     */
    String displayName();

    /**
     * Reports whether this provider can create the requested waypoint in its current client state.
     * WorldFinder invokes this callback synchronously on the client UI thread while constructing a
     * location menu and immediately before creation. Implementations must return promptly and must
     * not wait for network or disk I/O.
     *
     * <p>The default accepts every valid request, preserving compatibility with API implementations
     * compiled before this capability was introduced.</p>
     *
     * @param request immutable, non-null waypoint request
     * @return {@code true} when waypoint creation is currently supported for this destination
     */
    default boolean canCreateWaypoint(WaypointRequest request) {
        return true;
    }

    /**
     * Submits a waypoint to the integrated map mod.
     * WorldFinder invokes this callback synchronously from the client UI thread. Implementations
     * must return promptly and must not wait for network or disk I/O.
     *
     * @param request immutable, non-null waypoint request
     * @return {@code true} when the waypoint was accepted by the map mod
     */
    boolean createWaypoint(WaypointRequest request);

    /**
     * Reports whether sharing is currently usable.
     * WorldFinder may invoke this callback from a background worker, cache its advisory result,
     * and refresh it concurrently with other provider callbacks. Implementations must therefore
     * be thread-safe, return quickly, and never wait for network or disk I/O.
     *
     * @return true only when this map integration has confirmed at least one compatible recipient.
     */
    default boolean hasShareRecipients() {
        return false;
    }

    /**
     * Opens or submits the map mod's native waypoint sharing flow.
     * Implementations should return false when recipient compatibility can no longer be confirmed.
     * WorldFinder invokes this callback synchronously from the client UI thread, so it must return
     * promptly and must not wait for network or disk I/O.
     *
     * @param request immutable, non-null waypoint request
     * @return {@code true} when the sharing flow accepted the waypoint
     */
    default boolean shareWaypoint(WaypointRequest request) {
        return false;
    }
}
