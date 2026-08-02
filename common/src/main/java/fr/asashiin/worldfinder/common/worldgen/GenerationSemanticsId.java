package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.target.NamespacedId;

/**
 * Stable identity of an effective world-generation behavior.
 *
 * <p>The identifier describes produced seed-map results, not a Minecraft marketing version or a
 * Java/API compatibility family. Two Minecraft releases may therefore share this identity even
 * when they require different platform adapters.</p>
 *
 * @param value lowercase namespaced identifier
 */
public record GenerationSemanticsId(String value) {
    /** Validates a generation-semantics identifier. */
    public GenerationSemanticsId {
        value = NamespacedId.requireValid(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
