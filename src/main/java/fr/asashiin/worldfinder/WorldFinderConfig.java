package fr.asashiin.worldfinder;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WorldFinderConfig {
	public static final String DEFAULT_ENABLED_STRUCTURE_FILTERS = String.join(",",
			"overworld/village",
			"overworld/trial_chambers",
			"overworld/desert_pyramid",
			"overworld/jungle_temple",
			"overworld/igloo",
			"overworld/ocean_ruins",
			"overworld/shipwreck",
			"overworld/buried_treasure",
			"overworld/ruined_portal");

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	public static final ModConfigSpec.ConfigValue<String> SEED = BUILDER
			.comment("Server/world seed used for seed-based vanilla worldgen searches.")
			.define("seed", "");

	public static final ModConfigSpec.ConfigValue<String> DIMENSION = BUILDER
			.comment("Selected target dimension for map and seed searches: overworld, the_nether, or the_end.")
			.define("dimension", "overworld");

	public static final ModConfigSpec.IntValue SEARCH_RADIUS_CHUNKS = BUILDER
			.comment("Default search radius in chunks.")
			.defineInRange("searchRadiusChunks", 128, 8, 2048);

	public static final ModConfigSpec.IntValue MAX_RESULTS = BUILDER
			.comment("Maximum results shown after a search.")
			.defineInRange("maxResults", 12, 1, 50);

	public static final ModConfigSpec.BooleanValue SHOW_SLIME_CHUNKS = BUILDER
			.comment("Show vanilla slime chunks on the map when a seed is available.")
			.define("showSlimeChunks", true);

	public static final ModConfigSpec.BooleanValue SHOW_STRUCTURES_ON_MAP = BUILDER
			.comment("Show seed-based structure markers on the full-screen map when a seed is available.")
			.define("showStructuresOnMap", true);

	public static final ModConfigSpec.BooleanValue SHOW_CHUNK_BORDERS = BUILDER
			.comment("Show chunk border grid lines on the full-screen map.")
			.define("showChunkBorders", true);

	public static final ModConfigSpec.BooleanValue SHOW_BIOME_COLORS = BUILDER
			.comment("Color loaded chunks on the map using the biome sampled at chunk center.")
			.define("showBiomeColors", true);

	public static final ModConfigSpec.ConfigValue<String> ENABLED_STRUCTURE_FILTERS = BUILDER
			.comment("Comma-separated structure filter keys enabled on the World Finder map.")
			.define("enabledStructureFilters", DEFAULT_ENABLED_STRUCTURE_FILTERS);

	public static final ModConfigSpec.ConfigValue<String> SELECTED_BIOME_FILTERS = BUILDER
			.comment("Comma-separated biome ids highlighted on the World Finder map. Empty means no biome filter.")
			.define("selectedBiomeFilters", "");

	public static final ModConfigSpec.ConfigValue<String> BIOME_LAYER = BUILDER
			.comment("Biome layer shown on the World Finder map: surface or underground.")
			.define("biomeLayer", "surface");

	public static final ModConfigSpec.ConfigValue<String> BIOME_RENDER_MODE = BUILDER
			.comment("Biome map render mode: fast uses a SeedMapper-like raw biome map, detailed enables filters, underground sampling, and End terrain masking.")
			.define("biomeRenderMode", "detailed");

	static final ModConfigSpec SPEC = BUILDER.build();

	private WorldFinderConfig() {
	}
}
