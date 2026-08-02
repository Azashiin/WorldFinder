package fr.asashiin.worldfinder.api.world;

/** Vanilla dimension families understood by the WorldFinder 0.2 API. */
public enum WorldDimension {
    /** The primary surface dimension. */
    OVERWORLD("overworld", 1.0),
    /** The Nether, where one coordinate unit corresponds to eight Overworld units. */
    NETHER("the_nether", 8.0),
    /** The End dimension. */
    END("the_end", 1.0);

    private final String serializedName;
    private final double overworldCoordinateScale;

    WorldDimension(String serializedName, double overworldCoordinateScale) {
        this.serializedName = serializedName;
        this.overworldCoordinateScale = overworldCoordinateScale;
    }

    /**
     * Returns the stable dimension name used by WorldFinder serialization.
     *
     * @return lowercase vanilla dimension name without namespace
     */
    public String serializedName() {
        return serializedName;
    }

    /**
     * Returns the number of Overworld coordinate units represented by one unit in this dimension.
     *
     * @return positive coordinate scale
     */
    public double overworldCoordinateScale() {
        return overworldCoordinateScale;
    }
}
