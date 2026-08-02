package fr.asashiin.worldfinder.common.addon;

import java.util.Objects;
import java.util.concurrent.CancellationException;

final class AddonIsolation {
    private AddonIsolation() {
    }

    static void report(AddonIssueReporter reporter, String sourceId, AddonIssue.Operation operation,
                       Throwable failure) {
        rethrowFatal(failure);
        try {
            reporter.report(new AddonIssue(sourceId, operation, failure));
        } catch (Throwable reporterFailure) {
            rethrowFatal(reporterFailure);
            // Diagnostics must never turn an isolated addon failure into a client crash.
        }
    }

    static String sourceId(Object source) {
        Objects.requireNonNull(source, "source");
        return source.getClass().getName();
    }

    static void rethrowFatal(Throwable failure) {
        if (failure instanceof CancellationException cancellationException) {
            throw cancellationException;
        }
        if (failure instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("WorldFinder addon operation was interrupted", failure);
        }
        if (failure instanceof VirtualMachineError
                || failure.getClass().getName().equals("java.lang.ThreadDeath")) {
            throw (Error) failure;
        }
    }
}
