package fr.asashiin.worldfinder.client.finder;

import java.util.List;
import java.util.Locale;

import fr.asashiin.worldfinder.client.finder.WorldDimension;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

public record StructurePreset(String label, WorldDimension dimension, List<String> structureIds, int spacing, int separation, RandomSpreadType spreadType, int salt, String note) {
	public static final List<StructurePreset> PRESETS = List.of(
			new StructurePreset("Village", WorldDimension.OVERWORLD, List.of("minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"), 34, 8, RandomSpreadType.LINEAR, 10387312, "seed-based"),
			new StructurePreset("Abandoned Village", WorldDimension.OVERWORLD, List.of("minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"), 34, 8, RandomSpreadType.LINEAR, 10387312, "seed-based variant"),
			new StructurePreset("Ancient City", WorldDimension.OVERWORLD, List.of("minecraft:ancient_city"), 24, 8, RandomSpreadType.LINEAR, 20083232, "seed-based; deep dark"),
			new StructurePreset("Trial Chambers", WorldDimension.OVERWORLD, List.of("minecraft:trial_chambers"), 34, 12, RandomSpreadType.LINEAR, 94251327, "seed-based"),
			new StructurePreset("Desert Pyramid", WorldDimension.OVERWORLD, List.of("minecraft:desert_pyramid"), 32, 8, RandomSpreadType.LINEAR, 14357617, "seed-based; desert"),
			new StructurePreset("Jungle Temple", WorldDimension.OVERWORLD, List.of("minecraft:jungle_pyramid"), 32, 8, RandomSpreadType.LINEAR, 14357619, "seed-based; jungle"),
			new StructurePreset("Swamp Hut", WorldDimension.OVERWORLD, List.of("minecraft:swamp_hut"), 32, 8, RandomSpreadType.LINEAR, 14357620, "seed-based; swamp"),
			new StructurePreset("Igloo", WorldDimension.OVERWORLD, List.of("minecraft:igloo"), 32, 8, RandomSpreadType.LINEAR, 14357618, "seed-based; snowy biome"),
			new StructurePreset("Ocean Monument", WorldDimension.OVERWORLD, List.of("minecraft:monument"), 32, 5, RandomSpreadType.TRIANGULAR, 10387313, "seed-based; ocean"),
			new StructurePreset("Ocean Ruins", WorldDimension.OVERWORLD, List.of("minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm"), 20, 8, RandomSpreadType.LINEAR, 14357621, "seed-based; ocean"),
			new StructurePreset("Shipwreck", WorldDimension.OVERWORLD, List.of("minecraft:shipwreck", "minecraft:shipwreck_beached"), 24, 4, RandomSpreadType.LINEAR, 165745295, "seed-based"),
			new StructurePreset("Buried Treasure", WorldDimension.OVERWORLD, List.of("minecraft:buried_treasure"), 1, 0, RandomSpreadType.LINEAR, 10387320, "seed-based; beach/ocean"),
			new StructurePreset("Mineshaft", WorldDimension.OVERWORLD, List.of("minecraft:mineshaft", "minecraft:mineshaft_mesa"), 1, 0, RandomSpreadType.LINEAR, 0, "seed-based underground"),
			new StructurePreset("Pillager Outpost", WorldDimension.OVERWORLD, List.of("minecraft:pillager_outpost"), 32, 8, RandomSpreadType.LINEAR, 165745296, "seed-based"),
			new StructurePreset("Woodland Mansion", WorldDimension.OVERWORLD, List.of("minecraft:mansion"), 80, 20, RandomSpreadType.TRIANGULAR, 10387319, "seed-based; dark forest"),
			new StructurePreset("Ruined Portal", WorldDimension.OVERWORLD, List.of("minecraft:ruined_portal", "minecraft:ruined_portal_desert", "minecraft:ruined_portal_jungle", "minecraft:ruined_portal_swamp", "minecraft:ruined_portal_mountain", "minecraft:ruined_portal_ocean"), 40, 15, RandomSpreadType.LINEAR, 34222645, "seed-based"),
			new StructurePreset("Trail Ruins", WorldDimension.OVERWORLD, List.of("minecraft:trail_ruins"), 34, 8, RandomSpreadType.LINEAR, 83469867, "seed-based"),
			new StructurePreset("Geode", WorldDimension.OVERWORLD, List.of("minecraft:geode"), 1, 0, RandomSpreadType.LINEAR, 0, "seed-based feature"),
			new StructurePreset("Copper Ore Vein", WorldDimension.OVERWORLD, List.of(), 1, 0, RandomSpreadType.LINEAR, 0, "seed-based ore vein"),
			new StructurePreset("Iron Ore Vein", WorldDimension.OVERWORLD, List.of(), 1, 0, RandomSpreadType.LINEAR, 0, "seed-based ore vein"),
			new StructurePreset("Stronghold", WorldDimension.OVERWORLD, List.of("minecraft:stronghold"), 1, 0, RandomSpreadType.LINEAR, 0, "seed-based ring"),
			new StructurePreset("Ruined Portal", WorldDimension.NETHER, List.of("minecraft:ruined_portal_nether"), 40, 15, RandomSpreadType.LINEAR, 34222645, "seed-based Nether"),
			new StructurePreset("Nether Fortress", WorldDimension.NETHER, List.of("minecraft:fortress"), 27, 4, RandomSpreadType.LINEAR, 30084232, "seed-based Nether"),
			new StructurePreset("Fortress Blaze Spawner", WorldDimension.NETHER, List.of("minecraft:fortress"), 27, 4, RandomSpreadType.LINEAR, 30084232, "seed-based fortress piece"),
			new StructurePreset("Bastion Housing Units", WorldDimension.NETHER, List.of("minecraft:bastion_remnant"), 27, 4, RandomSpreadType.LINEAR, 30084232, "seed-based bastion variant"),
			new StructurePreset("Bastion Hoglin Stable", WorldDimension.NETHER, List.of("minecraft:bastion_remnant"), 27, 4, RandomSpreadType.LINEAR, 30084232, "seed-based bastion variant"),
			new StructurePreset("Bastion Treasure Room", WorldDimension.NETHER, List.of("minecraft:bastion_remnant"), 27, 4, RandomSpreadType.LINEAR, 30084232, "seed-based bastion variant"),
			new StructurePreset("Bastion Bridge", WorldDimension.NETHER, List.of("minecraft:bastion_remnant"), 27, 4, RandomSpreadType.LINEAR, 30084232, "seed-based bastion variant"),
			new StructurePreset("End City", WorldDimension.END, List.of("minecraft:end_city"), 20, 11, RandomSpreadType.TRIANGULAR, 10387313, "seed-based End"),
			new StructurePreset("End Gateway", WorldDimension.END, List.of("minecraft:end_gateway"), 20, 11, RandomSpreadType.TRIANGULAR, 10387313, "seed-based End")
	);

	public String configKey() {
		String normalizedLabel = label.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_|_$", "");
		return dimension.configName() + "/" + normalizedLabel;
	}
}
