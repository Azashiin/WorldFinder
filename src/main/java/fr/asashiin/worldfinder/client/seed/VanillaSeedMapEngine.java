package fr.asashiin.worldfinder.client.seed;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import fr.asashiin.worldfinder.client.finder.FinderResult;
import fr.asashiin.worldfinder.client.finder.OfflineVanillaWorldgen;
import fr.asashiin.worldfinder.client.finder.SlimeChunkFinder;
import fr.asashiin.worldfinder.client.finder.StructurePreset;
import fr.asashiin.worldfinder.client.finder.WorldDimension;
import net.minecraft.core.BlockPos;

public final class VanillaSeedMapEngine implements SeedMapEngine {
	@Override
	public String name() {
		return "Vanilla";
	}

	@Override
	public Optional<String> biomeIdAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ) {
		return OfflineVanillaWorldgen.biomeIdAt(seed, dimension, blockX, blockY, blockZ);
	}

	@Override
	public OptionalInt surfaceYAt(long seed, WorldDimension dimension, int blockX, int blockZ) {
		return OfflineVanillaWorldgen.surfaceYAt(seed, dimension, blockX, blockZ);
	}

	@Override
	public List<FinderResult> structuresInView(long seed, WorldDimension dimension, StructurePreset preset, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
		try {
			return OfflineVanillaWorldgen.findVisibleStructures(seed, dimension, preset, minChunkX, maxChunkX, minChunkZ, maxChunkZ);
		} catch (RuntimeException exception) {
			return List.of();
		}
	}

	@Override
	public List<FinderResult> strongholds(long seed, BlockPos origin) {
		return OfflineVanillaWorldgen.strongholds(seed, 128, origin);
	}

	@Override
	public boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
		return SlimeChunkFinder.isSlimeChunk(seed, chunkX, chunkZ);
	}
}
