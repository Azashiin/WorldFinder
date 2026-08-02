package fr.asashiin.worldfinder.api.world;

import java.util.concurrent.CancellationException;

/**
 * Loader-neutral cooperative cancellation signal for potentially expensive world-generation work.
 * Implementations must be thread-safe, non-blocking, and inexpensive to query frequently.
 */
@FunctionalInterface
public interface CancellationToken {
    /** Token which never requests cancellation. */
    CancellationToken NONE = () -> false;

    /**
     * Reports whether the caller no longer needs the result.
     *
     * @return {@code true} when work should stop as soon as practical
     */
    boolean isCancellationRequested();

    /**
     * Aborts the current operation when cancellation was requested.
     *
     * @throws CancellationException when {@link #isCancellationRequested()} is {@code true}
     */
    default void throwIfCancellationRequested() {
        if (isCancellationRequested()) {
            throw new CancellationException("WorldFinder world-generation query was cancelled");
        }
    }
}
