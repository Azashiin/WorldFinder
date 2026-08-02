package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.target.NamespacedId;

import java.util.Objects;

/**
 * Version-neutral, quantized fixture for the Sulfur Caves climate point added in Minecraft 26.2.
 *
 * <p>Minecraft quantizes climate coordinates by multiplying their float representation by
 * {@value #QUANTIZATION_FACTOR}. Keeping the integer form here makes every boundary explicit and
 * avoids float-rounding differences in compatibility tests. The fixture describes researched
 * vanilla data only; it does not make an adapter compiled against an older Minecraft jar support
 * 26.2.</p>
 */
public final class SulfurCavesClimatePointFixture {
    /** Vanilla climate-coordinate quantization factor. */
    public static final int QUANTIZATION_FACTOR = 10_000;

    /** Exact quantized Minecraft 26.2 Sulfur Caves parameter point. */
    public static final QuantizedClimatePoint POINT = new QuantizedClimatePoint(
            new QuantizedRange(-10_000L, 10_000L),
            new QuantizedRange(-10_000L, 10_000L),
            new QuantizedRange(-1_900L, 10_000L),
            new QuantizedRange(4_500L, 10_000L),
            new QuantizedRange(2_000L, 9_000L),
            new QuantizedRange(-11_000L, -8_500L),
            0L,
            "minecraft:sulfur_caves"
    );

    /** Exact insertion anchors in the ordered Overworld parameter list. */
    public static final ParameterListInsertion INSERTION = new ParameterListInsertion(
            "minecraft:lush_caves",
            "minecraft:deep_dark"
    );

    private SulfurCavesClimatePointFixture() {
    }

    /**
     * Closed interval in Minecraft's quantized climate-coordinate space.
     *
     * @param min inclusive lower bound
     * @param max inclusive upper bound
     */
    public record QuantizedRange(long min, long max) {
        /** Validates the interval ordering. */
        public QuantizedRange {
            if (min > max) {
                throw new IllegalArgumentException("Quantized climate range min exceeds max");
            }
        }
    }

    /**
     * Complete six-axis climate parameter point and its biome value.
     *
     * @param temperature temperature interval
     * @param humidity humidity interval
     * @param continentalness continentalness interval
     * @param erosion erosion interval
     * @param depth depth interval
     * @param weirdness weirdness interval
     * @param offset quantized fitness offset
     * @param biomeId namespaced biome identifier
     */
    public record QuantizedClimatePoint(
            QuantizedRange temperature,
            QuantizedRange humidity,
            QuantizedRange continentalness,
            QuantizedRange erosion,
            QuantizedRange depth,
            QuantizedRange weirdness,
            long offset,
            String biomeId
    ) {
        /** Validates every interval and the biome identifier. */
        public QuantizedClimatePoint {
            Objects.requireNonNull(temperature, "temperature");
            Objects.requireNonNull(humidity, "humidity");
            Objects.requireNonNull(continentalness, "continentalness");
            Objects.requireNonNull(erosion, "erosion");
            Objects.requireNonNull(depth, "depth");
            Objects.requireNonNull(weirdness, "weirdness");
            biomeId = NamespacedId.requireValid(biomeId);
        }
    }

    /**
     * Stable neighboring biome values which define one ordered-list insertion position.
     *
     * @param afterBiomeId existing value immediately before the inserted point
     * @param beforeBiomeId existing value immediately after the inserted point
     */
    public record ParameterListInsertion(String afterBiomeId, String beforeBiomeId) {
        /** Validates both anchors and rejects an ambiguous self-anchor. */
        public ParameterListInsertion {
            afterBiomeId = NamespacedId.requireValid(afterBiomeId);
            beforeBiomeId = NamespacedId.requireValid(beforeBiomeId);
            if (afterBiomeId.equals(beforeBiomeId)) {
                throw new IllegalArgumentException("Parameter-list insertion anchors must differ");
            }
        }
    }
}
