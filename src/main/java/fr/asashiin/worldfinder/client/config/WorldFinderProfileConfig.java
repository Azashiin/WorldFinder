package fr.asashiin.worldfinder.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import fr.asashiin.worldfinder.WorldFinderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

public final class WorldFinderProfileConfig {
	private static final String FILE_NAME = "worldfinder.properties";

	private final Path file;
	private final Properties properties = new Properties();

	private WorldFinderProfileConfig(Path file) {
		this.file = file;
		load();
	}

	public static WorldFinderProfileConfig resolve(Minecraft minecraft) {
		Path gameDirectory = minecraft.gameDirectory.toPath();
		IntegratedServer server = minecraft.hasSingleplayerServer() ? minecraft.getSingleplayerServer() : null;
		if (server != null) {
			Path file = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
			WorldFinderProfileConfig config = new WorldFinderProfileConfig(file);
			config.setMetadata("profileType", "singleplayer");
			config.setMetadata("worldName", server.getWorldData().getLevelName());
			return config;
		}

		ServerData serverData = minecraft.getCurrentServer();
		if (serverData != null) {
			String identity = !blank(serverData.ip) ? serverData.ip : serverData.name;
			Path file = gameDirectory.resolve("config").resolve("WorldFinder").resolve("Servers").resolve(safeFileName(identity) + ".properties");
			WorldFinderProfileConfig config = new WorldFinderProfileConfig(file);
			config.setMetadata("profileType", "server");
			config.setMetadata("serverName", serverData.name);
			config.setMetadata("serverIp", serverData.ip);
			return config;
		}

		Path file = gameDirectory.resolve("config").resolve("WorldFinder").resolve("Manual Seeds").resolve(FILE_NAME);
		WorldFinderProfileConfig config = new WorldFinderProfileConfig(file);
		config.setMetadata("profileType", "manual");
		return config;
	}

	public String seed() {
		return string("seed", WorldFinderConfig.SEED.get());
	}

	public void seed(String seed) {
		set("seed", seed);
	}

	public String dimension() {
		return string("dimension", WorldFinderConfig.DIMENSION.get());
	}

	public void dimension(String dimension) {
		set("dimension", dimension);
	}

	public String biomeLayer() {
		return string("biomeLayer", WorldFinderConfig.BIOME_LAYER.get());
	}

	public void biomeLayer(String biomeLayer) {
		set("biomeLayer", biomeLayer);
	}

	public String biomeRenderMode() {
		return string("biomeRenderMode", WorldFinderConfig.BIOME_RENDER_MODE.get());
	}

	public void biomeRenderMode(String biomeRenderMode) {
		set("biomeRenderMode", biomeRenderMode);
	}

	public boolean showSlimeChunks() {
		return bool("showSlimeChunks", WorldFinderConfig.SHOW_SLIME_CHUNKS.get());
	}

	public void showSlimeChunks(boolean value) {
		set("showSlimeChunks", Boolean.toString(value));
	}

	public boolean showStructuresOnMap() {
		return bool("showStructuresOnMap", WorldFinderConfig.SHOW_STRUCTURES_ON_MAP.get());
	}

	public void showStructuresOnMap(boolean value) {
		set("showStructuresOnMap", Boolean.toString(value));
	}

	public boolean showChunkBorders() {
		return bool("showChunkBorders", WorldFinderConfig.SHOW_CHUNK_BORDERS.get());
	}

	public void showChunkBorders(boolean value) {
		set("showChunkBorders", Boolean.toString(value));
	}

	public boolean showBiomeColors() {
		return bool("showBiomeColors", WorldFinderConfig.SHOW_BIOME_COLORS.get());
	}

	public void showBiomeColors(boolean value) {
		set("showBiomeColors", Boolean.toString(value));
	}

	public String enabledStructureFilters() {
		return string("enabledStructureFilters", WorldFinderConfig.DEFAULT_ENABLED_STRUCTURE_FILTERS);
	}

	public void enabledStructureFilters(String value) {
		set("enabledStructureFilters", value);
	}

	public String selectedBiomeFilters() {
		return string("selectedBiomeFilters", WorldFinderConfig.SELECTED_BIOME_FILTERS.get());
	}

	public void selectedBiomeFilters(String value) {
		set("selectedBiomeFilters", value);
	}

	public Path completedStructuresFile() {
		return file.resolveSibling("completed-structures.txt");
	}

	public Path file() {
		return file;
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				properties.store(writer, "World Finder profile config");
			}
		} catch (IOException | RuntimeException ignored) {
		}
	}

	private void load() {
		try {
			if (!Files.isRegularFile(file)) {
				return;
			}
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
		} catch (IOException | RuntimeException ignored) {
		}
	}

	private String string(String key, String fallback) {
		return properties.getProperty(key, fallback == null ? "" : fallback);
	}

	private boolean bool(String key, boolean fallback) {
		String value = properties.getProperty(key);
		return value == null ? fallback : Boolean.parseBoolean(value);
	}

	private void set(String key, String value) {
		properties.setProperty(key, value == null ? "" : value);
	}

	private void setMetadata(String key, String value) {
		if (!blank(value)) {
			properties.setProperty(key, value);
		}
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private static String safeFileName(String value) {
		String normalized = value == null ? "unknown" : value.toLowerCase(Locale.ROOT).trim();
		normalized = normalized.replaceAll("[^a-z0-9._-]+", "_");
		normalized = normalized.replaceAll("_+", "_");
		normalized = normalized.replaceAll("^_+|_+$", "");
		return normalized.isBlank() ? "unknown" : normalized;
	}
}
