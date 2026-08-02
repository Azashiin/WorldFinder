package fr.asashiin.worldfinder.common.map;

import java.util.Objects;

/**
 * One biome observation and its presentation colour.
 *
 * @param position horizontal block position of the sample
 * @param biomeId non-blank namespaced biome identifier
 * @param argbColor biome colour encoded as ARGB
 */
public record BiomeSample(MapPosition position, String biomeId, int argbColor) {
    /**
     * Creates and validates a biome sample.
     *
     * @throws NullPointerException if {@code position} or {@code biomeId} is {@code null}
     * @throws IllegalArgumentException if {@code biomeId} is blank
     */
    public BiomeSample {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(biomeId, "biomeId");
        if (biomeId.isBlank()) {
            throw new IllegalArgumentException("Biome id cannot be blank");
        }
    }
}
