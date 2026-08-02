package fr.asashiin.worldfinder.common.worldgen;

import java.util.Objects;
import java.util.Optional;

/**
 * Pale Garden placement in the vanilla Overworld plateau lookup tables.
 *
 * <p>The temperature and humidity indices are zero-based and match Mojang's five-by-five
 * {@code PLATEAU_BIOMES} tables. This is generation data, not a statement that the current binary
 * contains the corresponding biome registry entry.</p>
 */
public enum PaleGardenPlacement {
    /** Pale Garden is absent from the Overworld climate table. */
    ABSENT(null),
    /** Pale Garden occupies {@code PLATEAU_BIOMES_VARIANT[2][4]}. */
    PLATEAU_VARIANT_T2_H4(new PlateauCell(PlateauTable.VARIANT, 2, 4)),
    /** Pale Garden occupies {@code PLATEAU_BIOMES[2][4]}. */
    PLATEAU_BASE_T2_H4(new PlateauCell(PlateauTable.BASE, 2, 4));

    private final PlateauCell plateauCell;

    PaleGardenPlacement(PlateauCell plateauCell) {
        this.plateauCell = plateauCell;
    }

    /**
     * Returns the occupied plateau cell, if Pale Garden is present.
     *
     * @return exact table and zero-based indices, or an empty value for {@link #ABSENT}
     */
    public Optional<PlateauCell> plateauCell() {
        return Optional.ofNullable(plateauCell);
    }

    /** Plateau lookup table containing a biome value. */
    public enum PlateauTable {
        /** Main {@code PLATEAU_BIOMES} table. */
        BASE,
        /** Positive-weirdness {@code PLATEAU_BIOMES_VARIANT} table. */
        VARIANT
    }

    /**
     * One exact cell in a vanilla plateau lookup table.
     *
     * @param table main or positive-weirdness variant table
     * @param temperatureIndex zero-based temperature band
     * @param humidityIndex zero-based humidity band
     */
    public record PlateauCell(
            PlateauTable table,
            int temperatureIndex,
            int humidityIndex
    ) {
        /** Validates the five-by-five vanilla lookup coordinates. */
        public PlateauCell {
            Objects.requireNonNull(table, "table");
            if (temperatureIndex < 0 || temperatureIndex >= 5
                    || humidityIndex < 0 || humidityIndex >= 5) {
                throw new IllegalArgumentException(
                        "Vanilla plateau indices must be between 0 and 4");
            }
        }
    }
}
