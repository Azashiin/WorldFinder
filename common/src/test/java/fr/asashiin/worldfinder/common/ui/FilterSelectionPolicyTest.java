package fr.asashiin.worldfinder.common.ui;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.Action.ADD;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.Action.NONE;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.Action.REPLACE;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.Action.RESET;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.Action.TOGGLE;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.FilterKind.BIOME;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.FilterKind.STRUCTURE;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.PointerButton.LEFT;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.PointerButton.OTHER;
import static fr.asashiin.worldfinder.common.ui.FilterSelectionPolicy.PointerButton.RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilterSelectionPolicyTest {
    @Test
    void mapShortcutsToggleWithShiftLeftAndResetWithShiftRight() {
        assertEquals(TOGGLE, FilterSelectionPolicy.mapAction(LEFT, true));
        assertEquals(RESET, FilterSelectionPolicy.mapAction(RIGHT, true));
        assertEquals(NONE, FilterSelectionPolicy.mapAction(LEFT, false));
        assertEquals(NONE, FilterSelectionPolicy.mapAction(RIGHT, false));
        assertEquals(NONE, FilterSelectionPolicy.mapAction(OTHER, true));
    }

    @Test
    void mapShortcutAddsASecondBiomeThenResetsTheSelection() {
        Set<String> selected = FilterSelectionPolicy.applyBiomeAction(
                Set.of("minecraft:plains"),
                "minecraft:forest",
                FilterSelectionPolicy.mapAction(LEFT, true));

        assertEquals(Set.of("minecraft:plains", "minecraft:forest"), selected);
        assertEquals(Set.of(), FilterSelectionPolicy.applyBiomeAction(
                selected, null, FilterSelectionPolicy.mapAction(RIGHT, true)));
    }

    @Test
    void biomeListReplacesNormallyAndAddsWithShiftRight() {
        assertEquals(REPLACE, FilterSelectionPolicy.listAction(BIOME, LEFT, false));
        assertEquals(REPLACE, FilterSelectionPolicy.listAction(BIOME, LEFT, true));
        assertEquals(REPLACE, FilterSelectionPolicy.listAction(BIOME, RIGHT, false));
        assertEquals(ADD, FilterSelectionPolicy.listAction(BIOME, RIGHT, true));
        assertEquals(NONE, FilterSelectionPolicy.listAction(BIOME, OTHER, true));
    }

    @Test
    void structureListTogglesNormallyIsolatesWithRightAndAddsWithShiftRight() {
        assertEquals(TOGGLE, FilterSelectionPolicy.listAction(STRUCTURE, LEFT, false));
        assertEquals(TOGGLE, FilterSelectionPolicy.listAction(STRUCTURE, LEFT, true));
        assertEquals(REPLACE, FilterSelectionPolicy.listAction(STRUCTURE, RIGHT, false));
        assertEquals(ADD, FilterSelectionPolicy.listAction(STRUCTURE, RIGHT, true));
        assertEquals(NONE, FilterSelectionPolicy.listAction(STRUCTURE, OTHER, false));
    }

    @Test
    void shiftRightOnAFilterRowAddsWithoutReplacingTheExistingSelection() {
        assertEquals(Set.of("minecraft:plains", "minecraft:forest"),
                FilterSelectionPolicy.applyBiomeAction(
                        Set.of("minecraft:plains"),
                        "minecraft:forest",
                        FilterSelectionPolicy.listAction(BIOME, RIGHT, true)));
        assertEquals(Set.of("village", "shipwreck", "fortress"),
                FilterSelectionPolicy.applyStructureAction(
                        Set.of("village", "fortress"),
                        "shipwreck",
                        FilterSelectionPolicy.listAction(STRUCTURE, RIGHT, true),
                        Set.of("village", "shipwreck")));
    }

    @Test
    void biomeReplaceToggleAddAndResetHaveDistinctSemantics() {
        Set<String> plains = Set.of("minecraft:plains");

        assertEquals(Set.of("minecraft:forest"),
                FilterSelectionPolicy.applyBiomeAction(plains, "minecraft:forest", REPLACE));
        assertEquals(Set.of("minecraft:plains", "minecraft:forest"),
                FilterSelectionPolicy.applyBiomeAction(plains, "minecraft:forest", ADD));
        assertEquals(Set.of(),
                FilterSelectionPolicy.applyBiomeAction(plains, "minecraft:plains", TOGGLE));
        assertEquals(Set.of("minecraft:plains", "minecraft:forest"),
                FilterSelectionPolicy.applyBiomeAction(plains, "minecraft:forest", TOGGLE));
        assertEquals(Set.of(),
                FilterSelectionPolicy.applyBiomeAction(plains, null, RESET));
    }

    @Test
    void addingAnAlreadySelectedBiomeIsIdempotent() {
        Set<String> selected = Set.of("minecraft:plains", "minecraft:forest");

        assertEquals(selected,
                FilterSelectionPolicy.applyBiomeAction(selected, "minecraft:forest", ADD));
    }

    @Test
    void structureReplaceIsLimitedToTheActiveDimension() {
        Set<String> selected = Set.of("village", "shipwreck", "fortress", "end_city");
        Set<String> overworldIds = Set.of("village", "shipwreck", "monument");

        assertEquals(Set.of("monument", "fortress", "end_city"),
                FilterSelectionPolicy.applyStructureAction(
                        selected, "monument", REPLACE, overworldIds));
    }

    @Test
    void structureAddAndTogglePreserveOtherDimensions() {
        Set<String> selected = Set.of("village", "fortress");
        Set<String> overworldIds = Set.of("village", "shipwreck");

        assertEquals(Set.of("village", "shipwreck", "fortress"),
                FilterSelectionPolicy.applyStructureAction(
                        selected, "shipwreck", ADD, overworldIds));
        assertEquals(Set.of("fortress"),
                FilterSelectionPolicy.applyStructureAction(
                        selected, "village", TOGGLE, overworldIds));
    }

    @Test
    void structureFilterResetRestoresTheInitialStateOfTheActiveDimension() {
        Set<String> selected = Set.of(
                "village",
                "trial_chambers",
                "fortress",
                "end_city");
        Set<String> overworldIds = Set.of(
                "village",
                "shipwreck",
                "monument",
                "trial_chambers");
        Set<String> overworldDefaults = Set.of("village", "shipwreck", "monument");

        assertEquals(Set.of("village", "shipwreck", "monument", "fortress", "end_city"),
                FilterSelectionPolicy.resetStructureFilter(
                        selected, overworldIds, overworldDefaults));
    }

    @Test
    void structureFilterResetIsIdempotentAtTheInitialState() {
        Set<String> selected = Set.of(
                "village",
                "shipwreck",
                "monument",
                "fortress",
                "end_city");
        Set<String> overworldIds = Set.of(
                "village",
                "shipwreck",
                "monument",
                "trial_chambers");
        Set<String> overworldDefaults = Set.of("village", "shipwreck", "monument");

        assertEquals(selected,
                FilterSelectionPolicy.resetStructureFilter(
                        selected, overworldIds, overworldDefaults));
    }

    @Test
    void structureFilterResetRejectsDefaultsOutsideTheActiveDimension() {
        assertThrows(IllegalArgumentException.class, () ->
                FilterSelectionPolicy.resetStructureFilter(
                        Set.of("village"),
                        Set.of("village", "shipwreck"),
                        Set.of("village", "fortress")));
    }

    @Test
    void structureFiltersNeverShrinkTheWarmDiscoveryTargets() {
        Set<String> requested = Set.of("village", "shipwreck", "monument");

        assertEquals(requested, FilterSelectionPolicy.expandStructureScanTargets(
                requested, Set.of("village")));
    }

    @Test
    void newlyEnabledStructuresJoinTheWarmDiscoveryTargetsMonotonically() {
        Set<String> expanded = FilterSelectionPolicy.expandStructureScanTargets(
                Set.of("village", "shipwreck"), Set.of("village", "mineshaft"));

        assertEquals(Set.of("village", "shipwreck", "mineshaft"), expanded);
        assertEquals(expanded, FilterSelectionPolicy.expandStructureScanTargets(
                expanded, Set.of("village")));
    }

    @Test
    void structureTargetsMustBelongToTheActiveDimension() {
        assertThrows(IllegalArgumentException.class, () ->
                FilterSelectionPolicy.applyStructureAction(
                        Set.of("village"), "fortress", REPLACE,
                        Set.of("village", "shipwreck")));
    }

    @Test
    void resultsSnapshotMutableInputSelections() {
        Set<String> selected = new HashSet<>(Set.of("minecraft:plains"));
        Set<String> result = FilterSelectionPolicy.applyBiomeAction(selected, null, NONE);

        selected.add("minecraft:forest");

        assertEquals(Set.of("minecraft:plains"), result);
        assertThrows(UnsupportedOperationException.class,
                () -> result.add("minecraft:desert"));
    }
}
