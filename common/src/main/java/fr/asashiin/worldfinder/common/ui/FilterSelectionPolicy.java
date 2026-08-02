package fr.asashiin.worldfinder.common.ui;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Minecraft-independent policy for biome and structure filter gestures.
 *
 * <p>The screen remains responsible for hit testing and translating its native mouse-button
 * values. This class owns the user-visible gesture mapping and selection transitions so both
 * loader clients share one deterministic contract.</p>
 */
public final class FilterSelectionPolicy {
    private FilterSelectionPolicy() {
    }

    /** Filter catalog receiving a list gesture. */
    public enum FilterKind {
        /** Biome-filter catalog. */
        BIOME,
        /** Structure-filter catalog. */
        STRUCTURE
    }

    /** Mouse buttons relevant to filter gestures. */
    public enum PointerButton {
        /** Primary mouse button. */
        LEFT,
        /** Secondary mouse button. */
        RIGHT,
        /** Any mouse button without a filter binding. */
        OTHER
    }

    /** Immutable selection transition selected by a gesture. */
    public enum Action {
        /** Replaces the active biome selection or isolates one structure in the active dimension. */
        REPLACE,
        /** Adds a missing identifier and removes an already-selected identifier. */
        TOGGLE,
        /** Adds an identifier without removing the existing selection. */
        ADD,
        /** Removes the active selection. */
        RESET,
        /** Leaves the selection unchanged. */
        NONE
    }

    /**
     * Resolves a gesture made over the map.
     *
     * <p>Map shortcuts are deliberately Shift-qualified so ordinary map clicks remain available
     * for panning and context actions.</p>
     *
     * @param button translated mouse button
     * @param shiftDown whether Shift is held
     * @return biome-filter action for the gesture
     */
    public static Action mapAction(PointerButton button, boolean shiftDown) {
        Objects.requireNonNull(button, "button");
        if (!shiftDown) return Action.NONE;
        return switch (button) {
            case LEFT -> Action.TOGGLE;
            case RIGHT -> Action.RESET;
            case OTHER -> Action.NONE;
        };
    }

    /**
     * Resolves a gesture made over a biome or structure filter row.
     *
     * @param kind filter catalog receiving the gesture
     * @param button translated mouse button
     * @param shiftDown whether Shift is held
     * @return selection action for the gesture
     */
    public static Action listAction(FilterKind kind, PointerButton button, boolean shiftDown) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(button, "button");
        if (button == PointerButton.RIGHT) {
            return shiftDown ? Action.ADD : Action.REPLACE;
        }
        if (button != PointerButton.LEFT) return Action.NONE;
        return kind == FilterKind.BIOME ? Action.REPLACE : Action.TOGGLE;
    }

    /**
     * Applies a biome selection action and returns an immutable snapshot.
     *
     * <p>An empty biome selection means that no biome filter is active. {@link Action#REPLACE}
     * therefore replaces the complete selection, while {@link Action#ADD} and
     * {@link Action#TOGGLE} preserve the other selected biomes.</p>
     *
     * @param selected currently selected biome identifiers
     * @param targetId biome under the pointer; may be {@code null} only for RESET or NONE
     * @param action action to apply
     * @return immutable resulting selection
     */
    public static Set<String> applyBiomeAction(
            Set<String> selected,
            String targetId,
            Action action
    ) {
        return apply(selected, targetId, action, null);
    }

    /**
     * Applies a structure selection action and returns an immutable snapshot.
     *
     * <p>REPLACE isolates the target only among identifiers belonging to the active dimension.
     * Selections belonging to other dimensions are preserved.</p>
     *
     * @param selected currently enabled structure identifiers across all dimensions
     * @param targetId structure under the pointer; may be {@code null} only for RESET or NONE
     * @param action action to apply
     * @param activeDimensionIds complete structure-filter identifiers for the active dimension
     * @return immutable resulting selection
     * @throws IllegalArgumentException if a target does not belong to the active dimension
     */
    public static Set<String> applyStructureAction(
            Set<String> selected,
            String targetId,
            Action action,
            Set<String> activeDimensionIds
    ) {
        Objects.requireNonNull(activeDimensionIds, "activeDimensionIds");
        Set<String> dimensionSnapshot = Set.copyOf(activeDimensionIds);
        if (requiresTarget(action) && !dimensionSnapshot.contains(targetId)) {
            throw new IllegalArgumentException("Structure target must belong to the active dimension");
        }
        return apply(selected, targetId, action, dimensionSnapshot);
    }

    /**
     * Removes the structure-filter restriction for the active dimension.
     *
     * <p>Structure selections are stored as enabled identifiers, so resetting the filter means
     * replacing the active-dimension selection with its initial defaults. Identifiers belonging
     * to other dimensions are preserved. This is intentionally separate from
     * {@link Action#RESET}, whose generic scoped-selection meaning remains to remove the active
     * scope.</p>
     *
     * @param selected currently enabled structure identifiers across all dimensions
     * @param activeDimensionIds complete structure-filter identifiers for the active dimension
     * @param defaultDimensionIds initially enabled identifiers for the active dimension
     * @return immutable selection with the active dimension restored to its initial defaults
     * @throws IllegalArgumentException if a default identifier is not part of the active dimension
     */
    public static Set<String> resetStructureFilter(
            Set<String> selected,
            Set<String> activeDimensionIds,
            Set<String> defaultDimensionIds
    ) {
        Objects.requireNonNull(selected, "selected");
        Objects.requireNonNull(activeDimensionIds, "activeDimensionIds");
        Objects.requireNonNull(defaultDimensionIds, "defaultDimensionIds");
        Set<String> dimensionSnapshot = Set.copyOf(activeDimensionIds);
        Set<String> defaultsSnapshot = Set.copyOf(defaultDimensionIds);
        if (!dimensionSnapshot.containsAll(defaultsSnapshot)) {
            throw new IllegalArgumentException(
                    "Default structures must belong to the active dimension");
        }
        Set<String> result = new HashSet<>(selected);
        result.removeAll(dimensionSnapshot);
        result.addAll(defaultsSnapshot);
        return Set.copyOf(result);
    }

    /**
     * Expands the targets requested from structure discovery without discarding warm results.
     *
     * <p>Structure filters are a presentation concern. Removing an identifier from the visible
     * selection must therefore not cancel its running scan or invalidate an exact cached result.
     * Newly enabled identifiers are added monotonically and remain warm for the lifetime of the
     * screen session.</p>
     *
     * @param requested identifiers already requested from structure discovery
     * @param visible identifiers selected for presentation
     * @return immutable union of the requested and visible identifiers
     */
    public static Set<String> expandStructureScanTargets(
            Set<String> requested,
            Set<String> visible
    ) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(visible, "visible");
        Set<String> result = new HashSet<>(requested);
        result.addAll(visible);
        return Set.copyOf(result);
    }

    private static Set<String> apply(
            Set<String> selected,
            String targetId,
            Action action,
            Set<String> scope
    ) {
        Objects.requireNonNull(selected, "selected");
        Objects.requireNonNull(action, "action");
        Set<String> result = new HashSet<>(selected);
        if (requiresTarget(action)) requireTarget(targetId);

        switch (action) {
            case REPLACE -> {
                if (scope == null) result.clear();
                else result.removeAll(scope);
                result.add(targetId);
            }
            case TOGGLE -> {
                if (!result.add(targetId)) result.remove(targetId);
            }
            case ADD -> result.add(targetId);
            case RESET -> {
                if (scope == null) result.clear();
                else result.removeAll(scope);
            }
            case NONE -> {
            }
        }
        return Set.copyOf(result);
    }

    private static boolean requiresTarget(Action action) {
        return action == Action.REPLACE || action == Action.TOGGLE || action == Action.ADD;
    }

    private static void requireTarget(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Filter target must not be blank");
        }
    }
}
