package fr.asashiin.worldfinder.common.worldgen;

/**
 * Deterministic refusal raised when a native engine cannot prove a declared generation profile.
 *
 * <p>Callers must not retry the same request until its runtime contract or profile changes.</p>
 */
public final class UnsupportedWorldgenProfileException extends IllegalArgumentException {
    /**
     * Creates a refusal with a diagnostic suitable for logs.
     *
     * @param message non-null diagnostic message
     */
    public UnsupportedWorldgenProfileException(String message) {
        super(message);
    }
}
