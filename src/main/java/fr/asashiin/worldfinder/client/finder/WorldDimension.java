package fr.asashiin.worldfinder.client.finder;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public enum WorldDimension {
	OVERWORLD("Overworld", "overworld", Level.OVERWORLD),
	NETHER("Nether", "the_nether", Level.NETHER),
	END("End", "the_end", Level.END);

	private final String label;
	private final String configName;
	private final ResourceKey<Level> levelKey;

	WorldDimension(String label, String configName, ResourceKey<Level> levelKey) {
		this.label = label;
		this.configName = configName;
		this.levelKey = levelKey;
	}

	public String label() {
		return label;
	}

	public String configName() {
		return configName;
	}

	public ResourceKey<Level> levelKey() {
		return levelKey;
	}

	public WorldDimension next() {
		WorldDimension[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static WorldDimension fromLevelKey(ResourceKey<Level> levelKey) {
		if (levelKey != null) {
			for (WorldDimension dimension : values()) {
				if (dimension.levelKey.equals(levelKey)) {
					return dimension;
				}
			}
		}
		return OVERWORLD;
	}

	public static WorldDimension fromConfig(String value) {
		for (WorldDimension dimension : values()) {
			if (dimension.configName.equals(value)) {
				return dimension;
			}
		}
		return OVERWORLD;
	}
}
