package fr.asashiin.worldfinder.client.screen;

public final class BiomePalette {
	private BiomePalette() {
	}

	public static String readableName(String biomeId) {
		String name = biomeId;
		int separator = name.indexOf(':');
		if (separator >= 0 && separator + 1 < name.length()) {
			name = name.substring(separator + 1);
		}
		return name.replace('_', ' ');
	}

	public static int color(String biomeId) {
		return switch (biomeId) {
			case "minecraft:ocean" -> rgb(0x0000C8);
			case "minecraft:deep_ocean" -> rgb(0x202080);
			case "minecraft:frozen_ocean", "minecraft:frozen_river" -> rgb(0xA8A8FF);
			case "minecraft:cold_ocean", "minecraft:deep_cold_ocean" -> rgb(0x6060D8);
			case "minecraft:deep_frozen_ocean" -> rgb(0x7878D8);
			case "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean" -> rgb(0x008080);
			case "minecraft:river" -> rgb(0x0000FF);
			case "minecraft:beach" -> rgb(0xFADE55);
			case "minecraft:snowy_beach", "minecraft:snowy_plains", "minecraft:ice_spikes" -> rgb(0xDCEFF3);
			case "minecraft:plains" -> rgb(0x8DB360);
			case "minecraft:sunflower_plains", "minecraft:meadow" -> rgb(0xB5DB88);
			case "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest" -> rgb(0x056621);
			case "minecraft:old_growth_birch_forest" -> rgb(0x307444);
			case "minecraft:dark_forest" -> rgb(0x40511A);
			case "minecraft:pale_garden" -> rgb(0x8F9B8E);
			case "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga" -> rgb(0x0B6659);
			case "minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes", "minecraft:frozen_peaks" -> rgb(0xA8D5C0);
			case "minecraft:cherry_grove" -> rgb(0xF2A7C8);
			case "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle" -> rgb(0x537B09);
			case "minecraft:mangrove_swamp", "minecraft:swamp" -> rgb(0x07F9B2);
			case "minecraft:desert" -> rgb(0xFA9418);
			case "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna" -> rgb(0xC9B35A);
			case "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands" -> rgb(0xC96D3C);
			case "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills", "minecraft:stony_shore" -> rgb(0x8C9182);
			case "minecraft:dripstone_caves" -> rgb(0x8B735B);
			case "minecraft:lush_caves" -> rgb(0x4CAF50);
			case "minecraft:deep_dark" -> rgb(0x263244);
			case "minecraft:mushroom_fields" -> rgb(0xB06AC8);
			case "minecraft:nether_wastes" -> rgb(0xB14438);
			case "minecraft:crimson_forest" -> rgb(0xB91C1C);
			case "minecraft:warped_forest" -> rgb(0x138D8D);
			case "minecraft:soul_sand_valley" -> rgb(0x72513C);
			case "minecraft:basalt_deltas" -> rgb(0x4E4E57);
			case "minecraft:the_end" -> rgb(0x070B14);
			case "minecraft:end_highlands" -> rgb(0xF0EFB7);
			case "minecraft:end_midlands" -> rgb(0xDAD89B);
			case "minecraft:small_end_islands" -> rgb(0xC7C37D);
			case "minecraft:end_barrens" -> rgb(0xA9A56C);
			default -> rgb(0x74A15A);
		};
	}

	public static boolean hasExplicitColor(String biomeId) {
		return switch (biomeId) {
			case "minecraft:ocean", "minecraft:deep_ocean", "minecraft:frozen_ocean", "minecraft:frozen_river",
					"minecraft:cold_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_frozen_ocean",
					"minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean",
					"minecraft:river", "minecraft:beach", "minecraft:snowy_beach", "minecraft:snowy_plains",
					"minecraft:ice_spikes", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
					"minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
					"minecraft:dark_forest", "minecraft:pale_garden", "minecraft:taiga", "minecraft:old_growth_pine_taiga",
					"minecraft:old_growth_spruce_taiga", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes",
					"minecraft:frozen_peaks", "minecraft:cherry_grove", "minecraft:jungle", "minecraft:sparse_jungle",
					"minecraft:bamboo_jungle", "minecraft:mangrove_swamp", "minecraft:swamp", "minecraft:desert",
					"minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:badlands",
					"minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:jagged_peaks", "minecraft:stony_peaks",
					"minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills",
					"minecraft:stony_shore", "minecraft:dripstone_caves", "minecraft:lush_caves", "minecraft:deep_dark",
					"minecraft:mushroom_fields", "minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest",
					"minecraft:soul_sand_valley", "minecraft:basalt_deltas", "minecraft:the_end", "minecraft:end_highlands",
					"minecraft:end_midlands", "minecraft:small_end_islands", "minecraft:end_barrens" -> true;
			default -> false;
		};
	}

	private static int rgb(int rgb) {
		return 0xFF000000 | rgb;
	}
}
