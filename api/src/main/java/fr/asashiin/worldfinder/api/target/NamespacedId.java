package fr.asashiin.worldfinder.api.target;

import java.util.Objects;

/** Validation utilities for loader-neutral resource identifiers. */
public final class NamespacedId {
    private NamespacedId() {
    }

    /**
     * Validates a resource identifier without normalizing it.
     *
     * @param id identifier to validate; never {@code null}
     * @return the original identifier
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is not in lowercase {@code namespace:path} form
     */
    public static String requireValid(String id) {
        Objects.requireNonNull(id, "id");
        if (!isValid(id)) {
            throw new IllegalArgumentException("Expected a namespaced id, got: " + id);
        }
        return id;
    }

    /**
     * Returns whether {@code id} has the Minecraft-compatible {@code namespace:path} form.
     * Validation is deliberately ASCII-only and does not depend on locale or regex settings.
     *
     * @param id identifier to inspect; {@code null} is accepted and returns {@code false}
     * @return {@code true} only for a complete lowercase namespace and path
     */
    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        int separator = id.indexOf(':');
        if (separator <= 0 || separator == id.length() - 1 || id.indexOf(':', separator + 1) >= 0) {
            return false;
        }
        for (int index = 0; index < separator; index++) {
            if (!isNamespaceCharacter(id.charAt(index))) {
                return false;
            }
        }
        for (int index = separator + 1; index < id.length(); index++) {
            if (!isPathCharacter(id.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNamespaceCharacter(char character) {
        return character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '_' || character == '-' || character == '.';
    }

    private static boolean isPathCharacter(char character) {
        return isNamespaceCharacter(character) || character == '/';
    }
}
