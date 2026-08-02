package fr.asashiin.worldfinder.common.worldgen;

import java.util.Optional;

/** Ordered climate-parameter point topology used by the vanilla Overworld biome index. */
public enum OverworldParameterTopology {
    /** Parameter points and their order used from Minecraft 1.21.1 through 26.1.2. */
    BASELINE(null, null),
    /** Baseline topology plus the ordered Minecraft 26.2 Sulfur Caves point. */
    SULFUR_CAVES_26_2(
            SulfurCavesClimatePointFixture.POINT,
            SulfurCavesClimatePointFixture.INSERTION
    );

    private final SulfurCavesClimatePointFixture.QuantizedClimatePoint addedPoint;
    private final SulfurCavesClimatePointFixture.ParameterListInsertion insertion;

    OverworldParameterTopology(
            SulfurCavesClimatePointFixture.QuantizedClimatePoint addedPoint,
            SulfurCavesClimatePointFixture.ParameterListInsertion insertion
    ) {
        if ((addedPoint == null) != (insertion == null)) {
            throw new IllegalArgumentException(
                    "An added climate point and its insertion order must be declared together");
        }
        this.addedPoint = addedPoint;
        this.insertion = insertion;
    }

    /**
     * Returns the Sulfur Caves point added by this topology.
     *
     * @return added point, or empty for the baseline topology
     */
    public Optional<SulfurCavesClimatePointFixture.QuantizedClimatePoint> addedPoint() {
        return Optional.ofNullable(addedPoint);
    }

    /**
     * Returns the ordered-list insertion anchors for the added point.
     *
     * @return insertion anchors, or empty for the baseline topology
     */
    public Optional<SulfurCavesClimatePointFixture.ParameterListInsertion> insertion() {
        return Optional.ofNullable(insertion);
    }
}
