package fr.asashiin.worldfinder.common.addon;

import java.util.Objects;
import java.util.function.Consumer;

/** Injectable logging boundary used without coupling the common module to a logging framework. */
@FunctionalInterface
public interface AddonIssueReporter {
    /**
     * Receives one non-fatal addon diagnostic. Reporter failures are isolated by callers.
     *
     * @param issue non-null structured diagnostic
     */
    void report(AddonIssue issue);

    /**
     * Creates a reporter which intentionally discards diagnostics.
     *
     * @return stateless no-op reporter
     */
    static AddonIssueReporter ignoring() {
        return issue -> {
        };
    }

    /**
     * Adapts structured diagnostics to a plain message consumer.
     *
     * @param logger non-null message consumer
     * @return reporter forwarding {@link AddonIssue#message()}
     */
    static AddonIssueReporter messages(Consumer<String> logger) {
        Objects.requireNonNull(logger, "logger");
        return issue -> logger.accept(issue.message());
    }
}
