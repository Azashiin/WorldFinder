package fr.asashiin.worldfinder.api.target;

import java.util.Set;

/**
 * Stable identifiers used when an addon resolves one of WorldFinder's built-in structure filters.
 *
 * <p>These are resolver target identifiers, not Minecraft registry identifiers. An addon should
 * compare {@code StructureQuery.targetId()} against these constants and return
 * {@code ResolverResult.unhandled()} for targets it does not own.</p>
 */
public final class WorldFinderStructureTargets {
    /** Built-in Overworld structure targets. */
    public static final class Overworld {
        /** Village filter target. */
        public static final String VILLAGE = "worldfinder:village";
        /** Ancient City filter target. */
        public static final String ANCIENT_CITY = "worldfinder:ancient_city";
        /** Trial Chambers filter target. */
        public static final String TRIAL_CHAMBERS = "worldfinder:trial_chambers";
        /** Desert Pyramid filter target. */
        public static final String DESERT_PYRAMID = "worldfinder:desert_pyramid";
        /** Jungle Pyramid filter target. */
        public static final String JUNGLE_PYRAMID = "worldfinder:jungle_pyramid";
        /** Swamp Hut filter target. */
        public static final String SWAMP_HUT = "worldfinder:swamp_hut";
        /** Igloo without basement filter target. */
        public static final String IGLOO = "worldfinder:igloo";
        /** Igloo with basement filter target. */
        public static final String IGLOO_BASEMENT = "worldfinder:igloo_basement";
        /** Ocean Monument filter target. */
        public static final String MONUMENT = "worldfinder:monument";
        /** Cold and warm Ocean Ruins filter target. */
        public static final String OCEAN_RUINS = "worldfinder:ocean_ruins";
        /** Shipwreck filter target. */
        public static final String SHIPWRECK = "worldfinder:shipwreck";
        /** Buried Treasure filter target. */
        public static final String BURIED_TREASURE = "worldfinder:buried_treasure";
        /** Mineshaft filter target. */
        public static final String MINESHAFT = "worldfinder:mineshaft";
        /** Pillager Outpost filter target. */
        public static final String PILLAGER_OUTPOST = "worldfinder:pillager_outpost";
        /** Woodland Mansion filter target. */
        public static final String MANSION = "worldfinder:mansion";
        /** Overworld Ruined Portal filter target. */
        public static final String RUINED_PORTAL = "worldfinder:ruined_portal";
        /** Trail Ruins filter target. */
        public static final String TRAIL_RUINS = "worldfinder:trail_ruins";
        /** Stronghold filter target. */
        public static final String STRONGHOLD = "worldfinder:stronghold";

        /** Every built-in Overworld target. */
        public static final Set<String> ALL = Set.of(
                VILLAGE, ANCIENT_CITY, TRIAL_CHAMBERS, DESERT_PYRAMID, JUNGLE_PYRAMID,
                SWAMP_HUT, IGLOO, IGLOO_BASEMENT, MONUMENT, OCEAN_RUINS, SHIPWRECK,
                BURIED_TREASURE, MINESHAFT, PILLAGER_OUTPOST, MANSION, RUINED_PORTAL,
                TRAIL_RUINS, STRONGHOLD
        );

        private Overworld() {
        }
    }

    /** Built-in Nether structure targets. */
    public static final class Nether {
        /** Nether Ruined Portal filter target. */
        public static final String RUINED_PORTAL = "worldfinder:nether_portal";
        /** Nether Fortress filter target. */
        public static final String FORTRESS = "worldfinder:fortress";
        /** Nether Fossil filter target. */
        public static final String NETHER_FOSSIL = "worldfinder:nether_fossil";
        /** Fortress blaze-spawner filter target. */
        public static final String FORTRESS_BLAZE_SPAWNER = "worldfinder:fortress_blaze_spawner";
        /** Generic Bastion Remnant filter target. */
        public static final String BASTION = "worldfinder:bastion";
        /** Bastion housing-units filter target. */
        public static final String BASTION_HOUSING = "worldfinder:bastion_housing";
        /** Bastion hoglin-stable filter target. */
        public static final String BASTION_HOGLIN_STABLE = "worldfinder:bastion_hoglin_stable";
        /** Bastion treasure-room filter target. */
        public static final String BASTION_TREASURE = "worldfinder:bastion_treasure";
        /** Bastion bridge filter target. */
        public static final String BASTION_BRIDGE = "worldfinder:bastion_bridge";

        /** Every built-in Nether target. */
        public static final Set<String> ALL = Set.of(
                RUINED_PORTAL, FORTRESS, NETHER_FOSSIL, FORTRESS_BLAZE_SPAWNER,
                BASTION, BASTION_HOUSING, BASTION_HOGLIN_STABLE, BASTION_TREASURE,
                BASTION_BRIDGE
        );

        private Nether() {
        }
    }

    /** Built-in End structure targets. */
    public static final class End {
        /** Generic End City filter target. */
        public static final String END_CITY = "worldfinder:end_city";
        /** End City without ship filter target. */
        public static final String END_CITY_WITHOUT_SHIP = "worldfinder:end_city_without_ship";
        /** End City with ship filter target. */
        public static final String END_CITY_SHIP = "worldfinder:end_city_ship";
        /** End Gateway filter target. */
        public static final String END_GATEWAY = "worldfinder:end_gateway";

        /** Every built-in End target. */
        public static final Set<String> ALL = Set.of(
                END_CITY, END_CITY_WITHOUT_SHIP, END_CITY_SHIP, END_GATEWAY
        );

        private End() {
        }
    }

    /** Every built-in structure target across all three vanilla dimensions. */
    public static final Set<String> ALL = Set.of(
            Overworld.VILLAGE, Overworld.ANCIENT_CITY, Overworld.TRIAL_CHAMBERS,
            Overworld.DESERT_PYRAMID, Overworld.JUNGLE_PYRAMID, Overworld.SWAMP_HUT,
            Overworld.IGLOO, Overworld.IGLOO_BASEMENT, Overworld.MONUMENT,
            Overworld.OCEAN_RUINS, Overworld.SHIPWRECK, Overworld.BURIED_TREASURE,
            Overworld.MINESHAFT, Overworld.PILLAGER_OUTPOST, Overworld.MANSION,
            Overworld.RUINED_PORTAL, Overworld.TRAIL_RUINS, Overworld.STRONGHOLD,
            Nether.RUINED_PORTAL, Nether.FORTRESS, Nether.NETHER_FOSSIL,
            Nether.FORTRESS_BLAZE_SPAWNER, Nether.BASTION, Nether.BASTION_HOUSING,
            Nether.BASTION_HOGLIN_STABLE, Nether.BASTION_TREASURE, Nether.BASTION_BRIDGE,
            End.END_CITY, End.END_CITY_WITHOUT_SHIP, End.END_CITY_SHIP, End.END_GATEWAY
    );

    private WorldFinderStructureTargets() {
    }
}
