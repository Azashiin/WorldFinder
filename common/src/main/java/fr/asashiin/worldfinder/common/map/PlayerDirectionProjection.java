package fr.asashiin.worldfinder.common.map;

/** Converts Minecraft yaw into the clockwise rotation of an upward-pointing map arrow. */
public final class PlayerDirectionProjection {
    private PlayerDirectionProjection() {
    }

    /**
     * Converts Minecraft yaw to the clockwise rotation of an initially upward-pointing map arrow.
     *
     * @param minecraftYaw entity yaw in degrees
     * @return normalized clockwise rotation in the range {@code [0, 360)}
     */
    public static float arrowRotationDegrees(float minecraftYaw) {
        float rotation = (minecraftYaw + 180.0F) % 360.0F;
        return rotation < 0.0F ? rotation + 360.0F : rotation;
    }
}
