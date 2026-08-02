package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.WorldFinderAddon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/** Process-wide, thread-safe addon bootstrap and registry access point. */
public final class WorldFinderAddons {
    private static final AddonRegistry REGISTRY = new AddonRegistry();
    private static boolean initialized;

    private WorldFinderAddons() {
    }

    /**
     * Initializes an explicit addon collection once, forwarding diagnostics as plain messages.
     * Later initialization calls are no-ops.
     *
     * @param addons non-null addon collection
     * @param logger non-null message consumer
     */
    public static synchronized void initialize(Collection<? extends WorldFinderAddon> addons, Consumer<String> logger) {
        Objects.requireNonNull(logger, "logger");
        initialize(addons, AddonIssueReporter.messages(logger), logger);
    }

    /**
     * Initializes an explicit addon collection once with structured diagnostics.
     *
     * @param addons non-null addon collection
     * @param reporter non-null diagnostic sink
     */
    public static synchronized void initializeWithDiagnostics(
            Collection<? extends WorldFinderAddon> addons,
            AddonIssueReporter reporter
    ) {
        initialize(addons, reporter, message -> {
        });
    }

    private static void initialize(
            Collection<? extends WorldFinderAddon> addons,
            AddonIssueReporter reporter,
            Consumer<String> successLogger
    ) {
        if (initialized) {
            return;
        }
        Objects.requireNonNull(addons, "addons");
        Objects.requireNonNull(reporter, "reporter");
        for (WorldFinderAddon addon : addons) {
            String sourceId = addon == null ? "unknown" : AddonIsolation.sourceId(addon);
            try {
                REGISTRY.registerAddon(addon);
            } catch (Throwable failure) {
                AddonIsolation.report(reporter, sourceId, AddonIssue.Operation.REGISTRATION, failure);
                continue;
            }
            safeLog(successLogger, "Loaded WorldFinder addon " + sourceId);
        }
        REGISTRY.freeze();
        initialized = true;
    }

    /**
     * Discovers services provider by provider and initializes the registry once.
     * A malformed or unconstructable provider does not suppress later valid providers.
     *
     * @param classLoader class loader used for service discovery
     * @param logger non-null message consumer
     */
    public static synchronized void initializeServices(ClassLoader classLoader, Consumer<String> logger) {
        if (initialized) {
            return;
        }
        Objects.requireNonNull(logger, "logger");
        AddonIssueReporter reporter = AddonIssueReporter.messages(logger);
        initialize(discoverServices(classLoader, reporter), reporter, logger);
    }

    /**
     * Discovers services provider by provider and initializes with structured diagnostics.
     *
     * @param classLoader class loader used for service discovery
     * @param reporter non-null diagnostic sink
     */
    public static synchronized void initializeServicesWithDiagnostics(
            ClassLoader classLoader,
            AddonIssueReporter reporter
    ) {
        if (initialized) {
            return;
        }
        initialize(discoverServices(classLoader, reporter), reporter, message -> {
        });
    }

    static List<WorldFinderAddon> discoverServices(ClassLoader classLoader, AddonIssueReporter reporter) {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(reporter, "reporter");
        List<WorldFinderAddon> discovered = new ArrayList<>();
        Iterator<ServiceLoader.Provider<WorldFinderAddon>> providers;
        try {
            providers = ServiceLoader.load(WorldFinderAddon.class, classLoader).stream().iterator();
        } catch (Throwable failure) {
            AddonIsolation.report(reporter, "service-loader", AddonIssue.Operation.SERVICE_DISCOVERY, failure);
            return List.of();
        }

        int consecutiveDiscoveryFailures = 0;
        while (true) {
            boolean hasNext;
            try {
                hasNext = providers.hasNext();
            } catch (Throwable failure) {
                AddonIsolation.report(reporter, "service-loader", AddonIssue.Operation.SERVICE_DISCOVERY, failure);
                if (++consecutiveDiscoveryFailures >= 3) {
                    break;
                }
                continue;
            }
            if (!hasNext) {
                break;
            }

            ServiceLoader.Provider<WorldFinderAddon> provider;
            try {
                provider = providers.next();
            } catch (Throwable failure) {
                AddonIsolation.report(reporter, "service-loader", AddonIssue.Operation.SERVICE_DISCOVERY, failure);
                if (++consecutiveDiscoveryFailures >= 3) {
                    break;
                }
                continue;
            }
            consecutiveDiscoveryFailures = 0;

            String sourceId = providerName(provider);
            try {
                discovered.add(Objects.requireNonNull(provider.get(), "Service provider returned null"));
            } catch (Throwable failure) {
                AddonIsolation.report(reporter, sourceId, AddonIssue.Operation.SERVICE_INSTANTIATION, failure);
            }
        }
        return List.copyOf(discovered);
    }

    /**
     * Returns the process-wide registry. It becomes immutable after initialization.
     *
     * @return non-null registry
     */
    public static AddonRegistry registry() {
        return REGISTRY;
    }

    /**
     * Reports bootstrap state.
     *
     * @return {@code true} after the registry was frozen
     */
    public static synchronized boolean isInitialized() {
        return initialized;
    }

    private static String providerName(ServiceLoader.Provider<WorldFinderAddon> provider) {
        try {
            return provider.type().getName();
        } catch (Throwable failure) {
            AddonIsolation.rethrowFatal(failure);
            return "service-provider";
        }
    }

    private static void safeLog(Consumer<String> logger, String message) {
        try {
            logger.accept(message);
        } catch (Throwable failure) {
            AddonIsolation.rethrowFatal(failure);
        }
    }
}
