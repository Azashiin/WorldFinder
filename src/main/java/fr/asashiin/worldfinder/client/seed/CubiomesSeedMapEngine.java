package fr.asashiin.worldfinder.client.seed;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

import com.github.cubiomes.Cubiomes;
import com.github.cubiomes.EndNoise;
import com.github.cubiomes.Generator;
import com.github.cubiomes.OreVeinParameters;
import com.github.cubiomes.Piece;
import com.github.cubiomes.Pos;
import com.github.cubiomes.Pos3;
import com.github.cubiomes.Range;
import com.github.cubiomes.StructureConfig;
import com.github.cubiomes.StructureVariant;
import com.github.cubiomes.SurfaceNoise;

import fr.asashiin.worldfinder.client.finder.FinderResult;
import fr.asashiin.worldfinder.client.finder.StructurePreset;
import fr.asashiin.worldfinder.client.finder.WorldDimension;
import fr.asashiin.worldfinder.client.screen.BiomePalette;
import net.minecraft.core.BlockPos;

public final class CubiomesSeedMapEngine implements SeedMapEngine {
	private static final int VERSION = Cubiomes.MC_NEWEST();
	private static final int GENERATOR_FLAGS = 0;
	private static final int MAX_CONTEXT_CACHE = 8;
	private static final int MAX_END_CITY_PIECES = Math.max(Cubiomes.END_CITY_PIECES_MAX(), 400);
	private static final int MAX_ORE_VEIN_SCAN_CHUNKS = 6000;
	private static final int END_VOID_COLOR = 0xFF050813;
	private static final int END_MAIN_ISLAND_COLOR = 0xFFE6E7A6;
	private static final int[] CUBIOMES_BIOME_COLORS = loadCubiomesBiomeColors();

	private final SeedMapEngine fallback;
	private final Arena arena = Arena.ofShared();
	private final Map<ContextKey, Context> contexts = new LinkedHashMap<>(MAX_CONTEXT_CACHE, 0.75F, true);
	private final Map<Integer, Integer> biomeColorCache = new ConcurrentHashMap<>();

	public CubiomesSeedMapEngine(SeedMapEngine fallback) {
		this.fallback = fallback;
	}

	public static boolean isAvailable() {
		return CubiomesNative.load();
	}

	@Override
	public String name() {
		return "Cubiomes vanilla";
	}

	@Override
	public Optional<String> biomeIdAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ) {
		try {
			MemorySegment generator = context(seed, dimension).biomeGenerator();
			int biome = Cubiomes.getBiomeAt(generator, 4, Math.floorDiv(blockX, 4), Math.floorDiv(blockY, 4), Math.floorDiv(blockZ, 4));
			if (biome < 0) {
				return Optional.empty();
			}
			String name = Cubiomes.biome2str(VERSION, biome).getString(0);
			return name == null || name.isBlank() ? Optional.empty() : Optional.of("minecraft:" + name);
		} catch (RuntimeException | Error exception) {
			return fallback.biomeIdAt(seed, dimension, blockX, blockY, blockZ);
		}
	}

	@Override
	public OptionalInt surfaceYAt(long seed, WorldDimension dimension, int blockX, int blockZ) {
		return fallback.surfaceYAt(seed, dimension, blockX, blockZ);
	}

	@Override
	public int[] biomeColorsAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ, int sampleStep, int width, int height) {
		BiomeBatch batch = biomeBatch(seed, dimension, blockX, blockY, blockZ, sampleStep, width, height);
		if (batch == null) {
			return SeedMapEngine.super.biomeColorsAt(seed, dimension, blockX, blockY, blockZ, sampleStep, width, height);
		}
		int[] colors = new int[width * height];
		Map<Integer, Integer> localColors = new HashMap<>();
		for (int localZ = 0; localZ < height; localZ++) {
			for (int localX = 0; localX < width; localX++) {
				int biomeId = batch.biomeId(localX, localZ);
				colors[localZ * width + localX] = localColors.computeIfAbsent(biomeId, this::biomeColor);
			}
		}
		return colors;
	}

	@Override
	public String[] biomeIdsAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ, int sampleStep, int width, int height) {
		BiomeBatch batch = biomeBatch(seed, dimension, blockX, blockY, blockZ, sampleStep, width, height);
		if (batch == null) {
			return SeedMapEngine.super.biomeIdsAt(seed, dimension, blockX, blockY, blockZ, sampleStep, width, height);
		}
		String[] biomes = new String[width * height];
		for (int localZ = 0; localZ < height; localZ++) {
			for (int localX = 0; localX < width; localX++) {
				biomes[localZ * width + localX] = biomeId(batch.biomeId(localX, localZ));
			}
		}
		return biomes;
	}

	private BiomeBatch biomeBatch(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ, int sampleStep, int width, int height) {
		if (sampleStep < 4 || sampleStep % 4 != 0) {
			return null;
		}
		try (Arena temp = Arena.ofConfined()) {
			int rangeScale = Math.min(sampleStep, 16);
			int stride = Math.max(1, sampleStep / rangeScale);
			int rangeWidth = (width - 1) * stride + 1;
			int rangeHeight = (height - 1) * stride + 1;
			MemorySegment range = Range.allocate(temp);
			Range.scale(range, rangeScale);
			Range.x(range, Math.floorDiv(blockX, rangeScale));
			Range.z(range, Math.floorDiv(blockZ, rangeScale));
			Range.sx(range, rangeWidth);
			Range.sz(range, rangeHeight);
			Range.y(range, Math.floorDiv(blockY, rangeScale));
			Range.sy(range, 1);

			MemorySegment generator;
			long cacheSize;
			generator = context(seed, dimension).biomeGenerator();
			cacheSize = Cubiomes.getMinCacheSize(generator, Range.scale(range), Range.sx(range), Range.sy(range), Range.sz(range));
			MemorySegment biomeIds = temp.allocate(ValueLayout.JAVA_INT, cacheSize);
			int result = Cubiomes.genBiomes(generator, biomeIds, range);
			if (result != 0) {
				return null;
			}
			int[] ids = new int[rangeWidth * rangeHeight];
			for (int index = 0; index < ids.length; index++) {
				ids[index] = biomeIds.getAtIndex(ValueLayout.JAVA_INT, index);
			}
			return new BiomeBatch(ids, rangeWidth, stride);
		} catch (RuntimeException | Error exception) {
			return null;
		}
	}

	private static int[] emptyBiomeColors(int width, int height) {
		int[] colors = new int[width * height];
		Arrays.fill(colors, 0xFF475569);
		return colors;
	}

	@Override
	public int[] endTerrainColorsAt(long seed, int blockX, int blockZ, int sampleStep, int width, int height) {
		float[] terrainHeights = endTerrainHeights(seed, blockX, blockZ, sampleStep, width, height);
		if (terrainHeights == null) {
			return biomeColorsAt(seed, WorldDimension.END, blockX, 63, blockZ, sampleStep, width, height);
		}
		int[] colors = new int[width * height];
		for (int localZ = 0; localZ < height; localZ++) {
			for (int localX = 0; localX < width; localX++) {
				int index = localZ * width + localX;
				colors[index] = terrainHeights[index] <= 0.0F ? END_VOID_COLOR : END_MAIN_ISLAND_COLOR;
			}
		}
		return colors;
	}

	private float[] endTerrainHeights(long seed, int blockX, int blockZ, int sampleStep, int width, int height) {
		int scale = endHeightScale(sampleStep);
		if (scale == 0 || blockX % scale != 0 || blockZ % scale != 0 || sampleStep != scale) {
			return endTerrainHeightsFallback(seed, blockX, blockZ, sampleStep, width, height);
		}
		try (Arena temp = Arena.ofConfined()) {
			Context context = context(seed, WorldDimension.END);
			MemorySegment heights = temp.allocate(ValueLayout.JAVA_FLOAT, (long)width * height);
			int result = Cubiomes.mapEndSurfaceHeight(
					heights,
					context.endNoise(),
					context.surfaceNoise(),
					Math.floorDiv(blockX, scale),
					Math.floorDiv(blockZ, scale),
					width,
					height,
					scale,
					0);
			if (result != 0) {
				return null;
			}
			float[] values = new float[width * height];
			for (int index = 0; index < values.length; index++) {
				values[index] = heights.getAtIndex(ValueLayout.JAVA_FLOAT, index);
			}
			return values;
		} catch (RuntimeException | Error exception) {
			return endTerrainHeightsFallback(seed, blockX, blockZ, sampleStep, width, height);
		}
	}

	private float[] endTerrainHeightsFallback(long seed, int blockX, int blockZ, int sampleStep, int width, int height) {
		try {
			float[] values = new float[width * height];
			for (int localZ = 0; localZ < height; localZ++) {
				for (int localX = 0; localX < width; localX++) {
					int sampleX = blockX + localX * sampleStep;
					int sampleZ = blockZ + localZ * sampleStep;
					values[localZ * width + localX] = Cubiomes.getEndSurfaceHeight(VERSION, seed, sampleX, sampleZ);
				}
			}
			return values;
		} catch (RuntimeException | Error exception) {
			return null;
		}
	}

	private static int endHeightScale(int sampleStep) {
		return switch (sampleStep) {
			case 1, 2, 4, 8 -> sampleStep;
			default -> 0;
		};
	}

	@Override
	public List<FinderResult> structuresInView(long seed, WorldDimension dimension, StructurePreset preset, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
		try {
			if (preset.dimension() != dimension) {
				return List.of();
			}
			Context context = context(seed, dimension);
			List<FinderResult> results = new ArrayList<>();
			for (int structureId : cubiomesStructureIds(preset)) {
				addVisibleStructures(seed, preset.label(), structureId, minChunkX, maxChunkX, minChunkZ, maxChunkZ, context, results);
			}
			return results.isEmpty()
					? fallback.structuresInView(seed, dimension, preset, minChunkX, maxChunkX, minChunkZ, maxChunkZ)
					: List.copyOf(results);
		} catch (RuntimeException | Error exception) {
			return fallback.structuresInView(seed, dimension, preset, minChunkX, maxChunkX, minChunkZ, maxChunkZ);
		}
	}

	private int biomeColor(int biomeId) {
		return biomeColorCache.computeIfAbsent(biomeId, id -> {
			if (id < 0) {
				return 0xFF475569;
			}
			String name = Cubiomes.biome2str(VERSION, id).getString(0);
			String biomeIdString = name == null || name.isBlank() ? "" : "minecraft:" + name;
			if (BiomePalette.hasExplicitColor(biomeIdString)) {
				return BiomePalette.color(biomeIdString);
			}
			if (id < CUBIOMES_BIOME_COLORS.length && CUBIOMES_BIOME_COLORS[id] != 0) {
				return CUBIOMES_BIOME_COLORS[id];
			}
			return biomeIdString.isBlank() ? 0xFF475569 : BiomePalette.color(biomeIdString);
		});
	}

	private static String biomeId(int biomeId) {
		if (biomeId < 0) {
			return "";
		}
		String name = Cubiomes.biome2str(VERSION, biomeId).getString(0);
		return name == null || name.isBlank() ? "" : "minecraft:" + name;
	}

	private record BiomeBatch(int[] ids, int width, int stride) {
		int biomeId(int localX, int localZ) {
			return ids[localZ * stride * width + localX * stride];
		}
	}

	private static int[] loadCubiomesBiomeColors() {
		int[] colors = new int[256];
		try (Arena temp = Arena.ofConfined()) {
			MemoryLayout rgbLayout = MemoryLayout.sequenceLayout(3, ValueLayout.JAVA_BYTE);
			MemorySegment biomeColors = temp.allocate(rgbLayout, colors.length);
			Cubiomes.initBiomeColors(biomeColors);
			for (int biome = 0; biome < colors.length; biome++) {
				long offset = biome * rgbLayout.byteSize();
				int red = biomeColors.get(ValueLayout.JAVA_BYTE, offset) & 0xFF;
				int green = biomeColors.get(ValueLayout.JAVA_BYTE, offset + 1) & 0xFF;
				int blue = biomeColors.get(ValueLayout.JAVA_BYTE, offset + 2) & 0xFF;
				colors[biome] = 0xFF000000 | red << 16 | green << 8 | blue;
			}
		} catch (RuntimeException | Error ignored) {
			Arrays.fill(colors, 0);
		}
		return colors;
	}

	@Override
	public List<FinderResult> strongholds(long seed, BlockPos origin) {
		return fallback.strongholds(seed, origin);
	}

	@Override
	public List<FinderResult> oreVeinsInView(long seed, WorldDimension dimension, boolean copper, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
		if (dimension != WorldDimension.OVERWORLD) {
			return List.of();
		}
		long chunkCount = (long)(maxChunkX - minChunkX + 1) * (long)(maxChunkZ - minChunkZ + 1);
		if (chunkCount > MAX_ORE_VEIN_SCAN_CHUNKS) {
			return List.of();
		}
		try {
			Context context = context(seed, dimension);
			List<FinderResult> results = new ArrayList<>();
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
					FinderResult result = oreVeinInChunk(context, copper, chunkX, chunkZ);
					if (result != null) {
						results.add(result);
					}
				}
			}
			return List.copyOf(results);
		} catch (RuntimeException | Error exception) {
			return List.of();
		}
	}

	@Override
	public boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
		return fallback.isSlimeChunk(seed, chunkX, chunkZ);
	}

	private synchronized Context context(long seed, WorldDimension dimension) {
		ContextKey key = new ContextKey(seed, dimension);
		Context cached = contexts.get(key);
		if (cached != null) {
			return cached;
		}
		MemorySegment biomeGenerator = Generator.allocate(arena);
		int cubiomesDimension = cubiomesDimension(dimension);
		Cubiomes.setupGenerator(biomeGenerator, VERSION, GENERATOR_FLAGS);
		Cubiomes.applySeed(biomeGenerator, cubiomesDimension, seed);

		MemorySegment structureGenerator = Generator.allocate(arena);
		structureGenerator.copyFrom(biomeGenerator);

		MemorySegment surfaceNoise = SurfaceNoise.allocate(arena);
		Cubiomes.initSurfaceNoise(surfaceNoise, cubiomesDimension, seed);

		MemorySegment endNoise = EndNoise.allocate(arena);
		if (dimension == WorldDimension.END) {
			Cubiomes.setEndSeed(endNoise, VERSION, seed);
		}

		MemorySegment oreVeinParameters = OreVeinParameters.allocate(arena);
		Cubiomes.initOreVeinNoise(oreVeinParameters, seed, VERSION);

		Context created = new Context(biomeGenerator, structureGenerator, surfaceNoise, endNoise, oreVeinParameters);
		contexts.put(key, created);
		while (contexts.size() > MAX_CONTEXT_CACHE) {
			ContextKey eldest = contexts.keySet().iterator().next();
			contexts.remove(eldest);
		}
		return created;
	}

	private void addVisibleStructures(long seed, String label, int structureId, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, Context context, List<FinderResult> results) {
		int regionSize = regionSize(structureId);
		if (regionSize <= 0) {
			return;
		}
		int minRegionX = Math.floorDiv(minChunkX, regionSize);
		int maxRegionX = Math.floorDiv(maxChunkX, regionSize);
		int minRegionZ = Math.floorDiv(minChunkZ, regionSize);
		int maxRegionZ = Math.floorDiv(maxChunkZ, regionSize);
		try (Arena temp = Arena.ofConfined()) {
			MemorySegment structurePos = Pos.allocate(temp);
			for (int regionX = minRegionX; regionX <= maxRegionX; regionX++) {
				for (int regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
					if (Cubiomes.getStructurePos(structureId, VERSION, seed, regionX, regionZ, structurePos) == 0) {
						continue;
					}
					int x = Pos.x(structurePos);
					int z = Pos.z(structurePos);
					int chunkX = Math.floorDiv(x, 16);
					int chunkZ = Math.floorDiv(z, 16);
					if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ || !isViableStructure(structureId, context, x, z)) {
						continue;
					}
					String note = "seed-based cubiomes";
					if (structureId == Cubiomes.End_City() && endCityHasShip(seed, x, z, temp)) {
						note += "; end_ship";
					}
					if (structureId == Cubiomes.Village()) {
						boolean abandoned = isAbandonedVillage(seed, structureId, context, x, z, temp);
						if (!"Abandoned Village".equals(label) && abandoned) {
							note += "; abandoned";
						}
						if ("Abandoned Village".equals(label) && !abandoned) {
							continue;
						}
					}
					if (structureId == Cubiomes.Bastion()) {
						int bastionVariant = bastionVariant(seed, structureId, context, x, z, temp);
						if (!matchesBastionVariant(label, bastionVariant)) {
							continue;
						}
						note += "; " + bastionVariantName(bastionVariant);
					}
					if (structureId == Cubiomes.Fortress()) {
						List<FinderResult> blazeSpawners = fortressBlazeSpawners(seed, x, z, temp);
						if ("Fortress Blaze Spawner".equals(label)) {
							results.addAll(blazeSpawners);
							continue;
						}
						if (!blazeSpawners.isEmpty()) {
							note += "; blaze_spawners " + blazeSpawners.size();
						}
					}
					results.add(new FinderResult(label, x, 64, z, Math.sqrt((double)x * x + (double)z * z), note));
				}
			}
		}
	}

	private FinderResult oreVeinInChunk(Context context, boolean copper, int chunkX, int chunkZ) {
		for (int offsetX = 2; offsetX < 16; offsetX += 5) {
			for (int offsetZ = 2; offsetZ < 16; offsetZ += 5) {
				int x = chunkX * 16 + offsetX;
				int z = chunkZ * 16 + offsetZ;
				for (int y = -60; y <= 50; y += 4) {
					int block = Cubiomes.getOreVeinBlockAt(x, y, z, context.oreVeinParameters());
					if (isCopperVeinBlock(block) == copper && isOreVeinBlock(block)) {
						String label = copper ? "Copper Ore Vein" : "Iron Ore Vein";
						String note = copper ? "seed-based ore vein; copper" : "seed-based ore vein; iron";
						return new FinderResult(label, x, y, z, Math.sqrt((double)x * x + (double)z * z), note);
					}
				}
			}
		}
		return null;
	}

	private static boolean isOreVeinBlock(int block) {
		return block == Cubiomes.RAW_COPPER_BLOCK()
				|| block == Cubiomes.COPPER_ORE()
				|| block == Cubiomes.GRANITE()
				|| block == Cubiomes.RAW_IRON_BLOCK()
				|| block == Cubiomes.IRON_ORE()
				|| block == Cubiomes.TUFF();
	}

	private static boolean isCopperVeinBlock(int block) {
		return block == Cubiomes.RAW_COPPER_BLOCK()
				|| block == Cubiomes.COPPER_ORE()
				|| block == Cubiomes.GRANITE();
	}

	private boolean isAbandonedVillage(long seed, int structureId, Context context, int x, int z, Arena temp) {
		int biome = Cubiomes.getBiomeAt(context.biomeGenerator(), 4, Math.floorDiv(x, 4), Math.floorDiv(320, 4), Math.floorDiv(z, 4));
		if (biome < 0) {
			biome = Cubiomes.getBiomeAt(context.biomeGenerator(), 4, Math.floorDiv(x, 4), Math.floorDiv(64, 4), Math.floorDiv(z, 4));
		}
		MemorySegment variant = StructureVariant.allocate(temp);
		if (Cubiomes.getVariant(variant, structureId, VERSION, seed, x, z, biome) == 0) {
			return false;
		}
		return StructureVariant.abandoned(variant) == 1;
	}

	private int bastionVariant(long seed, int structureId, Context context, int x, int z, Arena temp) {
		int biome = Cubiomes.getBiomeAt(context.biomeGenerator(), 4, Math.floorDiv(x, 4), Math.floorDiv(64, 4), Math.floorDiv(z, 4));
		MemorySegment variant = StructureVariant.allocate(temp);
		if (Cubiomes.getVariant(variant, structureId, VERSION, seed, x, z, biome) == 0) {
			return -1;
		}
		return StructureVariant.start(variant);
	}

	private static boolean matchesBastionVariant(String label, int variant) {
		return switch (label) {
			case "Bastion Housing Units" -> variant == 0;
			case "Bastion Hoglin Stable" -> variant == 1;
			case "Bastion Treasure Room" -> variant == 2;
			case "Bastion Bridge" -> variant == 3;
			default -> true;
		};
	}

	private static String bastionVariantName(int variant) {
		return switch (variant) {
			case 0 -> "housing_units";
			case 1 -> "hoglin_stable";
			case 2 -> "treasure_room";
			case 3 -> "bridge";
			default -> "unknown_bastion_variant";
		};
	}

	private boolean isViableStructure(int structureId, Context context, int x, int z) {
		if (Cubiomes.isViableStructurePos(structureId, context.structureGenerator(), x, z, 0) == 0) {
			return false;
		}
		if (Cubiomes.isViableStructureTerrain(structureId, context.structureGenerator(), x, z) == 0) {
			return false;
		}
		return structureId != Cubiomes.End_City() || Cubiomes.isViableEndCityTerrain(context.structureGenerator(), context.surfaceNoise(), x, z) != 0;
	}

	private boolean endCityHasShip(long seed, int x, int z, Arena temp) {
		MemorySegment pieces = Piece.allocateArray(MAX_END_CITY_PIECES, temp);
		int numPieces = Cubiomes.getEndCityPieces(pieces, seed, x >> 4, z >> 4);
		for (int i = 0; i < numPieces; i++) {
			if (Piece.type(Piece.asSlice(pieces, i)) == Cubiomes.END_SHIP()) {
				return true;
			}
		}
		return false;
	}

	private List<FinderResult> fortressBlazeSpawners(long seed, int x, int z, Arena temp) {
		MemorySegment pieces = Piece.allocateArray(MAX_END_CITY_PIECES, temp);
		int numPieces = Cubiomes.getFortressPieces(pieces, MAX_END_CITY_PIECES, VERSION, seed, x >> 4, z >> 4);
		if (numPieces <= 0) {
			return List.of();
		}
		List<FinderResult> spawners = new ArrayList<>();
		for (int i = 0; i < numPieces; i++) {
			MemorySegment piece = Piece.asSlice(pieces, i);
			if (Piece.type(piece) != Cubiomes.BRIDGE_SPAWNER()) {
				continue;
			}
			MemorySegment position = Piece.pos(piece);
			int spawnerX = Pos3.x(position);
			int spawnerY = Pos3.y(position) + 10;
			int spawnerZ = Pos3.z(position);
			spawners.add(new FinderResult("Fortress Blaze Spawner", spawnerX, spawnerY, spawnerZ, Math.sqrt((double)spawnerX * spawnerX + (double)spawnerZ * spawnerZ), "seed-based cubiomes; fortress blaze_spawner"));
		}
		return List.copyOf(spawners);
	}

	private int regionSize(int structureId) {
		try (Arena temp = Arena.ofConfined()) {
			MemorySegment config = StructureConfig.allocate(temp);
			if (Cubiomes.getStructureConfig(structureId, VERSION, config) == 0) {
				return -1;
			}
			return Byte.toUnsignedInt(StructureConfig.regionSize(config));
		}
	}

	private static int[] cubiomesStructureIds(StructurePreset preset) {
		return switch (preset.label()) {
			case "Village" -> new int[] { Cubiomes.Village() };
			case "Abandoned Village" -> new int[] { Cubiomes.Village() };
			case "Ancient City" -> new int[] { Cubiomes.Ancient_City() };
			case "Trial Chambers" -> new int[] { Cubiomes.Trial_Chambers() };
			case "Desert Pyramid" -> new int[] { Cubiomes.Desert_Pyramid() };
			case "Jungle Temple" -> new int[] { Cubiomes.Jungle_Pyramid() };
			case "Swamp Hut" -> new int[] { Cubiomes.Swamp_Hut() };
			case "Igloo" -> new int[] { Cubiomes.Igloo() };
			case "Ocean Monument" -> new int[] { Cubiomes.Monument() };
			case "Ocean Ruins" -> new int[] { Cubiomes.Ocean_Ruin() };
			case "Shipwreck" -> new int[] { Cubiomes.Shipwreck() };
			case "Buried Treasure" -> new int[] { Cubiomes.Treasure() };
			case "Mineshaft" -> new int[] { Cubiomes.Mineshaft() };
			case "Pillager Outpost" -> new int[] { Cubiomes.Outpost() };
			case "Woodland Mansion" -> new int[] { Cubiomes.Mansion() };
			case "Ruined Portal" -> preset.dimension() == WorldDimension.NETHER ? new int[] { Cubiomes.Ruined_Portal_N() } : new int[] { Cubiomes.Ruined_Portal() };
			case "Trail Ruins" -> new int[] { Cubiomes.Trail_Ruins() };
			case "Geode" -> new int[] { Cubiomes.Geode() };
			case "Nether Fortress", "Fortress Blaze Spawner" -> new int[] { Cubiomes.Fortress() };
			case "Bastion Housing Units", "Bastion Hoglin Stable", "Bastion Treasure Room", "Bastion Bridge" -> new int[] { Cubiomes.Bastion() };
			case "End City" -> new int[] { Cubiomes.End_City() };
			case "End Gateway" -> new int[] { Cubiomes.End_Gateway() };
			default -> new int[0];
		};
	}

	private static int cubiomesDimension(WorldDimension dimension) {
		return switch (dimension) {
			case OVERWORLD -> Cubiomes.DIM_OVERWORLD();
			case NETHER -> Cubiomes.DIM_NETHER();
			case END -> Cubiomes.DIM_END();
		};
	}

	private record ContextKey(long seed, WorldDimension dimension) {
	}

	private record Context(MemorySegment biomeGenerator, MemorySegment structureGenerator, MemorySegment surfaceNoise, MemorySegment endNoise, MemorySegment oreVeinParameters) {
	}
}
