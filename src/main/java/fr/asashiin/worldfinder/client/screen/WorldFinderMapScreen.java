package fr.asashiin.worldfinder.client.screen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import fr.asashiin.worldfinder.WorldFinderConfig;
import fr.asashiin.worldfinder.client.SafeTeleportScheduler;
import fr.asashiin.worldfinder.client.config.WorldFinderProfileConfig;
import fr.asashiin.worldfinder.client.screen.BiomePalette;
import fr.asashiin.worldfinder.client.finder.FinderResult;
import fr.asashiin.worldfinder.client.finder.StructurePreset;
import fr.asashiin.worldfinder.client.finder.WorldDimension;
import fr.asashiin.worldfinder.client.integration.WaypointIntegrations;
import fr.asashiin.worldfinder.client.maprender.ColorTileTextureManager;
import fr.asashiin.worldfinder.client.seed.SeedMapEngine;
import fr.asashiin.worldfinder.client.seed.SeedMapEngines;
import fr.asashiin.worldfinder.client.seedmap.SeedMapCache;
import fr.asashiin.worldfinder.client.seedmap.SeedMapExecutor;
import fr.asashiin.worldfinder.client.screen.MapIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;

public class WorldFinderMapScreen extends Screen {
	private static final SeedMapExecutor BIOME_EXECUTOR = new SeedMapExecutor("World Finder biome worker");
	private static final SeedMapExecutor STRUCTURE_EXECUTOR = new SeedMapExecutor("World Finder structure worker", 1);
	private static final int MAX_RENDERED_CHUNKS = 22000;
	private static final int BIOME_TILE_SAMPLE_STEP = 4;
	private static final int BIOME_TILE_CELLS = 100;
	private static final int MAX_BIOME_TILE_CACHE_PER_DIMENSION = 1536;
	private static final int MAX_BIOME_TILE_DATA_CACHE = 8192;
	private static final int MAX_BIOME_TILE_ID_CACHE = 2048;
	private static final int MAX_PENDING_BIOME_TEXTURE_UPLOADS = 512;
	private static final int BIOME_TEXTURE_UPLOADS_PER_FRAME = 4;
	private static final int MAX_NEW_BIOME_TILE_REQUESTS_PER_FRAME = 8;
	private static final int MAX_NEW_UNDERGROUND_TILE_REQUESTS_PER_FRAME = 2;
	private static final long MARKER_REBUILD_DELAY_MILLIS = 450L;
	private static final int MARKER_CACHE_CHUNK_STEP = 32;
	private static final int MARKER_CACHE_CHUNK_PADDING = 16;
	private static final int[] EMPTY_PRESET_INDEXES = new int[0];
	private static final int MAP_BACKGROUND = 0x33071018;
	private static final int PANEL_BACKGROUND = 0x9A05080D;
	private static final int PANEL_BORDER = 0x553B82F6;
	private static final int MAP_GRID = 0x304A6075;
	private static final int MAP_AXIS = 0x667EA3C7;
	private static final int MAP_SLIME_CHUNK = 0x6043D17A;
	private static final int MAP_PLAYER = 0xFF55FF7A;
	private static final int MAP_SPAWN = 0xFFFFF2A8;
	private static final int MAP_HOVER = 0xFF93C5FD;
	private static final int MAP_STRONGHOLD = 0xFFD946EF;
	private static final Identifier VANILLA_BUTTON = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier VANILLA_BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final int ICON_SIZE = 18;
	private static final int FILTER_CARD_HEIGHT = 24;
	private static final int FILTER_CARD_GAP = 5;
	private static final int FILTER_CARD_WIDTH = 26;
	private static final int CONTEXT_MENU_WIDTH = 142;
	private static final int CONTEXT_MENU_PADDING = 4;
	private static final int CONTEXT_MENU_HEADER_HEIGHT = 24;
	private static final int CONTEXT_MENU_OPTION_HEIGHT = 22;
	private static final int CONTEXT_MENU_BUTTON_HEIGHT = 20;
	private static final int WAYPOINT_SUBMENU_WIDTH = 120;
	private static final double[] BLOCK_ZOOMS = { 0.25D, 0.5D, 1.0D, 2.0D, 4.0D, 8.0D };
	private static final int[] EMPTY_BIOME_TILE_COLORS = new int[0];
	private static final int END_VOID_TILE_COLOR = 0xFF050813;
	private static final int END_MAIN_ISLAND_SAFE_RADIUS = 192;
	private static final int END_OUTER_ISLAND_START_RADIUS = 1000;
	private static final int BIOME_SUGGESTION_MAX_ROWS = 4;
	private static final int BIOME_SUGGESTION_HEIGHT = 14;
	private static final int[] UNDERGROUND_TILE_SAMPLE_YS = { 40, 8, -24, -52 };
	private static final int[] UNDERGROUND_POINT_SAMPLE_YS = { 51, 40, 24, 8, -8, -24, -40, -52 };
	private static final List<String> OVERWORLD_SURFACE_BIOMES = List.of(
			"minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains", "minecraft:ice_spikes",
			"minecraft:desert", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:forest",
			"minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
			"minecraft:dark_forest", "minecraft:pale_garden", "minecraft:taiga", "minecraft:snowy_taiga",
			"minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:savanna",
			"minecraft:savanna_plateau", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
			"minecraft:windswept_forest", "minecraft:windswept_savanna", "minecraft:jungle",
			"minecraft:sparse_jungle", "minecraft:bamboo_jungle", "minecraft:badlands", "minecraft:eroded_badlands",
			"minecraft:wooded_badlands", "minecraft:meadow", "minecraft:grove", "minecraft:snowy_slopes",
			"minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:cherry_grove",
			"minecraft:river", "minecraft:frozen_river", "minecraft:beach", "minecraft:snowy_beach",
			"minecraft:stony_shore", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:warm_ocean",
			"minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:cold_ocean",
			"minecraft:deep_cold_ocean", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean",
			"minecraft:mushroom_fields"
	);
	private static final List<String> OVERWORLD_UNDERGROUND_BIOMES = List.of(
			"minecraft:dripstone_caves", "minecraft:lush_caves", "minecraft:deep_dark"
	);
	private static final List<String> NETHER_BIOMES = List.of(
			"minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:warped_forest",
			"minecraft:soul_sand_valley", "minecraft:basalt_deltas"
	);
	private static final List<String> END_BIOMES = List.of(
			"minecraft:the_end", "minecraft:end_highlands", "minecraft:end_midlands",
			"minecraft:small_end_islands", "minecraft:end_barrens"
	);
	private static final int[] STRUCTURE_COLORS = {
			0xFFFFD166, 0xFFFF5A5F, 0xFF6EE7B7, 0xFFF97316, 0xFF22C55E,
			0xFF84CC16, 0xFFBAE6FD, 0xFF38BDF8, 0xFF0EA5E9, 0xFFF59E0B,
			0xFF7C3AED, 0xFFFB7185, 0xFFD946EF, 0xFFEF4444, 0xFFA78BFA
	};
	private static final Map<BiomeTileKey, int[]> BIOME_TILE_DATA_CACHE = new LinkedHashMap<>(MAX_BIOME_TILE_DATA_CACHE, 0.75F, true);
	private static final Map<BiomeIdTileKey, String[]> BIOME_TILE_ID_CACHE = new LinkedHashMap<>(MAX_BIOME_TILE_ID_CACHE, 0.75F, true);
	private static final Map<StructureScanKey, List<FinderResult>> STRUCTURE_SCAN_CACHE = new LinkedHashMap<>();
	private static final Map<WorldDimension, int[]> STRUCTURE_PRESETS_BY_DIMENSION = structurePresetIndexesByDimension();

	private final Screen parent;
	private final SeedMapEngine engine;
	private final SeedMapCache<BiomeTileKey, int[]> biomeDataCache = new SeedMapCache<>(BIOME_TILE_DATA_CACHE, BIOME_EXECUTOR, key -> trimBiomeTileDataCache());
	private final SeedMapCache<StructureScanKey, List<FinderResult>> structureScanCache = new SeedMapCache<>(STRUCTURE_SCAN_CACHE, STRUCTURE_EXECUTOR);
	private final ColorTileTextureManager<BiomeTileKey, WorldDimension> biomeTileTextures = new ColorTileTextureManager<>(WorldDimension.class, BiomeTileKey::dimension, MAX_BIOME_TILE_CACHE_PER_DIMENSION, MAX_PENDING_BIOME_TEXTURE_UPLOADS, BIOME_TEXTURE_UPLOADS_PER_FRAME);
	private final Map<BiomeTileKey, Integer> biomeTilePreviewCache = new LinkedHashMap<>(MAX_BIOME_TILE_CACHE_PER_DIMENSION, 0.75F, true);
	private boolean[] structureFilters = defaultStructureFilters(WorldFinderConfig.ENABLED_STRUCTURE_FILTERS.get());
	private Set<String> selectedBiomeFilters = loadBiomeFilters(WorldFinderConfig.SELECTED_BIOME_FILTERS.get());
	private EditBox seedInput;
	private EditBox xSearchInput;
	private EditBox zSearchInput;
	private EditBox biomeSearchInput;
	private String xSearchText = "";
	private String zSearchText = "";
	private String biomeSearchText = "";
	private WorldFinderProfileConfig profileConfig;
	private WorldDimension dimension = WorldDimension.fromConfig(WorldFinderConfig.DIMENSION.get());
	private BiomeLayer biomeLayer = BiomeLayer.fromConfig(WorldFinderConfig.BIOME_LAYER.get());
	private BiomeRenderMode biomeRenderMode = BiomeRenderMode.fromConfig(WorldFinderConfig.BIOME_RENDER_MODE.get());
	private int zoomIndex = 1;
	private double cameraX;
	private double cameraZ;
	private boolean cameraInitialized;
	private boolean dragging;
	private FinderResult hoveredMarker;
	private boolean hoveredPlayer;
	private long hoveredPlayerStartedAt;
	private FinderResult delayedTooltipMarker;
	private long delayedTooltipStartedAt;
	private FinderResult selectedMarker;
	private FinderResult contextMarker;
	private FinderResult completedMenuMarker;
	private int contextX;
	private int contextY;
	private int completedMenuX;
	private int completedMenuY;
	private int panelWidth;
	private int mapLeft;
	private int mapTop;
	private int mapWidth;
	private int mapHeight;
	private int iconGridLeft;
	private int iconGridTop;
	private int iconGridColumns;
	private int iconGridRows;
	private int filterCardWidth = FILTER_CARD_WIDTH;
	private int biomeSuggestionTop;
	private int biomeSuggestionRows;
	private int panelStatusTop;
	private int iconScroll;
	private int hoveredStructureIndex = -1;
	private boolean biomeLayerFailed;
	private ViewKey cachedMarkersKey;
	private ViewKey pendingMarkersKey;
	private long markerRebuildAfter;
	private List<FinderResult> cachedMarkers = List.of();
	private Map<Integer, Integer> cachedStructureCounts = Map.of();
	private long zoomOverlayStartedAt;
	private long zoomOverlayUntil;
	private boolean initialDimensionSynced;
	private Set<String> completedStructureMarkers = new HashSet<>();
	private final Map<MapIcons.SpriteIcon, TextureAtlasSprite> markerSpriteCache = new HashMap<>();
	private boolean singleplayerSeedResolved;
	private Long cachedSingleplayerSeed;
	private long lastSingleplayerSeedAttemptMillis;
	private String cachedManualSeedText = "";
	private Long cachedManualSeed;
	private ItemStack cachedPlayerHeadStack;
	private String cachedPlayerHeadName = "";
	private WorldDimension cachedBiomeSuggestionDimension;
	private BiomeLayer cachedBiomeSuggestionLayer;
	private int cachedBiomeSuggestionRows = -1;
	private String cachedBiomeSuggestionText = "";
	private List<String> cachedBiomeSuggestions = List.of();

	private record ViewKey(long seed, WorldDimension dimension, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, int filterMask, boolean structures) {
	}

	private record BiomeTileKey(long seed, WorldDimension dimension, BiomeLayer layer, int sampleStep, int tileX, int tileZ, String biomeFilterKey) {
	}

	private record BiomeIdTileKey(long seed, WorldDimension dimension, BiomeLayer layer, int sampleStep, int tileX, int tileZ) {
	}

	private record StructureScanKey(long seed, WorldDimension dimension, int presetIndex, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, int originChunkX, int originChunkZ) {
	}

	private enum ContextAction {
		WAYPOINT("Waypoint"),
		CHAT("Chat"),
		TELEPORT("Teleport");

		private final String label;

		ContextAction(String label) {
			this.label = label;
		}
	}

	private enum BiomeLayer {
		SURFACE("surface", "Surf"),
		UNDERGROUND("underground", "Deep");

		private final String configName;
		private final String shortLabel;

		BiomeLayer(String configName, String shortLabel) {
			this.configName = configName;
			this.shortLabel = shortLabel;
		}

		private BiomeLayer next() {
			return this == SURFACE ? UNDERGROUND : SURFACE;
		}

		private static BiomeLayer fromConfig(String value) {
			return "underground".equalsIgnoreCase(value) ? UNDERGROUND : SURFACE;
		}
	}

	private enum BiomeRenderMode {
		FAST("fast", "Fast"),
		DETAILED("detailed", "Detail");

		private final String configName;
		private final String shortLabel;

		BiomeRenderMode(String configName, String shortLabel) {
			this.configName = configName;
			this.shortLabel = shortLabel;
		}

		private BiomeRenderMode next() {
			return this == FAST ? DETAILED : FAST;
		}

		private static BiomeRenderMode fromConfig(String value) {
			return "fast".equalsIgnoreCase(value) ? FAST : DETAILED;
		}
	}

	private static Map<WorldDimension, int[]> structurePresetIndexesByDimension() {
		Map<WorldDimension, List<Integer>> grouped = new HashMap<>();
		for (WorldDimension dimension : WorldDimension.values()) {
			grouped.put(dimension, new ArrayList<>());
		}
		for (int i = 0; i < StructurePreset.PRESETS.size(); i++) {
			grouped.get(StructurePreset.PRESETS.get(i).dimension()).add(i);
		}
		Map<WorldDimension, int[]> result = new HashMap<>();
		for (Map.Entry<WorldDimension, List<Integer>> entry : grouped.entrySet()) {
			int[] indexes = new int[entry.getValue().size()];
			for (int i = 0; i < indexes.length; i++) {
				indexes[i] = entry.getValue().get(i);
			}
			result.put(entry.getKey(), indexes);
		}
		return Map.copyOf(result);
	}

	public WorldFinderMapScreen(Screen parent) {
		this(parent, SeedMapEngines.current());
	}

	public WorldFinderMapScreen(Screen parent, SeedMapEngine engine) {
		super(Component.literal("World Finder"));
		this.parent = parent;
		this.engine = engine;
	}

	public static void clearGlobalCaches() {
		synchronized (BIOME_TILE_DATA_CACHE) {
			BIOME_TILE_DATA_CACHE.clear();
		}
		synchronized (BIOME_TILE_ID_CACHE) {
			BIOME_TILE_ID_CACHE.clear();
		}
		synchronized (STRUCTURE_SCAN_CACHE) {
			STRUCTURE_SCAN_CACHE.clear();
		}
	}

	public void reloadFromCommand() {
		closeLocalBiomeTiles();
		biomeDataCache.clearPending();
		clearGlobalCaches();
		clearSeedCaches();
		loadProfileConfig();
		clearMarkerCache();
		selectedMarker = null;
		contextMarker = null;
		completedMenuMarker = null;
		biomeLayerFailed = false;
		rebuild();
	}

	@Override
	protected void init() {
		loadProfileConfig();
		layout();
		if (!cameraInitialized && minecraft != null && minecraft.player != null) {
			cameraX = minecraft.player.getX();
			cameraZ = minecraft.player.getZ();
			cameraInitialized = true;
		}

		Long autoSeed = singleplayerSeed();
		seedInput = new EditBox(font, 10, 26, panelWidth - 20, 20, Component.literal("Seed"));
		seedInput.setValue(autoSeed == null ? profileConfig.seed() : Long.toString(autoSeed));
		seedInput.setEditable(autoSeed == null);
		seedInput.setTextColorUneditable(0xFF86EFAC);
		seedInput.setResponder(value -> {
			if (singleplayerSeed() != null) {
				return;
			}
			profileConfig.seed(value);
			profileConfig.save();
			clearMarkerCache();
		});
		addRenderableWidget(seedInput);

		int buttonWidth = (panelWidth - 28) / 2;
		int rightButtonX = 18 + buttonWidth;
		addRenderableWidget(button(10, 54, buttonWidth, dimensionShortLabel(), b -> {
			setDimension(dimension.next());
			rebuild();
		}));
		addRenderableWidget(button(rightButtonX, 54, buttonWidth, "Bio " + onOff(showBiomeColors()), b -> {
			profileConfig.showBiomeColors(!showBiomeColors());
			profileConfig.save();
			rebuild();
		}));
		addRenderableWidget(button(10, 78, buttonWidth, "Struct " + onOff(showStructuresOnMap()), b -> {
			profileConfig.showStructuresOnMap(!showStructuresOnMap());
			profileConfig.save();
			clearMarkerCache();
			rebuild();
		}));
		addRenderableWidget(button(rightButtonX, 78, buttonWidth, "Slime " + onOff(showSlimeChunks()), b -> {
			profileConfig.showSlimeChunks(!showSlimeChunks());
			profileConfig.save();
			rebuild();
		}));
		addRenderableWidget(button(10, 102, buttonWidth, "Grid " + onOff(showChunkBorders()), b -> {
			profileConfig.showChunkBorders(!showChunkBorders());
			profileConfig.save();
			rebuild();
		}));
		int halfButtonWidth = (buttonWidth - 4) / 2;
		addRenderableWidget(button(rightButtonX, 102, halfButtonWidth, "All", b -> setAllVisibleStructureFilters(true)));
		addRenderableWidget(button(rightButtonX + halfButtonWidth + 4, 102, buttonWidth - halfButtonWidth - 4, "None", b -> setAllVisibleStructureFilters(false)));

		int coordWidth = Math.max(46, (panelWidth - 62) / 2);
		xSearchInput = new EditBox(font, 10, 128, coordWidth, 20, Component.literal("X"));
		xSearchInput.setMaxLength(16);
		xSearchInput.setValue(xSearchText);
		xSearchInput.setResponder(value -> xSearchText = value);
		xSearchInput.setHint(Component.literal("X"));
		addRenderableWidget(xSearchInput);
		zSearchInput = new EditBox(font, 16 + coordWidth, 128, coordWidth, 20, Component.literal("Z"));
		zSearchInput.setMaxLength(16);
		zSearchInput.setValue(zSearchText);
		zSearchInput.setResponder(value -> zSearchText = value);
		zSearchInput.setHint(Component.literal("Z"));
		addRenderableWidget(zSearchInput);
		addRenderableWidget(button(panelWidth - 42, 128, 32, "Go", b -> goToSearchCoordinates()));

		int clearButtonWidth = 34;
		int layerButtonWidth = 42;
		int modeButtonWidth = 54;
		int clearButtonX = panelWidth - clearButtonWidth - 10;
		int layerButtonX = clearButtonX - layerButtonWidth - 4;
		int modeButtonX = layerButtonX - modeButtonWidth - 4;
		biomeSearchInput = new EditBox(font, 10, 152, Math.max(42, modeButtonX - 14), 20, Component.literal("Biome"));
		biomeSearchInput.setMaxLength(48);
		biomeSearchInput.setValue(biomeSearchText);
		biomeSearchInput.setResponder(value -> biomeSearchText = value);
		biomeSearchInput.setHint(Component.literal("Biome"));
		addRenderableWidget(biomeSearchInput);
		addRenderableWidget(button(modeButtonX, 152, modeButtonWidth, biomeRenderMode.shortLabel, b -> {
			biomeRenderMode = biomeRenderMode.next();
			profileConfig.biomeRenderMode(biomeRenderMode.configName);
			profileConfig.save();
			clearBiomeTiles();
			rebuild();
		}));
		addRenderableWidget(button(layerButtonX, 152, layerButtonWidth, biomeLayer.shortLabel, b -> {
			biomeLayer = biomeLayer.next();
			profileConfig.biomeLayer(biomeLayer.configName);
			profileConfig.save();
			clearBiomeFilter();
			rebuild();
		}));
		addRenderableWidget(button(clearButtonX, 152, clearButtonWidth, "Clr", b -> clearBiomeFilter()));

		int actionWidth = (panelWidth - 28) / 2;
		int actionRightX = 18 + actionWidth;
		int firstActionRow = height - 52;
		int secondActionRow = height - 28;
		addRenderableWidget(button(10, firstActionRow, actionWidth, "Zoom -", b -> zoom(-1)));
		addRenderableWidget(button(actionRightX, firstActionRow, actionWidth, "Zoom +", b -> zoom(1)));
		addRenderableWidget(button(10, secondActionRow, actionWidth, "Center", b -> centerOnPlayer()));
		addRenderableWidget(button(actionRightX, secondActionRow, actionWidth, "Done", b -> onClose()));
	}

	private void loadProfileConfig() {
		if (minecraft == null) {
			return;
		}
		profileConfig = WorldFinderProfileConfig.resolve(minecraft);
		dimension = WorldDimension.fromConfig(profileConfig.dimension());
		ResourceKey<Level> levelKey = currentClientDimension();
		if (!initialDimensionSynced && levelKey != null) {
			dimension = WorldDimension.fromLevelKey(levelKey);
			profileConfig.dimension(dimension.configName());
			profileConfig.save();
			initialDimensionSynced = true;
		}
		biomeLayer = BiomeLayer.fromConfig(profileConfig.biomeLayer());
		biomeRenderMode = BiomeRenderMode.fromConfig(profileConfig.biomeRenderMode());
		structureFilters = defaultStructureFilters(profileConfig.enabledStructureFilters());
		selectedBiomeFilters = loadBiomeFilters(profileConfig.selectedBiomeFilters());
		completedStructureMarkers = loadCompletedStructureMarkers();
	}

	private boolean showBiomeColors() {
		return profileConfig == null ? WorldFinderConfig.SHOW_BIOME_COLORS.get() : profileConfig.showBiomeColors();
	}

	private boolean showSlimeChunks() {
		return profileConfig == null ? WorldFinderConfig.SHOW_SLIME_CHUNKS.get() : profileConfig.showSlimeChunks();
	}

	private boolean showStructuresOnMap() {
		return profileConfig == null ? WorldFinderConfig.SHOW_STRUCTURES_ON_MAP.get() : profileConfig.showStructuresOnMap();
	}

	private boolean showChunkBorders() {
		return profileConfig == null ? WorldFinderConfig.SHOW_CHUNK_BORDERS.get() : profileConfig.showChunkBorders();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		layout();
		hoveredPlayer = isInsideMap(mouseX, mouseY) && isPlayerAt(mouseX, mouseY);
		hoveredMarker = isInsideMap(mouseX, mouseY) && !hoveredPlayer ? markerAt(mouseX, mouseY) : null;
		updatePlayerTooltipTimer(hoveredPlayer);
		updateMarkerTooltipTimer(hoveredMarker);
		hoveredStructureIndex = structureIndexAt(mouseX, mouseY);

		graphics.fill(0, 0, width, height, MAP_BACKGROUND);
		if (dimension == WorldDimension.END) {
			graphics.fill(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight, END_VOID_TILE_COLOR);
		}
		drawBiomeLayer(graphics);
		if (dimension == WorldDimension.END) {
			EndNavigationOverlayRenderer.draw(graphics, mapLeft, mapTop, mapWidth, mapHeight, worldToScreenX(0), worldToScreenZ(0), blockScale());
		}
		drawSlimeChunks(graphics);
		drawGrid(graphics);
		drawMarkers(graphics);
		drawSpawn(graphics);
		drawPlayer(graphics);
		drawPanel(graphics);
		drawHover(graphics, mouseX, mouseY);
		drawPlayerTooltip(graphics, mouseX, mouseY);
		drawMarkerTooltip(graphics, mouseX, mouseY);
		drawZoomOverlay(graphics);
		drawContextMenu(graphics, mouseX, mouseY);
		drawCompletedMenu(graphics, mouseX, mouseY);
		drawStructureTooltip(graphics, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void tick() {
		super.tick();
		syncSeedInputWithAutoSeed();
		processPendingMarkers();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (completedMenuMarker != null) {
			if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleCompletedMenuClick(event.x(), event.y())) {
				return true;
			}
			completedMenuMarker = null;
			rebuild();
			return true;
		}
		if (contextMarker != null) {
			if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleContextMenuClick(event.x(), event.y())) {
				return true;
			}
			contextMarker = null;
			rebuild();
			return true;
		}
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleBiomeSuggestionClick(event.x(), event.y())) {
			return true;
		}

		int structureIndex = structureIndexAt(event.x(), event.y());
		if (structureIndex >= 0) {
			if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				for (int i = 0; i < structureFilters.length; i++) {
					structureFilters[i] = i == structureIndex;
				}
			} else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				structureFilters[structureIndex] = !structureFilters[structureIndex];
			}
			saveStructureFilters();
			clearMarkerCache();
			rebuild();
			return true;
		}

		if (!isInsideMap(event.x(), event.y())) {
			return false;
		}
		if (isControlDown() && event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			clearBiomeFilter();
			return true;
		}
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || isControlDown() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return toggleBiomeFilterAt(event.x(), event.y());
		}
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			openContextMenu(event.x(), event.y());
			return true;
		}
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			selectedMarker = markerAt(event.x(), event.y());
			if (isCompletableMarker(selectedMarker)) {
				openCompletedMenu(selectedMarker, event.x(), event.y());
				selectedMarker = null;
				rebuild();
				return true;
			}
			dragging = selectedMarker == null;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging) {
			cameraX -= dragX / blockScale();
			cameraZ -= dragY / blockScale();
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (isInsideIconGrid(mouseX, mouseY)) {
			int maxScroll = Math.max(0, visibleStructurePresetIndexes().length - iconGridColumns * iconGridRows);
			iconScroll = Math.max(0, Math.min(maxScroll, iconScroll + (scrollY < 0 ? iconGridColumns : -iconGridColumns)));
			return true;
		}
		if (isInsideMap(mouseX, mouseY)) {
			zoom(scrollY > 0 ? 1 : -1, mouseX, mouseY);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_C && minecraft != null && minecraft.player != null) {
			centerOnPlayer();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			if (biomeSearchInput != null && biomeSearchInput.isFocused()) {
				selectFirstBiomeSuggestion();
				return true;
			}
			if ((xSearchInput != null && xSearchInput.isFocused()) || (zSearchInput != null && zSearchInput.isFocused())) {
				goToSearchCoordinates();
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		closeLocalBiomeTiles();
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	private void drawPanel(GuiGraphicsExtractor graphics) {
		graphics.fill(0, 0, panelWidth, height, PANEL_BACKGROUND);
		graphics.fill(0, 0, panelWidth, 48, 0x66101820);
		graphics.fill(panelWidth, 0, panelWidth + 1, height, PANEL_BORDER);
		graphics.fill(panelWidth - 1, 0, panelWidth, height, 0xAA93C5FD);
		graphics.text(font, "World Finder", 10, 9, 0xFFEAF2FF, true);
		graphics.text(font, singleplayerSeed() == null ? "Seed" : "Seed (solo auto)", 10, 20, 0xFF9FB1C7, true);
		drawPanelStatus(graphics);
		drawBiomeSelector(graphics);
		drawStructureFilterHeader(graphics);
		drawStructureIconGrid(graphics);
		drawPanelFooter(graphics);
	}

	private void drawPanelStatus(GuiGraphicsExtractor graphics) {
		String seedText = seedStatusText();
		String line1 = dimension.label() + " | " + biomeRenderMode.shortLabel + " | " + biomeLayer.shortLabel + " | " + engine.name();
		String line2 = seedText + " | structures " + enabledStructureFilterCount() + "/" + visibleStructurePresetIndexes().length + " | biomes " + (selectedBiomeFilters.isEmpty() ? "all" : selectedBiomeFilters.size());
		graphics.fill(8, panelStatusTop - 5, panelWidth - 8, panelStatusTop + 23, 0x44101820);
		graphics.outline(8, panelStatusTop - 5, panelWidth - 16, 28, 0x33475569);
		graphics.text(font, trimToWidth(line1, panelWidth - 20), 12, panelStatusTop, 0xFFB7C4D8, true);
		graphics.text(font, trimToWidth(line2, panelWidth - 20), 12, panelStatusTop + 12, 0xFF7C8A9D, true);
	}

	private String seedStatusText() {
		if (singleplayerSeed() != null) {
			return "Seed auto";
		}
		return seed() == null ? "Seed missing" : "Seed OK";
	}

	private void drawStructureFilterHeader(GuiGraphicsExtractor graphics) {
		int enabled = enabledStructureFilterCount();
		int total = visibleStructurePresetIndexes().length;
		int y = iconGridTop - 16;
		graphics.text(font, "Structures", 10, y, 0xFFEAF2FF, true);
		String count = enabled + "/" + total;
		int countWidth = font.width(count);
		graphics.fill(panelWidth - countWidth - 16, y - 2, panelWidth - 8, y + 10, 0x66000000);
		graphics.text(font, count, panelWidth - countWidth - 12, y, enabled == 0 ? 0xFFFF8A8A : 0xFFB7C4D8, true);
	}

	private void drawStructureIconGrid(GuiGraphicsExtractor graphics) {
		int[] indexes = visibleStructurePresetIndexes();
		int visibleCells = iconGridColumns * iconGridRows;
		if (visibleCells <= 0) {
			return;
		}
		iconScroll = Math.max(0, Math.min(iconScroll, Math.max(0, indexes.length - visibleCells)));
		int cardWidth = filterCardWidth();
		for (int cell = 0; cell < visibleCells; cell++) {
			int listIndex = iconScroll + cell;
			if (listIndex >= indexes.length) {
				break;
			}
			int presetIndex = indexes[listIndex];
			StructurePreset preset = StructurePreset.PRESETS.get(presetIndex);
			int column = cell % iconGridColumns;
			int row = cell / iconGridColumns;
			int x = iconGridLeft + column * (cardWidth + FILTER_CARD_GAP);
			int y = iconGridTop + row * (FILTER_CARD_HEIGHT + FILTER_CARD_GAP);
			boolean enabled = structureFilters[presetIndex];
			boolean only = enabled && enabledStructureFilterCount() == 1;
			boolean hovered = hoveredStructureIndex == presetIndex;
			int color = enabled ? STRUCTURE_COLORS[presetIndex % STRUCTURE_COLORS.length] : 0xFF334155;
			graphics.fill(x, y, x + cardWidth, y + FILTER_CARD_HEIGHT, hovered ? 0xAA14283A : enabled ? 0x660D2334 : 0x33101820);
			graphics.outline(x, y, cardWidth, FILTER_CARD_HEIGHT, only ? 0xFFFFFFFF : hovered ? 0xFFEAF2FF : color);
			int itemX = x + (cardWidth - 16) / 2;
			int itemY = y + (FILTER_CARD_HEIGHT - 16) / 2;
			if (hovered) {
				graphics.pose().pushMatrix();
				graphics.pose().scaleAround(1.12F, x + cardWidth / 2.0F, y + FILTER_CARD_HEIGHT / 2.0F);
				drawSpriteIcon(graphics, MapIcons.labelSprite(preset.label()), itemX, itemY, 16, 1.0F);
				graphics.pose().popMatrix();
			} else {
				drawSpriteIcon(graphics, MapIcons.labelSprite(preset.label()), itemX, itemY, 16, 1.0F);
			}
			if (!enabled) {
				graphics.fill(x + 1, y + 1, x + cardWidth - 1, y + FILTER_CARD_HEIGHT - 1, 0x77000000);
			}
		}
		if (indexes.length > visibleCells) {
			int trackTop = iconGridTop;
			int trackBottom = iconGridTop + iconGridRows * (FILTER_CARD_HEIGHT + FILTER_CARD_GAP) - FILTER_CARD_GAP;
			int trackX = panelWidth - 10;
			int thumbHeight = Math.max(12, (int)((trackBottom - trackTop) * (visibleCells / (double)indexes.length)));
			int travel = Math.max(1, trackBottom - trackTop - thumbHeight);
			int maxScroll = Math.max(1, indexes.length - visibleCells);
			int thumbY = trackTop + (int)(travel * (iconScroll / (double)maxScroll));
			graphics.fill(trackX, trackTop, trackX + 4, trackBottom, 0x66000000);
			graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF94A3B8);
		}
	}

	private void drawBiomeSelector(GuiGraphicsExtractor graphics) {
		int x = 10;
		int y = biomeSuggestionTop;
		int width = panelWidth - 20;
		List<String> suggestions = biomeSuggestions();
		for (int i = 0; i < suggestions.size(); i++) {
			String biome = suggestions.get(i);
			int rowY = y + i * BIOME_SUGGESTION_HEIGHT;
			boolean selected = selectedBiomeFilters.contains(biome);
			int color = BiomePalette.color(biome);
			graphics.fill(x, rowY, x + width, rowY + BIOME_SUGGESTION_HEIGHT - 1, selected ? 0xAA1E3A2D : 0x66101820);
			graphics.fill(x + 3, rowY + 3, x + 10, rowY + 10, color);
			graphics.outline(x + 3, rowY + 3, 7, 7, 0xCC000000);
			String label = (selected ? "ON " : "") + BiomePalette.readableName(biome);
			graphics.text(font, trimToWidth(label, width - 18), x + 14, rowY + 3, selected ? 0xFFEAF2FF : 0xFFB7C4D8, true);
		}
	}

	private void drawPanelFooter(GuiGraphicsExtractor graphics) {
		int y = iconGridTop + iconGridRows * (FILTER_CARD_HEIGHT + FILTER_CARD_GAP) + 8;
		if (y + 10 > height - 58) {
			return;
		}
		String line1 = "Left: toggle";
		String line2 = "Right: only";
		String line3 = "Ctrl+map: biome filter";
		graphics.text(font, line1, 10, y, 0xFF9FB1C7, true);
		if (y + 22 <= height - 58) {
			graphics.text(font, line2, 10, y + 12, 0xFF9FB1C7, true);
		}
		if (y + 34 <= height - 58) {
			graphics.text(font, line3, 10, y + 24, 0xFF9FB1C7, true);
		}
	}

	private void drawBiomeLayer(GuiGraphicsExtractor graphics) {
		Long seed = seed();
		if (biomeLayerFailed || !showBiomeColors() || seed == null) {
			return;
		}
		uploadPendingBiomeTextures();
		int sampleStep = biomeTileSampleStep();
		int tileBlocks = biomeTileBlocks(sampleStep);
		int minTileX = Math.floorDiv(screenToBlockX(mapLeft), tileBlocks) - 1;
		int maxTileX = Math.floorDiv(screenToBlockX(mapLeft + mapWidth), tileBlocks) + 1;
		int minTileZ = Math.floorDiv(screenToBlockZ(mapTop), tileBlocks) - 1;
		int maxTileZ = Math.floorDiv(screenToBlockZ(mapTop + mapHeight), tileBlocks) + 1;
		String biomeFilterKey = activeBiomeFilterKey();
		BiomeLayer tileLayer = activeBiomeTileLayer();
		int newRequestBudget = tileLayer == BiomeLayer.UNDERGROUND ? MAX_NEW_UNDERGROUND_TILE_REQUESTS_PER_FRAME : MAX_NEW_BIOME_TILE_REQUESTS_PER_FRAME;
		int newRequests = 0;
		for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
			for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
				BiomeTileKey key = new BiomeTileKey(seed, dimension, tileLayer, sampleStep, tileX, tileZ, biomeFilterKey);
				ColorTileTextureManager.Tile tile = biomeTileTextures.get(key);
				if (tile != null) {
					drawBiomeTile(graphics, key, tile);
					continue;
				}
				int[] colors = biomeDataCache.get(key);
				if (colors == null) {
					if (biomeDataCache.isPending(key)) {
						continue;
					}
					if (newRequests >= newRequestBudget) {
						continue;
					}
					newRequests++;
					colors = biomeDataCache.computeIfAbsent(key, this::computeBiomeTileColors);
				}
				if (colors != null) {
					if (colors.length == 0) {
						continue;
					}
					biomeTileTextures.queueUpload(key, colors);
				}
			}
		}
	}

	private void drawSlimeChunks(GuiGraphicsExtractor graphics) {
		Long seed = seed();
		if (!showSlimeChunks() || dimension != WorldDimension.OVERWORLD || seed == null || visibleChunkCount() > MAX_RENDERED_CHUNKS) {
			return;
		}
		forEachVisibleChunk((chunkX, chunkZ) -> {
			if (engine.isSlimeChunk(seed, chunkX, chunkZ)) {
				fillChunk(graphics, chunkX, chunkZ, MAP_SLIME_CHUNK);
			}
		});
	}

	private int[] computeBiomeTileColors(BiomeTileKey key) {
		int tileBlocks = biomeTileBlocks(key.sampleStep());
		int baseBlockX = key.tileX() * tileBlocks;
		int baseBlockZ = key.tileZ() * tileBlocks;
		if (key.dimension() == WorldDimension.END) {
			if (isCentralEndVoidTile(baseBlockX, baseBlockZ, tileBlocks)) {
				return EMPTY_BIOME_TILE_COLORS;
			}
			int[] colors = engine.endTerrainColorsAt(key.seed(), baseBlockX, baseBlockZ, key.sampleStep(), BIOME_TILE_CELLS, BIOME_TILE_CELLS);
			return isVoidEndTile(colors) ? EMPTY_BIOME_TILE_COLORS : colors;
		}
		if (isFastBiomeMode(key.layer(), key.biomeFilterKey())) {
			return engine.biomeColorsAt(key.seed(), key.dimension(), baseBlockX, biomeSampleY(BiomeLayer.SURFACE, key.dimension()), baseBlockZ, key.sampleStep(), BIOME_TILE_CELLS, BIOME_TILE_CELLS);
		}
		if (!key.biomeFilterKey().isEmpty()) {
			return filteredBiomeColors(key);
		}
		if (key.layer() == BiomeLayer.UNDERGROUND && key.dimension() == WorldDimension.OVERWORLD) {
			return undergroundBiomeColors(key);
		}
		return engine.biomeColorsAt(key.seed(), key.dimension(), baseBlockX, biomeSampleY(key.layer(), key.dimension()), baseBlockZ, key.sampleStep(), BIOME_TILE_CELLS, BIOME_TILE_CELLS);
	}

	private int[] filteredBiomeColors(BiomeTileKey key) {
		Set<String> selected = biomeFilterSet(key.biomeFilterKey());
		String[] biomes = biomeIdsForTile(key);
		if (biomes == null) {
			return null;
		}
		int[] colors = new int[BIOME_TILE_CELLS * BIOME_TILE_CELLS];
		for (int localZ = 0; localZ < BIOME_TILE_CELLS; localZ++) {
			for (int localX = 0; localX < BIOME_TILE_CELLS; localX++) {
				int index = localZ * BIOME_TILE_CELLS + localX;
				String biome = normalizeBiomeId(biomes[index]);
				int color = biome.isEmpty() ? 0xFF475569 : BiomePalette.color(biome);
				colors[index] = !biome.isEmpty() && selected.contains(biome) ? color : fadedBiomeColor(color);
			}
		}
		return colors;
	}

	private int[] undergroundBiomeColors(BiomeTileKey key) {
		String[] biomes = biomeIdsForTile(key);
		if (biomes == null) {
			return null;
		}
		int[] colors = new int[BIOME_TILE_CELLS * BIOME_TILE_CELLS];
		for (int index = 0; index < colors.length; index++) {
			String biome = normalizeBiomeId(biomes[index]);
			colors[index] = biome.isEmpty() ? 0xFF475569 : BiomePalette.color(biome);
		}
		return colors;
	}

	private String[] biomeIdsForTile(BiomeTileKey key) {
		BiomeIdTileKey idKey = new BiomeIdTileKey(key.seed(), key.dimension(), key.layer(), key.sampleStep(), key.tileX(), key.tileZ());
		synchronized (BIOME_TILE_ID_CACHE) {
			String[] cached = BIOME_TILE_ID_CACHE.get(idKey);
			if (cached != null) {
				return cached;
			}
		}
		String[] biomes = computeBiomeTileIds(idKey);
		if (biomes != null) {
			synchronized (BIOME_TILE_ID_CACHE) {
				BIOME_TILE_ID_CACHE.put(idKey, biomes);
				trimBiomeTileIdCache();
			}
		}
		return biomes;
	}

	private String[] computeBiomeTileIds(BiomeIdTileKey key) {
		int tileBlocks = biomeTileBlocks(key.sampleStep());
		int baseBlockX = key.tileX() * tileBlocks;
		int baseBlockZ = key.tileZ() * tileBlocks;
		if (key.layer() == BiomeLayer.UNDERGROUND && key.dimension() == WorldDimension.OVERWORLD) {
			return undergroundBiomeIds(key, baseBlockX, baseBlockZ);
		}
		return engine.biomeIdsAt(key.seed(), key.dimension(), baseBlockX, biomeSampleY(key.layer(), key.dimension()), baseBlockZ, key.sampleStep(), BIOME_TILE_CELLS, BIOME_TILE_CELLS);
	}

	private String[] undergroundBiomeIds(BiomeIdTileKey key, int baseBlockX, int baseBlockZ) {
		String[] selected = new String[BIOME_TILE_CELLS * BIOME_TILE_CELLS];
		String[] deepest = null;
		for (int sampleY : UNDERGROUND_TILE_SAMPLE_YS) {
			String[] biomes = engine.biomeIdsAt(key.seed(), key.dimension(), baseBlockX, sampleY, baseBlockZ, key.sampleStep(), BIOME_TILE_CELLS, BIOME_TILE_CELLS);
			deepest = biomes;
			for (int index = 0; index < biomes.length; index++) {
				if (selected[index] == null && isUndergroundBiome(biomes[index])) {
					selected[index] = normalizeBiomeId(biomes[index]);
				}
			}
		}
		for (int index = 0; index < selected.length; index++) {
			if (selected[index] == null) {
				selected[index] = deepest == null ? "" : normalizeBiomeId(deepest[index]);
			}
		}
		return selected;
	}

	private DynamicTexture createBiomeTileTexture(BiomeTileKey key, int[] colors) {
		if (colors.length != BIOME_TILE_CELLS * BIOME_TILE_CELLS) {
			throw new IllegalArgumentException("Unexpected biome tile size " + colors.length);
		}
		DynamicTexture texture = new DynamicTexture("World Finder biome tile " + key.tileX() + "," + key.tileZ(), BIOME_TILE_CELLS, BIOME_TILE_CELLS, true);
		for (int z = 0; z < BIOME_TILE_CELLS; z++) {
			for (int x = 0; x < BIOME_TILE_CELLS; x++) {
				texture.getPixels().setPixel(x, z, colors[z * BIOME_TILE_CELLS + x]);
			}
		}
		texture.upload();
		return texture;
	}

	private void uploadPendingBiomeTextures() {
		biomeTileTextures.uploadPending(this::createBiomeTileTexture);
	}

	private static boolean isVoidEndTile(int[] colors) {
		if (colors == null || colors.length == 0) {
			return false;
		}
		for (int color : colors) {
			if (color != END_VOID_TILE_COLOR) {
				return false;
			}
		}
		return true;
	}

	private static boolean isCentralEndVoidTile(int baseBlockX, int baseBlockZ, int tileBlocks) {
		int minX = baseBlockX;
		int maxX = baseBlockX + tileBlocks;
		int minZ = baseBlockZ;
		int maxZ = baseBlockZ + tileBlocks;
		long minRadiusSquared = distanceSquaredToRectangle(0, 0, minX, minZ, maxX, maxZ);
		long maxRadiusSquared = maxCornerDistanceSquared(minX, minZ, maxX, maxZ);
		long mainIslandRadiusSquared = (long)END_MAIN_ISLAND_SAFE_RADIUS * END_MAIN_ISLAND_SAFE_RADIUS;
		long outerIslandRadiusSquared = (long)END_OUTER_ISLAND_START_RADIUS * END_OUTER_ISLAND_START_RADIUS;
		return minRadiusSquared > mainIslandRadiusSquared && maxRadiusSquared < outerIslandRadiusSquared;
	}

	private static long distanceSquaredToRectangle(int x, int z, int minX, int minZ, int maxX, int maxZ) {
		long dx = x < minX ? minX - x : x > maxX ? x - maxX : 0L;
		long dz = z < minZ ? minZ - z : z > maxZ ? z - maxZ : 0L;
		return dx * dx + dz * dz;
	}

	private static long maxCornerDistanceSquared(int minX, int minZ, int maxX, int maxZ) {
		long d1 = (long)minX * minX + (long)minZ * minZ;
		long d2 = (long)minX * minX + (long)maxZ * maxZ;
		long d3 = (long)maxX * maxX + (long)minZ * minZ;
		long d4 = (long)maxX * maxX + (long)maxZ * maxZ;
		return Math.max(Math.max(d1, d2), Math.max(d3, d4));
	}

	private void drawBiomeTile(GuiGraphicsExtractor graphics, BiomeTileKey key, ColorTileTextureManager.Tile tile) {
		if (tile == null) {
			return;
		}
		int minX = tileScreenX(key, 0.0D);
		int minY = tileScreenY(key, 0.0D);
		int maxX = tileScreenX(key, 1.0D);
		int maxY = tileScreenY(key, 1.0D);
		drawTextureTile(graphics, tile, minX, minY, maxX, maxY);
	}

	private void drawTextureTile(GuiGraphicsExtractor graphics, ColorTileTextureManager.Tile tile, int x1, int y1, int x2, int y2) {
		int minX = Math.min(x1, x2);
		int maxX = Math.max(x1, x2);
		int minY = Math.min(y1, y2);
		int maxY = Math.max(y1, y2);
		if (maxX <= mapLeft || minX >= mapLeft + mapWidth || maxY <= mapTop || minY >= mapTop + mapHeight) {
			return;
		}
		float u0 = 0.0F;
		float u1 = 1.0F;
		float v0 = 0.0F;
		float v1 = 1.0F;
		int clippedMinX = minX;
		int clippedMaxX = maxX;
		int clippedMinY = minY;
		int clippedMaxY = maxY;
		if (clippedMinX < mapLeft) {
			u0 = (mapLeft - clippedMinX) / (float)Math.max(1, clippedMaxX - clippedMinX);
			clippedMinX = mapLeft;
		}
		if (clippedMaxX > mapLeft + mapWidth) {
			u1 = 1.0F - (clippedMaxX - mapLeft - mapWidth) / (float)Math.max(1, clippedMaxX - clippedMinX);
			clippedMaxX = mapLeft + mapWidth;
		}
		if (clippedMinY < mapTop) {
			v0 = (mapTop - clippedMinY) / (float)Math.max(1, clippedMaxY - clippedMinY);
			clippedMinY = mapTop;
		}
		if (clippedMaxY > mapTop + mapHeight) {
			v1 = 1.0F - (clippedMaxY - mapTop - mapHeight) / (float)Math.max(1, clippedMaxY - clippedMinY);
			clippedMaxY = mapTop + mapHeight;
		}
		graphics.blit(tile.texture().getTextureView(), tile.texture().getSampler(), clippedMinX, clippedMinY, clippedMaxX, clippedMaxY, u0, u1, v0, v1);
	}

	private void drawBiomeTilePreview(GuiGraphicsExtractor graphics, BiomeTileKey key) {
		Integer color = biomeTilePreviewCache.get(key);
		if (color == null) {
			return;
		}
		int minX = tileScreenX(key, 0.0D);
		int minY = tileScreenY(key, 0.0D);
		int maxX = tileScreenX(key, 1.0D);
		int maxY = tileScreenY(key, 1.0D);
		int left = Math.max(mapLeft, Math.min(minX, maxX));
		int right = Math.min(mapLeft + mapWidth, Math.max(minX, maxX));
		int top = Math.max(mapTop, Math.min(minY, maxY));
		int bottom = Math.min(mapTop + mapHeight, Math.max(minY, maxY));
		if (right > left && bottom > top) {
			graphics.fill(left, top, right, bottom, color);
		}
	}

	private int tileScreenX(BiomeTileKey key, double tileOffset) {
		double tileQuartX = biomeTileQuartX(key, tileOffset);
		double centerScreenX = mapLeft + mapWidth / 2.0D;
		double centerQuartX = cameraX / 4.0D;
		return (int)Math.floor(centerScreenX + (tileQuartX - centerQuartX) * quartScale());
	}

	private int tileScreenY(BiomeTileKey key, double tileOffset) {
		double tileQuartZ = biomeTileQuartZ(key, tileOffset);
		double centerScreenY = mapTop + mapHeight / 2.0D;
		double centerQuartZ = cameraZ / 4.0D;
		return (int)Math.floor(centerScreenY + (tileQuartZ - centerQuartZ) * quartScale());
	}

	private static double biomeTileQuartX(BiomeTileKey key, double tileOffset) {
		double tileBlocks = biomeTileBlocks(key.sampleStep());
		return (key.tileX() * tileBlocks + tileOffset * tileBlocks) / 4.0D;
	}

	private static double biomeTileQuartZ(BiomeTileKey key, double tileOffset) {
		double tileBlocks = biomeTileBlocks(key.sampleStep());
		return (key.tileZ() * tileBlocks + tileOffset * tileBlocks) / 4.0D;
	}

	private void computeBiomeTilePreview(BiomeTileKey key) {
		int tileBlocks = biomeTileBlocks(key.sampleStep());
		int baseBlockX = key.tileX() * tileBlocks;
		int baseBlockZ = key.tileZ() * tileBlocks;
		int sampleBlockX = baseBlockX + tileBlocks / 2;
		int sampleBlockZ = baseBlockZ + tileBlocks / 2;
		int color = key.dimension() == WorldDimension.END
				? engine.endTerrainColorsAt(key.seed(), sampleBlockX, sampleBlockZ, 1, 1, 1)[0]
				: biomeColor(key.seed(), key.dimension(), sampleBlockX, sampleBlockZ);
		if (!key.biomeFilterKey().isEmpty() && key.dimension() != WorldDimension.END) {
			Optional<String> biome = engine.biomeIdAt(key.seed(), key.dimension(), sampleBlockX, biomeSampleY(key.layer(), key.dimension()), sampleBlockZ).map(WorldFinderMapScreen::normalizeBiomeId);
			if (biome.isEmpty() || !biomeFilterSet(key.biomeFilterKey()).contains(biome.get())) {
				color = fadedBiomeColor(color);
			}
		}
		biomeTilePreviewCache.put(key, color);
		trimBiomeTilePreviewCache(key);
	}

	private void trimBiomeTilePreviewCache(BiomeTileKey key) {
		synchronized (biomeTilePreviewCache) {
			while (biomeTilePreviewCache.size() > MAX_BIOME_TILE_CACHE_PER_DIMENSION * WorldDimension.values().length) {
				BiomeTileKey eldest = biomeTilePreviewCache.keySet().iterator().next();
				biomeTilePreviewCache.remove(eldest);
			}
		}
	}

	private static void trimBiomeTileDataCache() {
		synchronized (BIOME_TILE_DATA_CACHE) {
			while (BIOME_TILE_DATA_CACHE.size() > MAX_BIOME_TILE_DATA_CACHE) {
				BiomeTileKey eldest = BIOME_TILE_DATA_CACHE.keySet().iterator().next();
				BIOME_TILE_DATA_CACHE.remove(eldest);
			}
		}
	}

	private static void trimBiomeTileIdCache() {
		synchronized (BIOME_TILE_ID_CACHE) {
			while (BIOME_TILE_ID_CACHE.size() > MAX_BIOME_TILE_ID_CACHE) {
				BiomeIdTileKey eldest = BIOME_TILE_ID_CACHE.keySet().iterator().next();
				BIOME_TILE_ID_CACHE.remove(eldest);
			}
		}
	}

	private void closeLocalBiomeTiles() {
		biomeTileTextures.closeAll();
		biomeTilePreviewCache.clear();
	}

	private void closeBiomeTile(BiomeTileKey key) {
		biomeTileTextures.close(key);
	}

	private void clearBiomeTiles() {
		biomeDataCache.clearPending();
		Long seed = seed();
		if (seed != null) {
			clearBiomeTiles(seed, dimension, biomeLayer);
		}
		biomeLayerFailed = false;
	}

	private void clearBiomeTiles(long seed, WorldDimension dimension, BiomeLayer layer) {
		synchronized (BIOME_TILE_DATA_CACHE) {
			BIOME_TILE_DATA_CACHE.keySet().removeIf(key -> key.seed() == seed && key.dimension() == dimension && key.layer() == layer);
		}
		synchronized (biomeTilePreviewCache) {
			biomeTilePreviewCache.keySet().removeIf(key -> key.seed() == seed && key.dimension() == dimension && key.layer() == layer);
		}
		biomeTileTextures.removeIf(key -> key.seed() == seed && key.dimension() == dimension && key.layer() == layer);
	}

	private void drawGrid(GuiGraphicsExtractor graphics) {
		if (!showChunkBorders()) {
			return;
		}
		int minChunkX = screenToChunkX(mapLeft);
		int maxChunkX = screenToChunkX(mapLeft + mapWidth) + 1;
		int minChunkZ = screenToChunkZ(mapTop);
		int maxChunkZ = screenToChunkZ(mapTop + mapHeight) + 1;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			int x = worldToScreenX(chunkX * 16);
			graphics.fill(x, mapTop, x + 1, mapTop + mapHeight, chunkX == 0 ? MAP_AXIS : MAP_GRID);
		}
		for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
			int y = worldToScreenZ(chunkZ * 16);
			graphics.fill(mapLeft, y, mapLeft + mapWidth, y + 1, chunkZ == 0 ? MAP_AXIS : MAP_GRID);
		}
	}

	private void drawMarkers(GuiGraphicsExtractor graphics) {
		for (FinderResult marker : markers()) {
			int x = worldToScreenX(marker.blockX());
			int y = worldToScreenZ(marker.blockZ());
			if (!isInsideMap(x, y)) {
				continue;
			}
			drawMarkerIcon(graphics, marker, x, y, marker.equals(hoveredMarker));
		}
		if (selectedMarker != null) {
			int x = worldToScreenX(selectedMarker.blockX());
			int y = worldToScreenZ(selectedMarker.blockZ());
			if (isInsideMap(x, y)) {
				graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xCCFFFFFF);
			}
		}
	}

	private void drawPlayer(GuiGraphicsExtractor graphics) {
		if (minecraft == null || minecraft.player == null) {
			return;
		}
		int x = worldToScreenX(minecraft.player.getX());
		int y = worldToScreenZ(minecraft.player.getZ());
		if (!isInsideMap(x, y)) {
			return;
		}
		if (!hoveredPlayer) {
			graphics.item(playerHeadStack(), x - 8, y - 8);
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().scaleAround(1.15F, x, y);
		graphics.item(playerHeadStack(), x - 8, y - 8);
		graphics.pose().popMatrix();
	}

	private ItemStack playerHeadStack() {
		if (minecraft == null || minecraft.player == null) {
			return new ItemStack(Items.PLAYER_HEAD);
		}
		String playerName = minecraft.player.getScoreboardName();
		if (cachedPlayerHeadStack == null || !playerName.equals(cachedPlayerHeadName)) {
			cachedPlayerHeadStack = new ItemStack(Items.PLAYER_HEAD);
			cachedPlayerHeadStack.set(DataComponents.PROFILE, minecraft.player.getProfile());
			cachedPlayerHeadName = playerName;
		}
		return cachedPlayerHeadStack;
	}

	private void drawPlayerTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!hoveredPlayer || minecraft == null || minecraft.player == null || System.currentTimeMillis() - hoveredPlayerStartedAt < 1000L) {
			return;
		}
		String label = minecraft.player.getScoreboardName();
		int boxWidth = Math.min(mapWidth - 16, Math.max(72, font.width(label) + 14));
		int boxHeight = 20;
		int x = Math.max(mapLeft + 4, Math.min(mouseX + 12, mapLeft + mapWidth - boxWidth - 4));
		int y = Math.max(mapTop + 4, Math.min(mouseY + 12, mapTop + mapHeight - boxHeight - 4));
		graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xDD05080D);
		graphics.text(font, trimToWidth(label, boxWidth - 14), x + 7, y + 6, 0xFFEAF2FF, true);
	}

	private void drawSpawn(GuiGraphicsExtractor graphics) {
		spawnMarker().ifPresent(marker -> {
			int x = worldToScreenX(marker.blockX());
			int y = worldToScreenZ(marker.blockZ());
			if (isInsideMap(x, y)) {
				drawMarkerIcon(graphics, marker, x, y, marker.equals(hoveredMarker));
			}
		});
	}

	private void drawHover(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!isInsideMap(mouseX, mouseY)) {
			return;
		}
		FinderResult marker = hoveredMarker;
		int blockX = screenToBlockX(mouseX);
		int blockZ = screenToBlockZ(mouseY);
		int chunkX = Math.floorDiv(blockX, 16);
		int chunkZ = Math.floorDiv(blockZ, 16);
		String line = marker == null
				? "X " + blockX + "  Z " + blockZ + "  C " + chunkX + ", " + chunkZ
				: marker.label() + " " + withKnownSurfaceY(marker).coordinates();
		String detail = biomeStatus(chunkX, chunkZ);
		int boxWidth = Math.min(mapWidth - 16, Math.max(132, Math.max(font.width(line), font.width(detail)) + 12));
		int boxLeft = mapLeft + 8;
		int boxTop = mapTop + mapHeight - 40;
		graphics.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + 32, 0x99000000);
		graphics.outline(boxLeft, boxTop, boxWidth, 32, marker == null ? 0x443B82F6 : markerColor(marker));
		graphics.text(font, trim(line, 72), boxLeft + 6, boxTop + 6, 0xFFEAF2FF, true);
		graphics.text(font, trim(detail, 72), boxLeft + 6, boxTop + 18, 0xFF9FB1C7, true);
	}

	private void drawMarkerTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
	}

	private void drawCompletedMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (completedMenuMarker == null) {
			return;
		}
		int width = Math.max(96, font.width("Completed") + 34);
		int height = 20;
		int x = completedMenuX(width);
		int y = completedMenuY(height);
		boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
		drawMinecraftButtonPanel(graphics, x, y, width, height, hovered);
		drawButtonPointer(graphics, x, y + height - 1, width);
		int boxX = x + 8;
		int boxY = y + 5;
		graphics.fill(boxX, boxY, boxX + 10, boxY + 10, 0xFF1C1C1C);
		graphics.fill(boxX + 1, boxY + 1, boxX + 9, boxY + 9, 0xFFE0E0E0);
		if (isCompleted(completedMenuMarker)) {
			graphics.text(font, "x", boxX + 2, boxY + 1, 0xFF202020, false);
		}
		graphics.text(font, "Completed", x + 23, y + 6, hovered ? 0xFFFFFFA0 : 0xFFE0E0E0, true);
	}

	private void drawMinecraftButtonPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean hovered) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? VANILLA_BUTTON_HIGHLIGHTED : VANILLA_BUTTON, x, y, width, height);
	}

	private void drawButtonPointer(GuiGraphicsExtractor graphics, int x, int y, int width) {
		int center = Math.max(x + 8, Math.min(completedMenuX, x + width - 8));
		int left = center - 6;
		graphics.fill(left, y, left + 12, y + 1, 0xFF000000);
		graphics.fill(left + 1, y + 1, left + 11, y + 2, 0xFF3B3B3B);
		graphics.fill(left + 2, y + 2, left + 10, y + 3, 0xFF555555);
		graphics.fill(left + 3, y + 3, left + 9, y + 4, 0xFF696969);
		graphics.fill(left + 4, y + 4, left + 8, y + 5, 0xFF4A4A4A);
		graphics.fill(left + 5, y + 5, left + 7, y + 6, 0xFF000000);
	}

	private void drawZoomOverlay(GuiGraphicsExtractor graphics) {
		long now = System.currentTimeMillis();
		if (now >= zoomOverlayUntil) {
			return;
		}
		float fade = 1.0F;
		long fadeStartedAt = zoomOverlayStartedAt + 1000L;
		if (now > fadeStartedAt) {
			fade = Math.max(0.0F, 1.0F - (now - fadeStartedAt) / 350.0F);
		}
		int alpha = Math.max(0, Math.min(255, (int)(fade * 190.0F)));
		String text = "Zoom " + zoomLabel();
		int boxWidth = font.width(text) + 18;
		int boxLeft = mapLeft + (mapWidth - boxWidth) / 2;
		int boxTop = mapTop + 10;
		graphics.fill(boxLeft, boxTop, boxLeft + boxWidth, boxTop + 24, withAlpha(0x000000, alpha));
		graphics.outline(boxLeft, boxTop, boxWidth, 24, withAlpha(0x93C5FD, Math.min(255, alpha + 40)));
		graphics.text(font, text, boxLeft + 9, boxTop + 8, withAlpha(0xEAF2FF, Math.min(255, alpha + 60)), true);
	}

	private void drawStructureTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (hoveredStructureIndex < 0) {
			return;
		}
		StructurePreset preset = StructurePreset.PRESETS.get(hoveredStructureIndex);
		String status = structureFilters[hoveredStructureIndex] ? "ON" : "OFF";
		int count = countVisibleStructureMarkers(hoveredStructureIndex);
		String line = preset.label();
		String detail = status + (count > 0 ? " | visible " + count : "");
		String action = "Left toggle | Right only";
		int boxWidth = Math.max(Math.max(font.width(line), font.width(detail)), font.width(action)) + 14;
		int x = Math.min(mouseX + 10, width - boxWidth - 4);
		int y = Math.min(mouseY + 10, height - 48);
		graphics.fill(x, y, x + boxWidth, y + 46, 0xEE05080D);
		graphics.outline(x, y, boxWidth, 46, STRUCTURE_COLORS[hoveredStructureIndex % STRUCTURE_COLORS.length]);
		graphics.text(font, line, x + 7, y + 6, 0xFFEAF2FF, true);
		graphics.text(font, detail, x + 7, y + 18, structureFilters[hoveredStructureIndex] ? 0xFFB7C4D8 : 0xFFFF9CA3, true);
		graphics.text(font, action, x + 7, y + 30, 0xFF7C8A9D, true);
	}

	private void drawContextMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (contextMarker == null) {
			return;
		}
		int menuWidth = CONTEXT_MENU_WIDTH;
		int menuHeight = contextMenuHeight();
		int x = contextMenuX();
		int y = contextMenuY();
		contextX = x;
		contextY = y;
		graphics.fill(x, y, x + menuWidth, y + menuHeight, 0xC805080D);
		graphics.outline(x, y, menuWidth, menuHeight, 0xAA000000);
		drawMinecraftButtonPanel(graphics, x + CONTEXT_MENU_PADDING, y + CONTEXT_MENU_PADDING, menuWidth - CONTEXT_MENU_PADDING * 2, CONTEXT_MENU_BUTTON_HEIGHT, false);
		graphics.text(font, trim(contextMarker.label(), 18), x + 10, y + 10, markerColor(contextMarker), true);
		List<ContextAction> options = contextMenuActions();
		for (int i = 0; i < options.size(); i++) {
			int optionY = y + CONTEXT_MENU_HEADER_HEIGHT + i * CONTEXT_MENU_OPTION_HEIGHT;
			boolean hovered = mouseX >= x + CONTEXT_MENU_PADDING && mouseX <= x + menuWidth - CONTEXT_MENU_PADDING
					&& mouseY >= optionY && mouseY < optionY + CONTEXT_MENU_BUTTON_HEIGHT;
			drawMinecraftButtonPanel(graphics, x + CONTEXT_MENU_PADDING, optionY, menuWidth - CONTEXT_MENU_PADDING * 2, CONTEXT_MENU_BUTTON_HEIGHT, hovered);
			ContextAction action = options.get(i);
			graphics.text(font, contextActionLabel(action), x + 10, optionY + 6, hovered ? 0xFFFFFFA0 : 0xFFE0E0E0, true);
			if (action == ContextAction.WAYPOINT) {
				graphics.text(font, ">", x + menuWidth - 15, optionY + 6, hovered ? 0xFFFFFFA0 : 0xFFE0E0E0, true);
			}
		}
		if (isWaypointSubmenuOpen(mouseX, mouseY)) {
			drawWaypointSubmenu(graphics, mouseX, mouseY);
		}
	}

	private void drawWaypointSubmenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<WaypointIntegrations.Target> targets = availableWaypointTargets();
		if (targets.isEmpty()) {
			return;
		}
		int rows = targets.size();
		int submenuHeight = CONTEXT_MENU_PADDING * 2 + rows * CONTEXT_MENU_OPTION_HEIGHT - 2;
		int x = waypointSubmenuX();
		int y = waypointSubmenuY(rows);
		graphics.fill(x, y, x + WAYPOINT_SUBMENU_WIDTH, y + submenuHeight, 0xC805080D);
		graphics.outline(x, y, WAYPOINT_SUBMENU_WIDTH, submenuHeight, 0xAA000000);
		for (int i = 0; i < targets.size(); i++) {
			int optionY = y + CONTEXT_MENU_PADDING + i * CONTEXT_MENU_OPTION_HEIGHT;
			boolean hovered = mouseX >= x + CONTEXT_MENU_PADDING && mouseX <= x + WAYPOINT_SUBMENU_WIDTH - CONTEXT_MENU_PADDING
					&& mouseY >= optionY && mouseY < optionY + CONTEXT_MENU_BUTTON_HEIGHT;
			drawMinecraftButtonPanel(graphics, x + CONTEXT_MENU_PADDING, optionY, WAYPOINT_SUBMENU_WIDTH - CONTEXT_MENU_PADDING * 2, CONTEXT_MENU_BUTTON_HEIGHT, hovered);
			graphics.text(font, targets.get(i).label(), x + 10, optionY + 6, hovered ? 0xFFFFFFA0 : 0xFFE0E0E0, true);
		}
	}

	private List<FinderResult> markers() {
		Long seed = seed();
		if (seed == null) {
			return List.of();
		}
		int minChunkX = screenToChunkX(mapLeft);
		int maxChunkX = screenToChunkX(mapLeft + mapWidth) + 1;
		int minChunkZ = screenToChunkZ(mapTop);
		int maxChunkZ = screenToChunkZ(mapTop + mapHeight) + 1;
		ViewKey key = new ViewKey(seed, dimension, markerCacheMinChunk(minChunkX), markerCacheMaxChunk(maxChunkX), markerCacheMinChunk(minChunkZ), markerCacheMaxChunk(maxChunkZ), structureFilterMask(), showStructuresOnMap());
		if (key.equals(cachedMarkersKey)) {
			return cachedMarkers;
		}
		if (!key.equals(pendingMarkersKey)) {
			pendingMarkersKey = key;
			markerRebuildAfter = System.currentTimeMillis() + MARKER_REBUILD_DELAY_MILLIS;
		}
		return canReuseVisibleMarkers(key) ? cachedMarkers : List.of();
	}

	private boolean canReuseVisibleMarkers(ViewKey key) {
		return cachedMarkersKey != null
				&& cachedMarkersKey.seed() == key.seed()
				&& cachedMarkersKey.dimension() == key.dimension()
				&& cachedMarkersKey.structures() == key.structures();
	}

	private void processPendingMarkers() {
		ViewKey key = pendingMarkersKey;
		if (key == null || System.currentTimeMillis() < markerRebuildAfter) {
			return;
		}
		Long currentSeed = seed();
		if (currentSeed == null || currentSeed != key.seed()) {
			pendingMarkersKey = null;
			return;
		}
		BlockPos strongholdOrigin = minecraft != null && minecraft.player != null ? minecraft.player.blockPosition() : BlockPos.ZERO;
		if (updateMarkersFromStructureCache(key, strongholdOrigin)) {
			pendingMarkersKey = null;
		}
	}

	private boolean updateMarkersFromStructureCache(ViewKey key, BlockPos strongholdOrigin) {
		List<FinderResult> markers = new ArrayList<>();
		Map<Integer, Integer> counts = new HashMap<>();
		boolean ready = true;
		if (key.structures()) {
			for (int index : visibleStructurePresetIndexes()) {
				if (!structureFilters[index]) {
					continue;
				}
				StructureScanKey scanKey = structureScanKey(key, index, strongholdOrigin);
				List<FinderResult> presetMarkers = structureScanCache.computeIfAbsent(scanKey, this::computeStructureScan);
				if (presetMarkers == null) {
					ready = false;
					continue;
				}
				counts.put(index, presetMarkers.size());
				markers.addAll(presetMarkers);
			}
		}
		cachedMarkers = List.copyOf(markers);
		cachedStructureCounts = Map.copyOf(counts);
		if (ready) {
			cachedMarkersKey = key;
		}
		return ready;
	}

	private StructureScanKey structureScanKey(ViewKey key, int presetIndex, BlockPos strongholdOrigin) {
		int originChunkX = 0;
		int originChunkZ = 0;
		if (isStrongholdPreset(presetIndex) && strongholdOrigin != null) {
			originChunkX = Math.floorDiv(strongholdOrigin.getX(), 16);
			originChunkZ = Math.floorDiv(strongholdOrigin.getZ(), 16);
		}
		return new StructureScanKey(key.seed(), key.dimension(), presetIndex, key.minChunkX(), key.maxChunkX(), key.minChunkZ(), key.maxChunkZ(), originChunkX, originChunkZ);
	}

	private List<FinderResult> computeStructureScan(StructureScanKey key) {
		if (isStrongholdPreset(key.presetIndex())) {
			BlockPos origin = new BlockPos(key.originChunkX() * 16, 0, key.originChunkZ() * 16);
			return strongholdsInView(key.seed(), key.minChunkX(), key.maxChunkX(), key.minChunkZ(), key.maxChunkZ(), origin);
		}
		StructurePreset preset = StructurePreset.PRESETS.get(key.presetIndex());
		if ("Copper Ore Vein".equals(preset.label())) {
			return engine.oreVeinsInView(key.seed(), key.dimension(), true, key.minChunkX(), key.maxChunkX(), key.minChunkZ(), key.maxChunkZ());
		}
		if ("Iron Ore Vein".equals(preset.label())) {
			return engine.oreVeinsInView(key.seed(), key.dimension(), false, key.minChunkX(), key.maxChunkX(), key.minChunkZ(), key.maxChunkZ());
		}
		return engine.structuresInView(key.seed(), key.dimension(), preset, key.minChunkX(), key.maxChunkX(), key.minChunkZ(), key.maxChunkZ());
	}

	private List<FinderResult> strongholdsInView(long seed, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, BlockPos origin) {
		if (origin == null) {
			return List.of();
		}
		List<FinderResult> visible = new ArrayList<>();
		try {
			visible.addAll(engine.strongholds(seed, origin));
		} catch (RuntimeException ignored) {
			// Some client registry states cannot build vanilla structure rings safely during screen rendering.
		}
		return visible.stream()
				.filter(result -> {
					int chunkX = Math.floorDiv(result.blockX(), 16);
					int chunkZ = Math.floorDiv(result.blockZ(), 16);
					return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
				})
				.toList();
	}

	private boolean isStrongholdPreset(int index) {
		return "Stronghold".equals(StructurePreset.PRESETS.get(index).label());
	}

	private int markerCacheMinChunk(int visibleMinChunk) {
		return Math.floorDiv(visibleMinChunk, MARKER_CACHE_CHUNK_STEP) * MARKER_CACHE_CHUNK_STEP - MARKER_CACHE_CHUNK_PADDING;
	}

	private int markerCacheMaxChunk(int visibleMaxChunk) {
		return Math.floorDiv(visibleMaxChunk, MARKER_CACHE_CHUNK_STEP) * MARKER_CACHE_CHUNK_STEP + MARKER_CACHE_CHUNK_STEP - 1 + MARKER_CACHE_CHUNK_PADDING;
	}

	private FinderResult markerAt(double mouseX, double mouseY) {
		Optional<FinderResult> spawn = spawnMarker();
		if (spawn.isPresent()) {
			FinderResult marker = spawn.get();
			int x = worldToScreenX(marker.blockX());
			int y = worldToScreenZ(marker.blockZ());
			if (Math.abs(mouseX - x) <= 8.0D && Math.abs(mouseY - y) <= 8.0D) {
				return marker;
			}
		}
		for (FinderResult marker : markers()) {
			int x = worldToScreenX(marker.blockX());
			int y = worldToScreenZ(marker.blockZ());
			if (Math.abs(mouseX - x) <= 8.0D && Math.abs(mouseY - y) <= 8.0D) {
				return marker;
			}
		}
		return null;
	}

	private boolean isPlayerAt(double mouseX, double mouseY) {
		if (minecraft == null || minecraft.player == null) {
			return false;
		}
		int x = worldToScreenX(minecraft.player.getX());
		int y = worldToScreenZ(minecraft.player.getZ());
		return Math.abs(mouseX - x) <= 8.0D && Math.abs(mouseY - y) <= 8.0D;
	}

	private Optional<FinderResult> spawnMarker() {
		if (dimension != WorldDimension.OVERWORLD || minecraft == null || minecraft.level == null) {
			return Optional.empty();
		}
		BlockPos spawn = clientSpawnPos();
		if (spawn == null) {
			return Optional.empty();
		}
		return Optional.of(new FinderResult("Spawn Point", spawn.getX(), spawn.getY(), spawn.getZ(), 0.0D, "world spawn"));
	}

	private BlockPos clientSpawnPos() {
		try {
			return minecraft.level.getRespawnData().pos();
		} catch (RuntimeException ignored) {
			// Older or unusual client level data can still expose the spawn through level data.
		}
		try {
			Object spawn = minecraft.level.getLevelData().getClass().getMethod("getSpawnPos").invoke(minecraft.level.getLevelData());
			return spawn instanceof BlockPos pos ? pos : null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private void openContextMenu(double mouseX, double mouseY) {
		FinderResult marker = markerAt(mouseX, mouseY);
		if (marker == null) {
			int blockX = screenToBlockX((int)mouseX);
			int blockZ = screenToBlockZ((int)mouseY);
			marker = new FinderResult(cursorBiomeLabel(blockX, blockZ), blockX, 64, blockZ, 0.0D, "cursor");
		}
		contextMarker = marker;
		contextX = (int)mouseX;
		contextY = (int)mouseY;
		rebuild();
	}

	private void openCompletedMenu(FinderResult marker, double mouseX, double mouseY) {
		completedMenuMarker = marker;
		completedMenuX = (int)mouseX;
		completedMenuY = (int)mouseY;
	}

	private boolean handleCompletedMenuClick(double mouseX, double mouseY) {
		int width = 104;
		int height = 24;
		int x = completedMenuX(width);
		int y = completedMenuY(height);
		if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
			completedMenuMarker = null;
			rebuild();
			return false;
		}
		toggleCompleted(completedMenuMarker);
		rebuild();
		return true;
	}

	private int completedMenuX(int width) {
		return Math.max(mapLeft + 4, Math.min(completedMenuX - width / 2, mapLeft + mapWidth - width - 4));
	}

	private int completedMenuY(int height) {
		return Math.max(mapTop + 4, Math.min(completedMenuY - height - 10, mapTop + mapHeight - height - 11));
	}

	private String cursorBiomeLabel(int blockX, int blockZ) {
		Long seed = seed();
		if (seed == null || !showBiomeColors()) {
			return "Map";
		}
		try {
			return biomeAtCurrentLayer(seed, dimension, blockX, blockZ)
					.map(BiomePalette::readableName)
					.map(name -> "Biome " + name)
					.orElse("Map");
		} catch (RuntimeException ignored) {
			return "Map";
		}
	}

	private boolean handleContextMenuClick(double mouseX, double mouseY) {
		WaypointIntegrations.Target waypointTarget = contextWaypointTargetAt(mouseX, mouseY);
		if (waypointTarget != null && contextMarker != null) {
			FinderResult marker = contextMarker;
			contextMarker = null;
			addWaypoint(marker, waypointTarget);
			rebuild();
			return true;
		}
		int option = contextMenuOptionAt(mouseX, mouseY);
		if (option < 0) {
			if (isInsideWaypointSubmenu(mouseX, mouseY)) {
				return true;
			}
			contextMarker = null;
			rebuild();
			return false;
		}
		FinderResult marker = contextMarker;
		ContextAction action = contextMenuActions().get(option);
		if (action == ContextAction.WAYPOINT) {
			List<WaypointIntegrations.Target> targets = availableWaypointTargets();
			if (targets.size() != 1) {
				return true;
			}
			contextMarker = null;
			addWaypoint(marker, targets.getFirst());
		} else if (action == ContextAction.CHAT) {
			contextMarker = null;
			openChat(marker);
		} else if (action == ContextAction.TELEPORT) {
			contextMarker = null;
			teleport(marker);
		}
		rebuild();
		return true;
	}

	private int contextMenuOptionAt(double mouseX, double mouseY) {
		List<ContextAction> options = contextMenuActions();
		if (mouseX < contextX + CONTEXT_MENU_PADDING || mouseX > contextX + CONTEXT_MENU_WIDTH - CONTEXT_MENU_PADDING
				|| mouseY < contextY + CONTEXT_MENU_HEADER_HEIGHT
				|| mouseY > contextY + CONTEXT_MENU_HEADER_HEIGHT + options.size() * CONTEXT_MENU_OPTION_HEIGHT) {
			return -1;
		}
		double relativeY = mouseY - contextY - CONTEXT_MENU_HEADER_HEIGHT;
		int option = (int)(relativeY / CONTEXT_MENU_OPTION_HEIGHT);
		if (relativeY - option * CONTEXT_MENU_OPTION_HEIGHT >= CONTEXT_MENU_BUTTON_HEIGHT) {
			return -1;
		}
		return option >= 0 && option < options.size() ? option : -1;
	}

	private WaypointIntegrations.Target contextWaypointTargetAt(double mouseX, double mouseY) {
		List<WaypointIntegrations.Target> targets = availableWaypointTargets();
		if (targets.isEmpty() || !isInsideWaypointSubmenu(mouseX, mouseY)) {
			return null;
		}
		double relativeY = mouseY - waypointSubmenuY(targets.size()) - CONTEXT_MENU_PADDING;
		int row = (int)(relativeY / CONTEXT_MENU_OPTION_HEIGHT);
		if (relativeY < 0 || relativeY - row * CONTEXT_MENU_OPTION_HEIGHT >= CONTEXT_MENU_BUTTON_HEIGHT) {
			return null;
		}
		return row >= 0 && row < targets.size() ? targets.get(row) : null;
	}

	private boolean isWaypointSubmenuOpen(double mouseX, double mouseY) {
		int option = contextMenuOptionAt(mouseX, mouseY);
		return option >= 0 && contextMenuActions().get(option) == ContextAction.WAYPOINT || isInsideWaypointSubmenu(mouseX, mouseY);
	}

	private boolean isInsideWaypointSubmenu(double mouseX, double mouseY) {
		int rows = availableWaypointTargets().size();
		if (rows == 0) {
			return false;
		}
		int x = waypointSubmenuX();
		int y = waypointSubmenuY(rows);
		int height = CONTEXT_MENU_PADDING * 2 + rows * CONTEXT_MENU_OPTION_HEIGHT - 2;
		return mouseX >= x && mouseX <= x + WAYPOINT_SUBMENU_WIDTH && mouseY >= y && mouseY <= y + height;
	}

	private int contextMenuX() {
		return Math.max(mapLeft + 4, Math.min(contextX, mapLeft + mapWidth - CONTEXT_MENU_WIDTH - 4));
	}

	private int contextMenuY() {
		return Math.max(mapTop + 4, Math.min(contextY, mapTop + mapHeight - contextMenuHeight() - 4));
	}

	private int contextMenuHeight() {
		return CONTEXT_MENU_PADDING + CONTEXT_MENU_HEADER_HEIGHT + contextMenuActions().size() * CONTEXT_MENU_OPTION_HEIGHT;
	}

	private List<ContextAction> contextMenuActions() {
		boolean canTeleport = canTeleport();
		List<ContextAction> actions = new ArrayList<>(3);
		if (!availableWaypointTargets().isEmpty()) {
			actions.add(ContextAction.WAYPOINT);
		}
		actions.add(ContextAction.CHAT);
		if (canTeleport) {
			actions.add(ContextAction.TELEPORT);
		}
		return actions;
	}

	private String contextActionLabel(ContextAction action) {
		return action.label;
	}

	private int waypointSubmenuX() {
		int preferred = contextX + CONTEXT_MENU_WIDTH + 3;
		if (preferred + WAYPOINT_SUBMENU_WIDTH <= mapLeft + mapWidth - 4) {
			return preferred;
		}
		return Math.max(mapLeft + 4, contextX - WAYPOINT_SUBMENU_WIDTH - 3);
	}

	private int waypointSubmenuY(int rows) {
		int height = CONTEXT_MENU_PADDING * 2 + Math.max(1, rows) * CONTEXT_MENU_OPTION_HEIGHT - 2;
		return Math.max(mapTop + 4, Math.min(contextY + CONTEXT_MENU_HEADER_HEIGHT, mapTop + mapHeight - height - 4));
	}

	private void addWaypoint(FinderResult marker, WaypointIntegrations.Target target) {
		boolean added = WaypointIntegrations.add(target, "WF " + marker.label(), new BlockPos(marker.blockX(), marker.blockY(), marker.blockZ()), dimension, currentClientDimension(), markerColor(marker) & 0x00FFFFFF);
		if (minecraft != null && minecraft.player != null) {
			String message = added ? "World Finder waypoint added to " + target.label() : "World Finder could not add waypoint to " + target.label();
			minecraft.player.sendSystemMessage(Component.literal(message));
		}
	}

	private List<WaypointIntegrations.Target> availableWaypointTargets() {
		return WaypointIntegrations.availableTargets(dimension, currentClientDimension());
	}

	private ResourceKey<Level> currentClientDimension() {
		return minecraft != null && minecraft.level != null ? minecraft.level.dimension() : null;
	}

	private void setDimension(WorldDimension newDimension) {
		if (newDimension == null || dimension == newDimension) {
			return;
		}
		dimension = newDimension;
		if (profileConfig != null) {
			profileConfig.dimension(dimension.configName());
			profileConfig.save();
		}
		iconScroll = 0;
		handleDimensionChanged();
	}

	private void handleDimensionChanged() {
		selectedMarker = null;
		hoveredMarker = null;
		contextMarker = null;
		completedMenuMarker = null;
		biomeDataCache.clearPending();
		biomeTileTextures.clearPendingUploads();
		clearMarkerCache();
	}

	private void toggleCompleted(FinderResult marker) {
		if (!isCompletableMarker(marker)) {
			return;
		}
		String key = completedMarkerKey(marker);
		if (completedStructureMarkers.contains(key)) {
			completedStructureMarkers.remove(key);
		} else {
			completedStructureMarkers.add(key);
		}
		saveCompletedStructureMarkers();
	}

	private boolean isCompleted(FinderResult marker) {
		return marker != null && completedStructureMarkers.contains(completedMarkerKey(marker));
	}

	private boolean isCompletableMarker(FinderResult marker) {
		return structurePresetIndex(marker) >= 0;
	}

	private int structurePresetIndex(FinderResult marker) {
		if (marker == null) {
			return -1;
		}
		for (int i = 0; i < StructurePreset.PRESETS.size(); i++) {
			if (StructurePreset.PRESETS.get(i).label().equals(marker.label())) {
				return i;
			}
		}
		return -1;
	}

	private String completedMarkerKey(FinderResult marker) {
		Long currentSeed = seed();
		String seedKey = currentSeed == null ? "unknown" : Long.toString(currentSeed);
		String label = marker.label().replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
		return seedKey + "\t" + dimension.configName() + "\t" + label + "\t" + marker.blockX() + "\t" + marker.blockZ();
	}

	private Set<String> loadCompletedStructureMarkers() {
		Path file = completedStructureMarkersFile();
		try {
			if (!Files.exists(file)) {
				return new HashSet<>();
			}
			return new HashSet<>(Files.readAllLines(file, StandardCharsets.UTF_8));
		} catch (IOException | RuntimeException ignored) {
			return new HashSet<>();
		}
	}

	private void saveCompletedStructureMarkers() {
		Path file = completedStructureMarkersFile();
		try {
			Files.createDirectories(file.getParent());
			List<String> lines = completedStructureMarkers.stream().sorted().toList();
			Files.write(file, lines, StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException ignored) {
		}
	}

	private Path completedStructureMarkersFile() {
		if (profileConfig != null) {
			return profileConfig.completedStructuresFile();
		}
		Path gameDirectory = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath();
		return gameDirectory.resolve("config").resolve("WorldFinder").resolve("Manual Seeds").resolve("completed-structures.txt");
	}

	private void openChat(FinderResult marker) {
		if (minecraft != null) {
			minecraft.setScreen(new ChatScreen(marker.label() + " " + marker.coordinates(), false));
		}
	}

	private void teleport(FinderResult marker) {
		if (minecraft != null && minecraft.player != null) {
			if (!canTeleport()) {
				return;
			}
			String targetDimension = dimension.levelKey().identifier().toString();
			if (dimension == WorldDimension.OVERWORLD || dimension == WorldDimension.END) {
				teleportToSafeSurface(withKnownSurfaceY(marker), targetDimension);
			} else if (dimension == WorldDimension.NETHER) {
				teleportToNetherSurface(marker, targetDimension);
			} else {
				teleportDirect(marker, targetDimension);
			}
			minecraft.setScreen(null);
		}
	}

	private boolean canTeleport() {
		if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) {
			return false;
		}
		try {
			var root = minecraft.player.connection.getCommands().getRoot();
			return root.getChild("execute") != null && (root.getChild("tp") != null || root.getChild("teleport") != null);
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	private void teleportDirect(FinderResult marker, String targetDimension) {
		teleportDirect(marker, targetDimension, marker.blockY());
	}

	private void teleportDirect(FinderResult marker, String targetDimension, int blockY) {
		SafeTeleportScheduler.suppressTeleportFeedback();
		minecraft.player.connection.sendCommand("execute in " + targetDimension + " run tp @s " + centeredCoordinate(marker.blockX()) + " " + blockY + " " + centeredCoordinate(marker.blockZ()));
	}

	private void teleportToSafeSurface(FinderResult marker, String targetDimension) {
		int minY = marker.blockY() - 8;
		int maxY = marker.blockY() + 24;
		teleportDirect(marker, targetDimension, maxY + 2);
		SafeTeleportScheduler.schedule(targetDimension, marker.blockX(), marker.blockZ(), minY, maxY, true);
	}

	private void teleportToNetherSurface(FinderResult marker, String targetDimension) {
		teleportDirect(marker, targetDimension, Math.min(120, Math.max(32, marker.blockY() + 8)));
		SafeTeleportScheduler.schedule(targetDimension, marker.blockX(), marker.blockZ(), 32, 118, true);
	}

	private static String centeredCoordinate(int blockCoordinate) {
		return Double.toString(blockCoordinate + 0.5D);
	}

	private int biomeColor(long seed, WorldDimension dimension, int blockX, int blockZ) {
		int color;
		try {
			color = dimension == WorldDimension.END
					? engine.endTerrainColorsAt(seed, blockX, blockZ, 1, 1, 1)[0]
					: biomeAtCurrentLayer(seed, dimension, blockX, blockZ)
							.map(BiomePalette::color)
							.orElse(0xAA475569);
		} catch (RuntimeException ignored) {
			color = dimension == WorldDimension.END ? 0xFF080D16 : 0xAA475569;
		}
		return color;
	}

	private Optional<String> biomeName(int chunkX, int chunkZ) {
		Long seed = seed();
		if (seed == null || !showBiomeColors()) {
			return Optional.empty();
		}
		try {
			return biomeAtCurrentLayer(seed, dimension, chunkX * 16 + 8, chunkZ * 16 + 8).map(BiomePalette::readableName);
		} catch (RuntimeException ignored) {
			return Optional.empty();
		}
	}

	private Optional<String> biomeAtCurrentLayer(long seed, WorldDimension dimension, int blockX, int blockZ) {
		BiomeLayer sampleLayer = activeBiomeTileLayer();
		if (sampleLayer == BiomeLayer.UNDERGROUND && dimension == WorldDimension.OVERWORLD) {
			Optional<String> fallback = Optional.empty();
			for (int sampleY : UNDERGROUND_POINT_SAMPLE_YS) {
				Optional<String> biome = engine.biomeIdAt(seed, dimension, blockX, sampleY, blockZ).map(WorldFinderMapScreen::normalizeBiomeId);
				if (biome.isPresent() && isUndergroundBiome(biome.get())) {
					return biome;
				}
				if (biome.isPresent()) {
					fallback = biome;
				}
			}
			return fallback;
		}
		return engine.biomeIdAt(seed, dimension, blockX, biomeSampleY(sampleLayer, dimension), blockZ).map(WorldFinderMapScreen::normalizeBiomeId);
	}

	private boolean toggleBiomeFilterAt(double mouseX, double mouseY) {
		Long seed = seed();
		if (seed == null || dimension == WorldDimension.END || biomeRenderMode == BiomeRenderMode.FAST || !showBiomeColors()) {
			return false;
		}
		int blockX = screenToBlockX((int)mouseX);
		int blockZ = screenToBlockZ((int)mouseY);
		Optional<String> biome = biomeAtCurrentLayer(seed, dimension, blockX, blockZ);
		if (biome.isEmpty()) {
			return false;
		}
		toggleBiomeFilter(biome.get());
		return true;
	}

	private boolean handleBiomeSuggestionClick(double mouseX, double mouseY) {
		int x = 10;
		int y = biomeSuggestionTop;
		int width = panelWidth - 20;
		if (mouseX < x || mouseX > x + width || mouseY < y || mouseY >= y + biomeSuggestionRows * BIOME_SUGGESTION_HEIGHT) {
			return false;
		}
		int index = (int)((mouseY - y) / BIOME_SUGGESTION_HEIGHT);
		List<String> suggestions = biomeSuggestions();
		if (index < 0 || index >= suggestions.size()) {
			return false;
		}
		toggleBiomeFilter(suggestions.get(index));
		return true;
	}

	private void selectFirstBiomeSuggestion() {
		List<String> suggestions = biomeSuggestions();
		if (!suggestions.isEmpty()) {
			toggleBiomeFilter(suggestions.get(0));
		}
	}

	private void toggleBiomeFilter(String biomeId) {
		String normalized = normalizeBiomeId(biomeId);
		if (normalized.isEmpty()) {
			return;
		}
		if (!selectedBiomeFilters.add(normalized)) {
			selectedBiomeFilters.remove(normalized);
		}
		saveBiomeFilters();
		rebuild();
	}

	private void clearBiomeFilter() {
		if (selectedBiomeFilters.isEmpty()) {
			return;
		}
		selectedBiomeFilters.clear();
		saveBiomeFilters();
		rebuild();
	}

	private void setAllVisibleStructureFilters(boolean enabled) {
		for (int index : visibleStructurePresetIndexes()) {
			structureFilters[index] = enabled;
		}
		saveStructureFilters();
		clearMarkerCache();
		rebuild();
	}

	private List<String> biomeSuggestions() {
		String query = biomeSearchText == null ? "" : biomeSearchText.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		if (dimension == cachedBiomeSuggestionDimension && biomeLayer == cachedBiomeSuggestionLayer && biomeSuggestionRows == cachedBiomeSuggestionRows && query.equals(cachedBiomeSuggestionText)) {
			return cachedBiomeSuggestions;
		}
		List<String> matches = new ArrayList<>();
		for (String biome : visibleBiomeList()) {
			String shortName = biome.substring(biome.indexOf(':') + 1);
			String readable = BiomePalette.readableName(biome).toLowerCase(Locale.ROOT).replace(' ', '_');
			if (query.isEmpty() || shortName.contains(query) || readable.contains(query) || biome.contains(query)) {
				matches.add(biome);
			}
			if (matches.size() >= biomeSuggestionRows) {
				break;
			}
		}
		cachedBiomeSuggestionDimension = dimension;
		cachedBiomeSuggestionLayer = biomeLayer;
		cachedBiomeSuggestionRows = biomeSuggestionRows;
		cachedBiomeSuggestionText = query;
		cachedBiomeSuggestions = List.copyOf(matches);
		return cachedBiomeSuggestions;
	}

	private List<String> visibleBiomeList() {
		return switch (dimension) {
			case OVERWORLD -> biomeLayer == BiomeLayer.UNDERGROUND ? OVERWORLD_UNDERGROUND_BIOMES : OVERWORLD_SURFACE_BIOMES;
			case NETHER -> NETHER_BIOMES;
			case END -> END_BIOMES;
		};
	}

	private boolean isControlDown() {
		if (minecraft == null) {
			return false;
		}
		long window = minecraft.getWindow().handle();
		return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
	}

	private static int biomeSampleY(BiomeLayer layer, WorldDimension dimension) {
		if (dimension == WorldDimension.OVERWORLD) {
			return layer == BiomeLayer.UNDERGROUND ? -52 : 320;
		}
		return 63;
	}

	private static boolean isUndergroundBiome(String biomeId) {
		String normalized = normalizeBiomeId(biomeId);
		return "minecraft:dripstone_caves".equals(normalized)
				|| "minecraft:lush_caves".equals(normalized)
				|| "minecraft:deep_dark".equals(normalized);
	}

	private int biomeTileSampleStep() {
		return BIOME_TILE_SAMPLE_STEP;
	}

	private BiomeLayer activeBiomeTileLayer() {
		return biomeRenderMode == BiomeRenderMode.FAST ? BiomeLayer.SURFACE : biomeLayer;
	}

	private static boolean isFastBiomeMode(BiomeLayer layer, String biomeFilterKey) {
		return layer == BiomeLayer.SURFACE && "fast".equals(biomeFilterKey);
	}

	private static int biomeTileBlocks(int sampleStep) {
		return sampleStep * BIOME_TILE_CELLS;
	}

	private String biomeStatus(int chunkX, int chunkZ) {
		if (!showBiomeColors()) {
			return "Biomes OFF";
		}
		if (seed() == null) {
			return "Seed required for biomes";
		}
		if (biomeLayerFailed) {
			return "Biomes unavailable";
		}
		if (!isBiomeTileReady(chunkX * 16 + 8, chunkZ * 16 + 8)) {
			return "Loading biome tile...";
		}
		return biomeName(chunkX, chunkZ).map(name -> "Biome " + name).orElse("Biome unavailable");
	}

	private boolean isBiomeTileReady(int blockX, int blockZ) {
		Long seed = seed();
		if (seed == null) {
			return false;
		}
		int sampleStep = biomeTileSampleStep();
		int tileBlocks = biomeTileBlocks(sampleStep);
		BiomeTileKey key = new BiomeTileKey(seed, dimension, activeBiomeTileLayer(), sampleStep, Math.floorDiv(blockX, tileBlocks), Math.floorDiv(blockZ, tileBlocks), activeBiomeFilterKey());
		return biomeTileTextures.contains(key) || biomeDataCache.get(key) != null;
	}

	private int markerColor(FinderResult marker) {
		if ("Spawn Point".equals(marker.label())) {
			return MAP_SPAWN;
		}
		for (int i = 0; i < StructurePreset.PRESETS.size(); i++) {
			if (StructurePreset.PRESETS.get(i).label().equals(marker.label())) {
				return STRUCTURE_COLORS[i % STRUCTURE_COLORS.length];
			}
		}
		return 0xFFEAB308;
	}

	private void drawMarkerIcon(GuiGraphicsExtractor graphics, FinderResult marker, int centerX, int centerY, boolean hovered) {
		int x = centerX - 8;
		int y = centerY - 8;
		boolean completed = isCompleted(marker);
		float scale = 1.0F;
		if (hovered) {
			scale *= 1.15F;
		}
		float alpha = completed ? hovered ? 0.55F : 0.35F : 1.0F;
		if (scale != 1.0F) {
			graphics.pose().pushMatrix();
			graphics.pose().scaleAround(scale, centerX, centerY);
		}
		drawMarkerSprite(graphics, marker, x, y, alpha);
		if (scale != 1.0F) {
			graphics.pose().popMatrix();
		}
	}

	private void drawMarkerSprite(GuiGraphicsExtractor graphics, FinderResult marker, int x, int y, float alpha) {
		drawSpriteIcon(graphics, MapIcons.markerSprite(marker), x, y, 16, alpha);
	}

	private void drawSpriteIcon(GuiGraphicsExtractor graphics, MapIcons.SpriteIcon icon, int x, int y, int size, float alpha) {
		int color = withAlpha(0xFFFFFF, (int)(alpha * 255.0F));
		if (icon.rawTexture()) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x, y, 0.0F, 0.0F, size, size, 16, 16, color);
			return;
		}
		TextureAtlasSprite sprite = markerSpriteCache.computeIfAbsent(icon, key -> minecraft.getAtlasManager().get(new SpriteId(key.atlas(), key.texture())));
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, color);
	}

	private void updateMarkerTooltipTimer(FinderResult marker) {
		if (marker == null && delayedTooltipMarker == null) {
			return;
		}
		if (marker != null && marker.equals(delayedTooltipMarker)) {
			return;
		}
		delayedTooltipMarker = marker;
		delayedTooltipStartedAt = System.currentTimeMillis();
	}

	private void updatePlayerTooltipTimer(boolean hoveringPlayer) {
		if (!hoveringPlayer) {
			hoveredPlayerStartedAt = 0L;
			return;
		}
		if (hoveredPlayerStartedAt == 0L) {
			hoveredPlayerStartedAt = System.currentTimeMillis();
		}
	}

	private void fillChunk(GuiGraphicsExtractor graphics, int chunkX, int chunkZ, int color) {
		int x1 = worldToScreenX(chunkX * 16);
		int y1 = worldToScreenZ(chunkZ * 16);
		int x2 = worldToScreenX(chunkX * 16 + 16);
		int y2 = worldToScreenZ(chunkZ * 16 + 16);
		int left = Math.max(mapLeft, Math.min(x1, x2));
		int right = Math.min(mapLeft + mapWidth, Math.max(x1, x2));
		int top = Math.max(mapTop, Math.min(y1, y2));
		int bottom = Math.min(mapTop + mapHeight, Math.max(y1, y2));
		if (right > left && bottom > top) {
			graphics.fill(left, top, right, bottom, color);
		}
	}

	private void fillBlockCell(GuiGraphicsExtractor graphics, int blockX, int blockZ, int blockSize, int color) {
		int x1 = worldToScreenX(blockX);
		int y1 = worldToScreenZ(blockZ);
		int x2 = worldToScreenX(blockX + blockSize);
		int y2 = worldToScreenZ(blockZ + blockSize);
		int left = Math.max(mapLeft, Math.min(x1, x2));
		int right = Math.min(mapLeft + mapWidth, Math.max(x1, x2));
		int top = Math.max(mapTop, Math.min(y1, y2));
		int bottom = Math.min(mapTop + mapHeight, Math.max(y1, y2));
		if (right > left && bottom > top) {
			graphics.fill(left, top, right, bottom, color);
		}
	}

	private int biomeRenderColor(String biomeId) {
		if (biomeId == null) {
			return 0xFF2D3748;
		}
		return switch (biomeId) {
			case "minecraft:river", "minecraft:frozen_river" -> 0xFF0B55D9;
			case "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean", "minecraft:deep_cold_ocean",
					"minecraft:frozen_ocean", "minecraft:deep_frozen_ocean" -> 0xFF25308A;
			case "minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean" -> 0xFF287A92;
			case "minecraft:beach", "minecraft:snowy_beach" -> 0xFFE8D36A;
			case "minecraft:desert" -> 0xFFD9B95F;
			case "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands" -> 0xFF9A6B4F;
			case "minecraft:swamp", "minecraft:mangrove_swamp" -> 0xFF506B32;
			case "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle" -> 0xFF0E6A28;
			case "minecraft:dark_forest", "minecraft:pale_garden" -> 0xFF214F42;
			case "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
					"minecraft:old_growth_birch_forest", "minecraft:taiga", "minecraft:snowy_taiga",
					"minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga" -> 0xFF2E7042;
			case "minecraft:savanna", "minecraft:savanna_plateau" -> 0xFF97B55B;
			case "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:grove", "minecraft:snowy_slopes",
					"minecraft:frozen_peaks", "minecraft:jagged_peaks" -> 0xFFD8EEF0;
			case "minecraft:meadow", "minecraft:cherry_grove", "minecraft:sunflower_plains" -> 0xFF9BC56B;
			case "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
					"minecraft:windswept_savanna", "minecraft:stony_peaks", "minecraft:stony_shore" -> 0xFF7C8473;
			case "minecraft:mushroom_fields" -> 0xFFB48BC2;
			case "minecraft:dripstone_caves" -> 0xFF7B6551;
			case "minecraft:lush_caves" -> 0xFF2DBE83;
			case "minecraft:deep_dark" -> 0xFF111827;
			case "minecraft:nether_wastes" -> 0xFF7F1D1D;
			case "minecraft:crimson_forest" -> 0xFF8F1D3D;
			case "minecraft:warped_forest" -> 0xFF0F766E;
			case "minecraft:soul_sand_valley" -> 0xFF7C6F64;
			case "minecraft:basalt_deltas" -> 0xFF3F3F46;
			case "minecraft:the_end" -> 0xFFE6E7A6;
			case "minecraft:end_highlands" -> 0xFFF1F2BA;
			case "minecraft:end_midlands" -> 0xFFD8DA91;
			case "minecraft:small_end_islands" -> 0xFFCACC7D;
			case "minecraft:end_barrens" -> 0xFFB8BA70;
			default -> 0xFF6B8E4E;
		};
	}

	private void forEachVisibleChunk(ChunkConsumer consumer) {
		int minChunkX = screenToChunkX(mapLeft);
		int maxChunkX = screenToChunkX(mapLeft + mapWidth) + 1;
		int minChunkZ = screenToChunkZ(mapTop);
		int maxChunkZ = screenToChunkZ(mapTop + mapHeight) + 1;
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				consumer.accept(chunkX, chunkZ);
			}
		}
	}

	private int visibleChunkCount() {
		return Math.max(0, screenToChunkX(mapLeft + mapWidth) - screenToChunkX(mapLeft) + 2) * Math.max(0, screenToChunkZ(mapTop + mapHeight) - screenToChunkZ(mapTop) + 2);
	}

	private int worldToScreenX(double blockX) {
		return (int)Math.round(mapLeft + mapWidth / 2.0D + (blockX - cameraX) * blockScale());
	}

	private int worldToScreenZ(double blockZ) {
		return (int)Math.round(mapTop + mapHeight / 2.0D + (blockZ - cameraZ) * blockScale());
	}

	private int screenToBlockX(int screenX) {
		return (int)Math.floor(cameraX + (screenX - (mapLeft + mapWidth / 2.0D)) / blockScale());
	}

	private int screenToBlockZ(int screenY) {
		return (int)Math.floor(cameraZ + (screenY - (mapTop + mapHeight / 2.0D)) / blockScale());
	}

	private int screenToChunkX(int screenX) {
		return Math.floorDiv(screenToBlockX(screenX), 16);
	}

	private int screenToChunkZ(int screenY) {
		return Math.floorDiv(screenToBlockZ(screenY), 16);
	}

	private double blockScale() {
		return displayBlockScale() / guiPixelScale();
	}

	private double quartScale() {
		return blockScale() * 4.0D;
	}

	private double displayBlockScale() {
		return BLOCK_ZOOMS[zoomIndex];
	}

	private double guiPixelScale() {
		if (minecraft == null) {
			return 1.0D;
		}
		return Math.max(1.0D, minecraft.getWindow().getGuiScale());
	}

	private void zoom(int direction) {
		zoom(direction, mapLeft + mapWidth / 2.0D, mapTop + mapHeight / 2.0D);
	}

	private void zoom(int direction, double anchorScreenX, double anchorScreenY) {
		int previous = zoomIndex;
		double oldScale = blockScale();
		double anchorBlockX = cameraX + (anchorScreenX - (mapLeft + mapWidth / 2.0D)) / oldScale;
		double anchorBlockZ = cameraZ + (anchorScreenY - (mapTop + mapHeight / 2.0D)) / oldScale;
		zoomIndex = Math.max(0, Math.min(BLOCK_ZOOMS.length - 1, zoomIndex + direction));
		if (previous != zoomIndex) {
			double newScale = blockScale();
			cameraX = anchorBlockX - (anchorScreenX - (mapLeft + mapWidth / 2.0D)) / newScale;
			cameraZ = anchorBlockZ - (anchorScreenY - (mapTop + mapHeight / 2.0D)) / newScale;
			zoomOverlayStartedAt = System.currentTimeMillis();
			zoomOverlayUntil = zoomOverlayStartedAt + 1350L;
			invalidateMarkerCache();
		}
	}

	private String zoomLabel() {
		double scale = displayBlockScale();
		if (scale >= 1.0D) {
			return trimDouble(scale) + "px/block";
		}
		return "1px/" + trimDouble(1.0D / scale) + " blocks";
	}

	private void centerOnPlayer() {
		if (minecraft != null && minecraft.player != null) {
			cameraX = minecraft.player.getX();
			cameraZ = minecraft.player.getZ();
			cameraInitialized = true;
			invalidateMarkerCache();
		}
	}

	private void goToSearchCoordinates() {
		if (xSearchInput == null || zSearchInput == null) {
			return;
		}
		Integer x = parseCoordinate(xSearchInput.getValue());
		Integer z = parseCoordinate(zSearchInput.getValue());
		if (x == null || z == null) {
			return;
		}
		centerOn(x, z);
	}

	private void centerOn(int blockX, int blockZ) {
		cameraX = blockX;
		cameraZ = blockZ;
		cameraInitialized = true;
		invalidateMarkerCache();
	}

	private static Integer parseCoordinate(String value) {
		try {
			return (int)Math.floor(Double.parseDouble(value));
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private void rebuild() {
		clearWidgets();
		init();
	}

	private void clearMarkerCache() {
		cachedMarkersKey = null;
		pendingMarkersKey = null;
		structureScanCache.clearPending();
		cachedMarkers = List.of();
		cachedStructureCounts = Map.of();
	}

	private void invalidateMarkerCache() {
		cachedMarkersKey = null;
		pendingMarkersKey = null;
		structureScanCache.clearPending();
	}

	private Long seed() {
		Long autoSeed = singleplayerSeed();
		if (autoSeed != null) {
			return autoSeed;
		}
		String value = seedInput == null ? fallbackSeedConfigValue() : seedInput.getValue();
		if (value.equals(cachedManualSeedText)) {
			return cachedManualSeed;
		}
		cachedManualSeedText = value;
		cachedManualSeed = parseSeed(value);
		return cachedManualSeed;
	}

	private void clearSeedCaches() {
		singleplayerSeedResolved = false;
		cachedSingleplayerSeed = null;
		lastSingleplayerSeedAttemptMillis = 0L;
		cachedManualSeedText = null;
		cachedManualSeed = null;
	}

	private Long singleplayerSeed() {
		if (singleplayerSeedResolved && cachedSingleplayerSeed != null) {
			return cachedSingleplayerSeed;
		}
		long now = System.currentTimeMillis();
		if (now - lastSingleplayerSeedAttemptMillis < 1000L) {
			return null;
		}
		lastSingleplayerSeedAttemptMillis = now;
		if (minecraft == null || !minecraft.hasSingleplayerServer()) {
			return null;
		}
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server == null) {
			return null;
		}
		try {
			return cacheSingleplayerSeed(server.overworld().getSeed());
		} catch (RuntimeException ignored) {
		}
		try {
			Object seed = invokeSeed(invokeWorldGenOptions(server.getWorldData()));
			if (seed instanceof Number number) {
				return cacheSingleplayerSeed(number.longValue());
			}
		} catch (RuntimeException ignored) {
			// Production runtime names can differ from the mapped development names.
		}
		return readSingleplayerSeedFromLevelDat(server);
	}

	private Long readSingleplayerSeedFromLevelDat(IntegratedServer server) {
		try {
			Path levelDat = server.getWorldPath(LevelResource.LEVEL_DATA_FILE);
			if (!Files.isRegularFile(levelDat)) {
				return null;
			}
			CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
			CompoundTag data = root.getCompoundOrEmpty("Data");
			CompoundTag worldGenSettings = data.getCompoundOrEmpty("WorldGenSettings");
			Optional<Long> seed = worldGenSettings.getLong("seed");
			if (seed.isPresent()) {
				return cacheSingleplayerSeed(seed.get());
			}
			return data.getLong("RandomSeed").map(this::cacheSingleplayerSeed).orElse(null);
		} catch (IOException | RuntimeException ignored) {
			return null;
		}
	}

	private Long cacheSingleplayerSeed(long seed) {
		cachedSingleplayerSeed = seed;
		singleplayerSeedResolved = true;
		if (profileConfig != null && !Long.toString(seed).equals(profileConfig.seed())) {
			profileConfig.seed(Long.toString(seed));
			profileConfig.save();
		}
		return cachedSingleplayerSeed;
	}

	private String fallbackSeedConfigValue() {
		return profileConfig == null ? WorldFinderConfig.SEED.get() : profileConfig.seed();
	}

	private static Object invokeWorldGenOptions(Object worldData) {
		try {
			return worldData.getClass().getMethod("worldGenOptions").invoke(worldData);
		} catch (ReflectiveOperationException first) {
			try {
				return worldData.getClass().getMethod("worldOptions").invoke(worldData);
			} catch (ReflectiveOperationException second) {
				throw new IllegalStateException("Unable to read singleplayer world generation options", second);
			}
		}
	}

	private static Object invokeSeed(Object worldOptions) {
		try {
			return worldOptions.getClass().getMethod("seed").invoke(worldOptions);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Unable to read singleplayer world seed", exception);
		}
	}

	private void syncSeedInputWithAutoSeed() {
		if (seedInput == null) {
			return;
		}
		Long autoSeed = singleplayerSeed();
		if (autoSeed == null) {
			return;
		}
		String value = Long.toString(autoSeed);
		if (!value.equals(seedInput.getValue())) {
			seedInput.setValue(value);
			clearMarkerCache();
		}
		seedInput.setEditable(false);
	}

	private static Long parseSeed(String rawSeed) {
		String seed = rawSeed == null ? "" : rawSeed.trim();
		if (seed.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(seed);
		} catch (NumberFormatException ignored) {
			return (long)seed.hashCode();
		}
	}

	private void layout() {
		int preferredPanelWidth = width >= 1200 ? 280 : width >= 760 ? 240 : 204;
		int minimumPanelWidth = width < 520 ? 168 : 200;
		panelWidth = Math.min(Math.max(minimumPanelWidth, preferredPanelWidth), Math.max(120, width - 96));
		mapLeft = panelWidth + 1;
		mapTop = 0;
		mapWidth = Math.max(80, width - mapLeft);
		mapHeight = Math.max(80, height);
		iconGridLeft = 10;
		biomeSuggestionTop = 176;
		biomeSuggestionRows = height < 440 ? 2 : height < 520 ? 3 : BIOME_SUGGESTION_MAX_ROWS;
		panelStatusTop = biomeSuggestionTop + biomeSuggestionRows * BIOME_SUGGESTION_HEIGHT + 12;
		iconGridTop = panelStatusTop + 46;
		int availableGridWidth = Math.max(FILTER_CARD_WIDTH, panelWidth - 24);
		iconGridColumns = Math.max(1, (availableGridWidth + FILTER_CARD_GAP) / (FILTER_CARD_WIDTH + FILTER_CARD_GAP));
		filterCardWidth = Math.max(FILTER_CARD_WIDTH, (availableGridWidth - (iconGridColumns - 1) * FILTER_CARD_GAP) / iconGridColumns);
		int gridBottom = Math.max(iconGridTop, height - 64);
		iconGridRows = Math.max(0, (gridBottom - iconGridTop) / (FILTER_CARD_HEIGHT + FILTER_CARD_GAP));
	}

	private Button button(int x, int y, int width, String label, Button.OnPress onPress) {
		return Button.builder(Component.literal(label), onPress).bounds(x, y, width, 20).build();
	}

	private boolean isInsideMap(double x, double y) {
		return x >= mapLeft && x <= mapLeft + mapWidth && y >= mapTop && y <= mapTop + mapHeight;
	}

	private boolean isInsideIconGrid(double mouseX, double mouseY) {
		if (iconGridRows <= 0) {
			return false;
		}
		return mouseX >= iconGridLeft
				&& mouseX <= iconGridLeft + iconGridColumns * filterCardWidth() + (iconGridColumns - 1) * FILTER_CARD_GAP
				&& mouseY >= iconGridTop
				&& mouseY <= iconGridTop + iconGridRows * (FILTER_CARD_HEIGHT + FILTER_CARD_GAP);
	}

	private int structureIndexAt(double mouseX, double mouseY) {
		if (!isInsideIconGrid(mouseX, mouseY)) {
			return -1;
		}
		int cardWidth = filterCardWidth();
		int column = (int)((mouseX - iconGridLeft) / (cardWidth + FILTER_CARD_GAP));
		int row = (int)((mouseY - iconGridTop) / (FILTER_CARD_HEIGHT + FILTER_CARD_GAP));
		int localX = (int)(mouseX - iconGridLeft - column * (cardWidth + FILTER_CARD_GAP));
		int localY = (int)(mouseY - iconGridTop - row * (FILTER_CARD_HEIGHT + FILTER_CARD_GAP));
		if (column < 0 || column >= iconGridColumns || row < 0 || row >= iconGridRows) {
			return -1;
		}
		if (localX < 0 || localX > cardWidth || localY < 0 || localY > FILTER_CARD_HEIGHT) {
			return -1;
		}
		int listIndex = iconScroll + row * iconGridColumns + column;
		int[] indexes = visibleStructurePresetIndexes();
		return listIndex >= 0 && listIndex < indexes.length ? indexes[listIndex] : -1;
	}

	private int filterCardWidth() {
		return filterCardWidth;
	}

	private int[] visibleStructurePresetIndexes() {
		return STRUCTURE_PRESETS_BY_DIMENSION.getOrDefault(dimension, EMPTY_PRESET_INDEXES);
	}

	private int enabledStructureFilterCount() {
		int count = 0;
		for (int index : visibleStructurePresetIndexes()) {
			if (structureFilters[index]) {
				count++;
			}
		}
		return count;
	}

	private int structureFilterMask() {
		int mask = 0;
		int bit = 1;
		for (int index : visibleStructurePresetIndexes()) {
			if (structureFilters[index]) {
				mask |= bit;
			}
			bit <<= 1;
		}
		return mask;
	}

	private int countVisibleStructureMarkers(int presetIndex) {
		if (!showStructuresOnMap() || !structureFilters[presetIndex]) {
			return 0;
		}
		markers();
		return cachedStructureCounts.getOrDefault(presetIndex, 0);
	}

	private FinderResult withKnownSurfaceY(FinderResult result) {
		if (result == null || minecraft == null || minecraft.level == null || result.note().contains("biome")) {
			return result;
		}
		int chunkX = Math.floorDiv(result.blockX(), 16);
		int chunkZ = Math.floorDiv(result.blockZ(), 16);
		if (minecraft.level.dimension().equals(dimension.levelKey()) && minecraft.level.hasChunk(chunkX, chunkZ)) {
			int surfaceY = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, result.blockX(), result.blockZ());
			if (isValidTeleportSurfaceY(surfaceY)) {
				return withSurfaceY(result, surfaceY, "loaded surface Y");
			}
		}
		Long seed = seed();
		if (seed != null && dimension == WorldDimension.OVERWORLD) {
			OptionalInt surfaceY = engine.surfaceYAt(seed, dimension, result.blockX(), result.blockZ());
			if (surfaceY.isPresent() && isValidTeleportSurfaceY(surfaceY.getAsInt())) {
				return withSurfaceY(result, surfaceY.getAsInt(), "seed surface Y");
			}
		}
		return result;
	}

	private boolean isValidTeleportSurfaceY(int surfaceY) {
		return surfaceY > -60 && surfaceY < 400;
	}

	private FinderResult withSurfaceY(FinderResult result, int surfaceY, String source) {
		if (surfaceY == result.blockY()) {
			return result;
		}
		String note = result.note().contains(source) ? result.note() : result.note() + "; " + source;
		return new FinderResult(result.label(), result.blockX(), surfaceY, result.blockZ(), result.distance(), note);
	}

	private String dimensionShortLabel() {
		return switch (dimension) {
			case OVERWORLD -> "Overworld";
			case NETHER -> "Nether";
			case END -> "End";
		};
	}

	private static boolean[] defaultStructureFilters(String configured) {
		boolean[] filters = new boolean[StructurePreset.PRESETS.size()];
		Set<String> enabledKeys = new HashSet<>();
		if (configured != null && !configured.isBlank()) {
			for (String key : configured.split(",")) {
				String trimmed = key.trim();
				if (!trimmed.isEmpty()) {
					enabledKeys.add(trimmed);
				}
			}
		}
		for (int i = 0; i < filters.length; i++) {
			filters[i] = enabledKeys.contains(StructurePreset.PRESETS.get(i).configKey());
		}
		return filters;
	}

	private void saveStructureFilters() {
		List<String> enabledKeys = new ArrayList<>();
		for (int i = 0; i < structureFilters.length; i++) {
			if (structureFilters[i]) {
				enabledKeys.add(StructurePreset.PRESETS.get(i).configKey());
			}
		}
		profileConfig.enabledStructureFilters(String.join(",", enabledKeys));
		profileConfig.save();
	}

	private static Set<String> loadBiomeFilters(String configured) {
		Set<String> filters = new HashSet<>();
		if (configured != null && !configured.isBlank()) {
			for (String key : configured.split(",")) {
				String trimmed = normalizeBiomeId(key.trim());
				if (!trimmed.isEmpty()) {
					filters.add(trimmed);
				}
			}
		}
		return filters;
	}

	private void saveBiomeFilters() {
		profileConfig.selectedBiomeFilters(biomeFilterKey());
		profileConfig.save();
	}

	private String biomeFilterKey() {
		List<String> filters = new ArrayList<>(selectedBiomeFilters);
		filters.sort(String::compareTo);
		return String.join(",", filters);
	}

	private String activeBiomeFilterKey() {
		if (biomeRenderMode == BiomeRenderMode.FAST) {
			return "fast";
		}
		return dimension == WorldDimension.END ? "" : biomeFilterKey();
	}

	private static Set<String> biomeFilterSet(String filterKey) {
		Set<String> filters = new HashSet<>();
		if (filterKey == null || filterKey.isBlank()) {
			return filters;
		}
		for (String key : filterKey.split(",")) {
			String trimmed = normalizeBiomeId(key.trim());
			if (!trimmed.isEmpty()) {
				filters.add(trimmed);
			}
		}
		return filters;
	}

	private static String normalizeBiomeId(String biomeId) {
		if (biomeId == null || biomeId.isBlank()) {
			return "";
		}
		return biomeId.indexOf(':') >= 0 ? biomeId : "minecraft:" + biomeId;
	}

	private static int fadedBiomeColor(int color) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		r = (int)(r * 0.22D + 245 * 0.78D);
		g = (int)(g * 0.22D + 247 * 0.78D);
		b = (int)(b * 0.22D + 250 * 0.78D);
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	private static String onOff(boolean value) {
		return value ? "ON" : "OFF";
	}

	private static String trim(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
	}

	private String trimToWidth(String value, int maxWidth) {
		if (font.width(value) <= maxWidth) {
			return value;
		}
		String suffix = "...";
		int end = value.length();
		while (end > 0 && font.width(value.substring(0, end) + suffix) > maxWidth) {
			end--;
		}
		return end <= 0 ? suffix : value.substring(0, end) + suffix;
	}

	private static String trimDouble(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.001D) {
			return Integer.toString((int)Math.rint(value));
		}
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	private static int withAlpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
	}

	@FunctionalInterface
	private interface ChunkConsumer {
		void accept(int chunkX, int chunkZ);
	}
}
