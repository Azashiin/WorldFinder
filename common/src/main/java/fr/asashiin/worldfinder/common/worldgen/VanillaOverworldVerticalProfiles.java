package fr.asashiin.worldfinder.common.worldgen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact vertical map bands attached to supported vanilla Normal Overworld revisions. */
final class VanillaOverworldVerticalProfiles {
    private static final List<Integer> BASE_LEVELS = List.of(64, -51);
    private static final Map<String, Set<Integer>> BASE_UNDERGROUND = Map.of(
            "minecraft:deep_dark", Set.of(-51),
            "minecraft:dripstone_caves", Set.copyOf(BASE_LEVELS),
            "minecraft:lush_caves", Set.copyOf(BASE_LEVELS)
    );

    static final OverworldBiomeVerticalProfile BASELINE = profile(BASE_UNDERGROUND);
    static final OverworldBiomeVerticalProfile SULFUR_CAVES_26_2 = sulfurProfile();

    private VanillaOverworldVerticalProfiles() {
    }

    private static OverworldBiomeVerticalProfile sulfurProfile() {
        LinkedHashMap<String, Set<Integer>> levels = new LinkedHashMap<>(BASE_UNDERGROUND);
        levels.put("minecraft:sulfur_caves", Set.of(-51));
        return profile(levels);
    }

    private static OverworldBiomeVerticalProfile profile(Map<String, Set<Integer>> underground) {
        // Every currently supported vanilla Normal revision retains the 1.18+ -64..319 range.
        // The data lives on the revision, rather than in the UI, so a future profile can replace
        // both limits and layer bands without a version-specific screen implementation.
        return new OverworldBiomeVerticalProfile(-64, 320, 64, BASE_LEVELS, underground);
    }
}
