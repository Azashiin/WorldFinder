package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.target.BiomeSearchTarget;
import fr.asashiin.worldfinder.api.target.NamespacedId;
import fr.asashiin.worldfinder.common.map.BiomeVerticalSelection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Versioned vertical limits and useful fixed-height bands for a vanilla Normal Overworld profile.
 *
 * <p>The fixed levels are UI sampling bands, not claims that terrain or walkable air exists at a
 * coordinate. Per-biome level sets only control filter availability; the biome engine remains the
 * source of truth for every sampled point.</p>
 *
 * @param minYInclusive lowest valid Overworld biome-query coordinate
 * @param maxYExclusive first coordinate above the Overworld build range
 * @param nominalSurfaceY nominal query coordinate carried alongside {@code SURFACE}
 * @param fixedYLevels the {@code Surface} and {@code Underground} fixed-height projections
 * @param undergroundBiomeLevels relevant fixed-height bands for known underground biomes
 */
public record OverworldBiomeVerticalProfile(
        int minYInclusive,
        int maxYExclusive,
        int nominalSurfaceY,
        List<Integer> fixedYLevels,
        Map<String, Set<Integer>> undergroundBiomeLevels
) {
    public OverworldBiomeVerticalProfile {
        if (minYInclusive >= maxYExclusive) {
            throw new IllegalArgumentException("Overworld vertical limits are empty");
        }
        requireInside(nominalSurfaceY, minYInclusive, maxYExclusive, "nominal surface Y");
        Objects.requireNonNull(fixedYLevels, "fixedYLevels");
        if (fixedYLevels.size() != 2) {
            throw new IllegalArgumentException(
                    "Exactly two fixed-height layers are required alongside Highlight");
        }
        ArrayList<Integer> levelCopy = new ArrayList<>(fixedYLevels.size());
        Integer previous = null;
        for (Integer level : fixedYLevels) {
            Objects.requireNonNull(level, "fixed Y level");
            requireInside(level, minYInclusive, maxYExclusive, "fixed Y level");
            if (previous != null && level >= previous) {
                throw new IllegalArgumentException("Fixed Y levels must be unique and strictly descending");
            }
            levelCopy.add(level);
            previous = level;
        }
        List<Integer> validatedFixedYLevels = List.copyOf(levelCopy);
        if (!validatedFixedYLevels.contains(nominalSurfaceY)) {
            throw new IllegalArgumentException(
                    "The fixed-height layers must contain the nominal Surface Y");
        }
        int undergroundY = validatedFixedYLevels.stream()
                .filter(level -> level != nominalSurfaceY)
                .findFirst()
                .orElseThrow();
        if (undergroundY >= nominalSurfaceY) {
            throw new IllegalArgumentException("Underground Y must be below Surface Y");
        }
        fixedYLevels = validatedFixedYLevels;

        Objects.requireNonNull(undergroundBiomeLevels, "undergroundBiomeLevels");
        LinkedHashMap<String, Set<Integer>> biomeLevels = new LinkedHashMap<>();
        undergroundBiomeLevels.forEach((biomeId, levels) -> {
            String validatedId = NamespacedId.requireValid(biomeId);
            Objects.requireNonNull(levels, "underground levels for " + validatedId);
            if (levels.isEmpty()) {
                throw new IllegalArgumentException(
                        "An underground biome must have at least one relevant fixed layer: " + validatedId);
            }
            LinkedHashSet<Integer> levelSet = new LinkedHashSet<>();
            for (Integer level : levels) {
                Objects.requireNonNull(level, "underground biome Y level");
                if (!validatedFixedYLevels.contains(level)) {
                    throw new IllegalArgumentException(
                            "Underground biome level is not in the profile cycle: " + level);
                }
                levelSet.add(level);
            }
            biomeLevels.put(validatedId, Collections.unmodifiableSet(levelSet));
        });
        undergroundBiomeLevels = Collections.unmodifiableMap(biomeLevels);
    }

    /** Returns the three-state cycle: Underground, Surface, then Highlight. */
    public List<BiomeVerticalSelection> selections() {
        return List.of(
                BiomeVerticalSelection.fixedY(undergroundY()),
                BiomeVerticalSelection.fixedY(nominalSurfaceY),
                BiomeVerticalSelection.surface()
        );
    }

    /** Advances with wraparound from the final fixed layer to {@code Surface}. */
    public BiomeVerticalSelection next(BiomeVerticalSelection current) {
        return offset(current, 1);
    }

    /** Moves backwards with wraparound from {@code Surface} to the deepest fixed layer. */
    public BiomeVerticalSelection previous(BiomeVerticalSelection current) {
        return offset(current, -1);
    }

    /** Returns whether a selection belongs to this exact profile. */
    public boolean contains(BiomeVerticalSelection selection) {
        return selections().contains(Objects.requireNonNull(selection, "selection"));
    }

    /** Returns the stable user-facing name for one selection in this profile. */
    public String displayName(BiomeVerticalSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection.isSurface()) {
            return "Highlight";
        }
        int fixedY = selection.fixedY().orElseThrow();
        if (fixedY == nominalSurfaceY) {
            return "Surface";
        }
        if (fixedYLevels.contains(fixedY)) {
            return "Underground";
        }
        throw new IllegalArgumentException("Vertical selection does not belong to this profile");
    }

    /** Returns the single fixed height represented by the Underground button state. */
    public int undergroundY() {
        return fixedYLevels.stream()
                .filter(level -> level != nominalSurfaceY)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Returns whether a catalog biome is meaningful for the selected projection.
     * Unknown addon underground targets remain available at fixed Y and must enforce finer ranges
     * through their resolver support contract.
     */
    public boolean isBiomeAvailable(
            String biomeId,
            BiomeSearchTarget.Layer layer,
            BiomeVerticalSelection selection
    ) {
        NamespacedId.requireValid(biomeId);
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(selection, "selection");
        if (layer == BiomeSearchTarget.Layer.SURFACE) {
            return true;
        }
        if (selection.isSurface()) {
            return false;
        }
        Set<Integer> relevantLevels = undergroundBiomeLevels.get(biomeId);
        return relevantLevels == null || relevantLevels.contains(selection.fixedY().orElseThrow());
    }

    /** Returns known relevant fixed layers for one underground biome. */
    public Set<Integer> relevantLevels(String biomeId) {
        Set<Integer> levels = undergroundBiomeLevels.get(NamespacedId.requireValid(biomeId));
        return levels == null ? Set.of() : levels;
    }

    private BiomeVerticalSelection offset(BiomeVerticalSelection current, int offset) {
        Objects.requireNonNull(current, "current");
        List<BiomeVerticalSelection> selections = selections();
        int index = selections.indexOf(current);
        if (index < 0) {
            throw new IllegalArgumentException("Vertical selection does not belong to this profile");
        }
        return selections.get(Math.floorMod(index + offset, selections.size()));
    }

    private static void requireInside(int value, int minInclusive, int maxExclusive, String label) {
        if (value < minInclusive || value >= maxExclusive) {
            throw new IllegalArgumentException(label + " lies outside the Overworld build range");
        }
    }
}
