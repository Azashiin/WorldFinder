package fr.asashiin.worldfinder.api.world;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit outcome of a resolver invocation.
 *
 * <p>An unhandled result delegates to the next resolver or the vanilla engine. A handled result
 * claims the query, even when its value is an empty collection.</p>
 *
 * @param <T> non-null handled value type
 */
public final class ResolverResult<T> {
    private static final ResolverResult<?> UNHANDLED = new ResolverResult<>(false, null);

    private final boolean handled;
    private final T value;

    private ResolverResult(boolean handled, T value) {
        this.handled = handled;
        this.value = value;
    }

    /**
     * Creates an owning result.
     *
     * @param value non-null resolved value
     * @param <T> resolved value type
     * @return handled result
     */
    public static <T> ResolverResult<T> handled(T value) {
        return new ResolverResult<>(true, Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a delegating result.
     *
     * @param <T> expected value type
     * @return shared unhandled result
     */
    @SuppressWarnings("unchecked")
    public static <T> ResolverResult<T> unhandled() {
        return (ResolverResult<T>) UNHANDLED;
    }

    /**
     * Reports whether the resolver claimed the query.
     *
     * @return {@code true} when this result claims the query
     */
    public boolean handled() {
        return handled;
    }

    /**
     * Returns the handled value.
     *
     * @return non-null resolved value
     * @throws NoSuchElementException when this resolver delegated the query
     */
    public T value() {
        if (!handled) {
            throw new NoSuchElementException("Unhandled resolver result has no value");
        }
        return value;
    }

    /**
     * Exposes the value as an optional compatibility view.
     *
     * @return handled value as an optional, or empty when delegated
     */
    public Optional<T> optionalValue() {
        return handled ? Optional.of(value) : Optional.empty();
    }

    /**
     * Returns the resolved value or a caller-provided fallback.
     *
     * @param fallback value returned when unhandled; may be {@code null}
     * @return resolved value or {@code fallback}
     */
    public T orElse(T fallback) {
        return handled ? value : fallback;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ResolverResult<?> that)) {
            return false;
        }
        return handled == that.handled && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handled, value);
    }

    @Override
    public String toString() {
        return handled ? "ResolverResult[handled=" + value + ']'
                : "ResolverResult[unhandled]";
    }
}
