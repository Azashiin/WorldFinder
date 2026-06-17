package fr.asashiin.worldfinder.client.finder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import fr.asashiin.worldfinder.client.finder.WorldDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

public final class OfflineVanillaWorldgen {
	private static final HolderLookup.Provider VANILLA_REGISTRIES = VanillaRegistries.createLookup();
	private static final RegistryAccess VANILLA_REGISTRY_ACCESS = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
	private static final int MAX_CONTEXT_CACHE = 8;
	private static final Map<ContextKey, Context> CONTEXT_CACHE = new LinkedHashMap<>(MAX_CONTEXT_CACHE, 0.75F, true);

	private OfflineVanillaWorldgen() {
	}

	public static List<FinderResult> findVisibleStructures(long seed, WorldDimension dimension, StructurePreset preset, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
		Context context = context(seed, dimension);
		List<Holder<Structure>> structures = context.structures(preset);
		if (structures.isEmpty()) {
			return List.of();
		}

		List<FinderResult> results = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		for (Holder<Structure> structure : structures) {
			for (StructurePlacement placement : context.structureState().getPlacementsForStructure(structure)) {
				if (placement instanceof RandomSpreadStructurePlacement spread) {
					addVisibleRandomSpread(context, structure, preset.label(), minChunkX, maxChunkX, minChunkZ, maxChunkZ, spread, seen, results);
				} else if (placement instanceof ConcentricRingsStructurePlacement rings) {
					addVisibleRings(context, structure, preset.label(), minChunkX, maxChunkX, minChunkZ, maxChunkZ, rings, seen, results);
				}
			}
		}
		return List.copyOf(results);
	}

	public static List<FinderResult> strongholds(long seed, int maxResults, BlockPos origin) {
		try {
			Context context = context(seed, WorldDimension.OVERWORLD);
			Identifier id = Identifier.withDefaultNamespace("stronghold");
			Optional<Holder.Reference<Structure>> stronghold = context.registries.lookupOrThrow(Registries.STRUCTURE)
					.get(ResourceKey.create(Registries.STRUCTURE, id));
			List<FinderResult> results = new ArrayList<>();
			if (stronghold.isPresent()) {
				for (StructurePlacement placement : context.structureState().getPlacementsForStructure(stronghold.get())) {
					if (!(placement instanceof ConcentricRingsStructurePlacement rings)) {
						continue;
					}
					List<ChunkPos> chunks = context.structureState().getRingPositionsFor(rings);
					if (chunks == null) {
						continue;
					}
					for (ChunkPos chunk : chunks) {
						int x = chunk.getMiddleBlockX();
						int z = chunk.getMiddleBlockZ();
						results.add(new FinderResult("Stronghold", x, context.surfaceY(x, z), z, horizontalDistance(origin.getX(), origin.getZ(), x, z), "seed-based ring"));
					}
				}
			}
			if (results.isEmpty()) {
				results.addAll(approximateStrongholdRings(seed, maxResults, origin, context));
			}
			results.sort(Comparator.comparingDouble(FinderResult::distance));
			return List.copyOf(results.subList(0, Math.min(maxResults, results.size())));
		} catch (RuntimeException exception) {
			List<FinderResult> results = approximateStrongholdRings(seed, maxResults, origin, null);
			results.sort(Comparator.comparingDouble(FinderResult::distance));
			return List.copyOf(results.subList(0, Math.min(maxResults, results.size())));
		}
	}

	public static Optional<String> biomeIdAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ) {
		return context(seed, dimension).biomeAt(blockX, blockY, blockZ)
				.unwrapKey()
				.map(ResourceKey::identifier)
				.map(Identifier::toString);
	}

	private static List<FinderResult> approximateStrongholdRings(long seed, int maxResults, BlockPos origin, Context context) {
		int distance = 32;
		int count = 128;
		int spread = 3;
		List<FinderResult> results = new ArrayList<>();
		RandomSource random = RandomSource.create();
		random.setSeed(seed);
		double angle = random.nextDouble() * Math.PI * 2.0D;
		int placedInRing = 0;
		int ring = 0;
		for (int index = 0; index < count; index++) {
			double ringDistance = 4 * distance + distance * ring * 6 + (random.nextDouble() - 0.5D) * distance * 2.5D;
			int chunkX = (int)Math.round(Math.cos(angle) * ringDistance);
			int chunkZ = (int)Math.round(Math.sin(angle) * ringDistance);
			int x = chunkX * 16 + 8;
			int z = chunkZ * 16 + 8;
			int y = context == null ? 64 : context.surfaceY(x, z);
			results.add(new FinderResult("Stronghold", x, y, z, horizontalDistance(origin.getX(), origin.getZ(), x, z), "seed-based ring fallback"));
			angle += Math.PI * 2.0D / spread;
			placedInRing++;
			if (placedInRing == spread) {
				ring++;
				placedInRing = 0;
				spread += 2 * spread / (ring + 1);
				spread = Math.min(spread, count - index);
				angle += random.nextDouble() * Math.PI * 2.0D;
			}
			if (results.size() >= Math.max(maxResults, count)) {
				break;
			}
		}
		return results;
	}

	public static OptionalInt surfaceYAt(long seed, WorldDimension dimension, int blockX, int blockZ) {
		try {
			return OptionalInt.of(context(seed, dimension).surfaceY(blockX, blockZ));
		} catch (RuntimeException exception) {
			return OptionalInt.empty();
		}
	}

	private static synchronized Context context(long seed, WorldDimension dimension) {
		ContextKey key = new ContextKey(seed, dimension);
		Context cached = CONTEXT_CACHE.get(key);
		if (cached != null) {
			return cached;
		}

		Context created = Context.create(seed, dimension);
		CONTEXT_CACHE.put(key, created);
		if (CONTEXT_CACHE.size() > MAX_CONTEXT_CACHE) {
			ContextKey eldest = CONTEXT_CACHE.keySet().iterator().next();
			CONTEXT_CACHE.remove(eldest);
		}
		return created;
	}

	private static void addVisibleRandomSpread(Context context, Holder<Structure> structure, String label, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, RandomSpreadStructurePlacement placement, Set<Long> seen, List<FinderResult> results) {
		int spacing = placement.spacing();
		int minGridX = Math.floorDiv(minChunkX, spacing);
		int maxGridX = Math.floorDiv(maxChunkX, spacing);
		int minGridZ = Math.floorDiv(minChunkZ, spacing);
		int maxGridZ = Math.floorDiv(maxChunkZ, spacing);
		BlockPos origin = BlockPos.ZERO;

		for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
			for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
				ChunkPos chunk = placement.getPotentialStructureChunk(context.seed, gridX * spacing, gridZ * spacing);
				if (chunk.x() >= minChunkX && chunk.x() <= maxChunkX && chunk.z() >= minChunkZ && chunk.z() <= maxChunkZ) {
					addStructureResult(context, structure, label, placement, chunk, origin, seen, results);
				}
			}
		}
	}

	private static void addVisibleRings(Context context, Holder<Structure> structure, String label, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ, ConcentricRingsStructurePlacement rings, Set<Long> seen, List<FinderResult> results) {
		List<ChunkPos> chunks = context.structureState().getRingPositionsFor(rings);
		if (chunks == null) {
			return;
		}
		BlockPos origin = BlockPos.ZERO;
		for (ChunkPos chunk : chunks) {
			if (chunk.x() >= minChunkX && chunk.x() <= maxChunkX && chunk.z() >= minChunkZ && chunk.z() <= maxChunkZ) {
				addStructureResult(context, structure, label, rings, chunk, origin, seen, results);
			}
		}
	}

	private static void addStructureResult(Context context, Holder<Structure> structure, String label, StructurePlacement placement, ChunkPos chunk, BlockPos origin, Set<Long> seen, List<FinderResult> results) {
		long key = ChunkPos.pack(chunk.x(), chunk.z());
		if (!seen.add(key) || !placement.isStructureChunk(context.structureState(), chunk.x(), chunk.z())) {
			return;
		}

		GenerationPointCheck generationPoint = context.validGenerationPoint(structure.value(), chunk);
		BlockPos locate;
		String note = "seed-based";
		if (generationPoint.position().isPresent()) {
			locate = generationPoint.position().get();
		} else if (!generationPoint.completed() || structure.value() instanceof JigsawStructure) {
			locate = placement.getLocatePos(chunk);
			if (!context.matchesStructureBiome(structure.value(), locate.getX(), locate.getZ())) {
				return;
			}
			note = structure.value() instanceof JigsawStructure ? "seed-based jigsaw" : "seed-based biome fallback";
		} else {
			return;
		}
		int x = locate.getX();
		int z = locate.getZ();

		results.add(new FinderResult(label, x, context.surfaceY(x, z), z, horizontalDistance(origin.getX(), origin.getZ(), x, z), note));
	}

	private static double horizontalDistance(int ax, int az, int bx, int bz) {
		int dx = ax - bx;
		int dz = az - bz;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private record Context(long seed, WorldDimension dimension, HolderLookup.Provider registries, ChunkGenerator generator, RandomState randomState, ChunkGeneratorStructureState state, LevelHeightAccessor heightAccessor) {
		private static Context create(long seed, WorldDimension dimension) {
			WorldDimensions dimensions = WorldPresets.createNormalWorldDimensions(VANILLA_REGISTRIES);
			LevelStem stem = dimensions.get(stemKey(dimension)).orElseThrow();
			ChunkGenerator generator = stem.generator();
			RandomState randomState = RandomState.create(VANILLA_REGISTRIES, noiseSettingsKey(dimension), seed);
			DimensionType dimensionType = stem.type().value();
			LevelHeightAccessor heightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());
			return new Context(seed, dimension, VANILLA_REGISTRIES, generator, randomState, null, heightAccessor);
		}

		private ChunkGeneratorStructureState structureState() {
			return this.state != null
					? this.state
					: this.generator.createState(this.registries.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, this.seed);
		}

		private List<Holder<Structure>> structures(StructurePreset preset) {
			HolderLookup.RegistryLookup<Structure> structureLookup = this.registries.lookupOrThrow(Registries.STRUCTURE);
			List<Holder<Structure>> holders = new ArrayList<>();
			for (String rawId : preset.structureIds()) {
				Identifier id = Identifier.tryParse(rawId);
				if (id == null) {
					continue;
				}
				structureLookup.get(ResourceKey.create(Registries.STRUCTURE, id)).ifPresent(holders::add);
			}
			return holders;
		}

		private int surfaceY(int x, int z) {
			return this.generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor, this.randomState);
		}

		private Holder<Biome> biomeAtSurface(int x, int z) {
			return biomeAt(x, surfaceY(x, z), z);
		}

		private Holder<Biome> biomeAt(int x, int y, int z) {
			BiomeSource source = this.generator.getBiomeSource();
			return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), this.randomState.sampler());
		}

		private GenerationPointCheck validGenerationPoint(Structure structure, ChunkPos chunk) {
			try {
				Optional<BlockPos> position = structure.findValidGenerationPoint(new Structure.GenerationContext(
						VANILLA_REGISTRY_ACCESS,
						this.generator,
						this.generator.getBiomeSource(),
						this.randomState,
						null,
						this.seed,
						chunk,
						this.heightAccessor,
						structure.biomes()::contains))
						.map(GenerationStub::position);
				return new GenerationPointCheck(position, true);
			} catch (RuntimeException exception) {
				return new GenerationPointCheck(Optional.empty(), false);
			}
		}

		private boolean matchesStructureBiome(Structure structure, int x, int z) {
			if (structure.biomes().contains(biomeAtSurface(x, z))) {
				return true;
			}
			for (int y = this.heightAccessor.getMinY(); y <= this.heightAccessor.getMaxY(); y += 16) {
				if (structure.biomes().contains(biomeAt(x, y, z))) {
					return true;
				}
			}
			return false;
		}
	}

	private static ResourceKey<LevelStem> stemKey(WorldDimension dimension) {
		return switch (dimension) {
			case OVERWORLD -> LevelStem.OVERWORLD;
			case NETHER -> LevelStem.NETHER;
			case END -> LevelStem.END;
		};
	}

	private static ResourceKey<NoiseGeneratorSettings> noiseSettingsKey(WorldDimension dimension) {
		return switch (dimension) {
			case OVERWORLD -> NoiseGeneratorSettings.OVERWORLD;
			case NETHER -> NoiseGeneratorSettings.NETHER;
			case END -> NoiseGeneratorSettings.END;
		};
	}

	private record ContextKey(long seed, WorldDimension dimension) {
	}

	private record GenerationPointCheck(Optional<BlockPos> position, boolean completed) {
	}
}
