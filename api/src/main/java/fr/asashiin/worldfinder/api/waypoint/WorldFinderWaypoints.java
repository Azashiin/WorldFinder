package fr.asashiin.worldfinder.api.waypoint;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe dynamic registry for optional map-mod integrations initialized after WorldFinder.
 * Providers retain insertion order. Duplicate identifiers are rejected rather than replaced.
 */
public final class WorldFinderWaypoints {
    private static final Map<String, WaypointProviderDescriptor> PROVIDERS = new LinkedHashMap<>();

    private WorldFinderWaypoints() {
    }

    /**
     * Registers a provider.
     *
     * @param provider non-null provider with a valid, unique identifier and non-blank name
     * @throws IllegalArgumentException if another provider already owns the identifier
     * @deprecated use {@link #registerOwned(WaypointProvider)} and close its ownership handle
     */
    @Deprecated(forRemoval = false)
    public static void registerProvider(WaypointProvider provider) {
        registerOwned(provider);
    }

    /**
     * Registers a provider and returns an ownership-safe deregistration handle.
     *
     * @param provider non-null provider with a valid, unique identifier and non-blank name
     * @return non-null handle which removes only this exact provider
     * @throws IllegalArgumentException if another provider already owns the identifier
     */
    public static WaypointRegistration registerOwned(WaypointProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = NamespacedId.requireValid(provider.id());
        String displayName = Objects.requireNonNull(provider.displayName(), "provider displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Waypoint provider displayName must not be blank: " + id);
        }
        WaypointProviderDescriptor descriptor = new WaypointProviderDescriptor(id, displayName, provider);
        synchronized (WorldFinderWaypoints.class) {
            if (PROVIDERS.putIfAbsent(id, descriptor) != null) {
                throw new IllegalArgumentException("Duplicate WorldFinder waypoint provider: " + id);
            }
            return new Registration(id, provider);
        }
    }

    /**
     * Removes the provider currently registered under an identifier, if present.
     *
     * @param providerId valid namespaced provider identifier
     * @deprecated use the handle returned by {@link #registerOwned(WaypointProvider)} to avoid
     * removing a provider registered later under the same identifier
     */
    @Deprecated(forRemoval = false)
    public static synchronized void unregisterProvider(String providerId) {
        PROVIDERS.remove(NamespacedId.requireValid(providerId));
    }

    /**
     * Returns an immutable snapshot in registration order.
     *
     * @return provider snapshot, never {@code null}
     */
    public static synchronized List<WaypointProvider> providers() {
        return PROVIDERS.values().stream().map(WaypointProviderDescriptor::provider).toList();
    }

    /**
     * Returns providers active at snapshot time with their registration-time validated metadata.
     * Calling this method never invokes third-party provider callbacks.
     * The returned descriptors do not keep registrations active.
     *
     * @return immutable descriptor snapshot in registration order
     */
    public static synchronized List<WaypointProviderDescriptor> descriptors() {
        return List.copyOf(PROVIDERS.values());
    }

    /**
     * Reports whether the exact descriptor still represents the active registration.
     * This check does not invoke provider callbacks.
     *
     * @param descriptor non-null descriptor obtained from a registry snapshot
     * @return {@code true} only while that exact registration remains active
     */
    public static synchronized boolean isActive(WaypointProviderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        return PROVIDERS.get(descriptor.id()) == descriptor;
    }

    /**
     * Looks up a provider by its stable identifier.
     *
     * @param providerId valid namespaced provider identifier
     * @return provider when currently registered
     */
    public static synchronized Optional<WaypointProvider> provider(String providerId) {
        return Optional.ofNullable(PROVIDERS.get(NamespacedId.requireValid(providerId)))
                .map(WaypointProviderDescriptor::provider);
    }

    private static final class Registration implements WaypointRegistration {
        private final String providerId;
        private final WaypointProvider provider;
        private boolean closed;

        private Registration(String providerId, WaypointProvider provider) {
            this.providerId = providerId;
            this.provider = provider;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public boolean active() {
            synchronized (WorldFinderWaypoints.class) {
                WaypointProviderDescriptor descriptor = PROVIDERS.get(providerId);
                return !closed && descriptor != null && descriptor.provider() == provider;
            }
        }

        @Override
        public void close() {
            synchronized (WorldFinderWaypoints.class) {
                if (!closed) {
                    WaypointProviderDescriptor descriptor = PROVIDERS.get(providerId);
                    if (descriptor != null && descriptor.provider() == provider) {
                        PROVIDERS.remove(providerId);
                    }
                    closed = true;
                }
            }
        }
    }
}
