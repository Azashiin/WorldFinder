package fr.asashiin.worldfinder.common.ui;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.common.map.BiomeVerticalSelection;
import fr.asashiin.worldfinder.common.worldgen.OverworldBiomeVerticalProfile;

import java.util.Objects;

/** Pure interaction and presentation policy for the cyclic biome-height button. */
public final class BiomeVerticalControl {
    public static final String OVERWORLD_TOOLTIP =
            "Overworld biome layer: Underground, Surface, or Highlight. "
                    + "Left click: next; right click: previous.";
    public static final String OTHER_DIMENSION_TOOLTIP =
            "Biome height layers are available only in the Overworld.";

    private BiomeVerticalControl() {
    }

    /** Returns the visible state while retaining the remembered Overworld selection everywhere. */
    public static State state(
            OverworldBiomeVerticalProfile profile,
            WorldDimension dimension,
            BiomeVerticalSelection overworldSelection
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(overworldSelection, "overworldSelection");
        boolean active = dimension == WorldDimension.OVERWORLD;
        return new State(
                profile.displayName(overworldSelection),
                active,
                active ? OVERWORLD_TOOLTIP : OTHER_DIMENSION_TOOLTIP
        );
    }

    /** Applies a left/right click, or returns the unchanged selection when disabled. */
    public static BiomeVerticalSelection click(
            OverworldBiomeVerticalProfile profile,
            WorldDimension dimension,
            BiomeVerticalSelection current,
            PointerButton button
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(button, "button");
        if (dimension != WorldDimension.OVERWORLD) {
            return current;
        }
        return switch (button) {
            case LEFT -> profile.next(current);
            case RIGHT -> profile.previous(current);
            case OTHER -> current;
        };
    }

    public enum PointerButton {
        LEFT,
        RIGHT,
        OTHER
    }

    /** Immutable button label, enabled state, and explanatory tooltip. */
    public record State(String label, boolean active, String tooltip) {
        public State {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(tooltip, "tooltip");
        }
    }
}
