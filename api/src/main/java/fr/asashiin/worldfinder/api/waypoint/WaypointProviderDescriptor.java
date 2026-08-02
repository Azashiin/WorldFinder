package fr.asashiin.worldfinder.api.waypoint;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.Objects;

/**
 * Immutable registration-time metadata snapshot for a waypoint integration.
 * A descriptor does not own or pin the registration; the provider may have been deregistered
 * after a caller obtained the snapshot.
 *
 * @param id validated provider identifier
 * @param displayName validated display name captured once during registration
 * @param provider provider callback target captured by the registration snapshot
 */
public record WaypointProviderDescriptor(
        String id,
        String displayName,
        WaypointProvider provider
) {
    /** Validates and snapshots a provider's stable metadata. */
    public WaypointProviderDescriptor {
        id = NamespacedId.requireValid(id);
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Waypoint provider displayName must not be blank: " + id);
        }
        Objects.requireNonNull(provider, "provider");
    }
}
