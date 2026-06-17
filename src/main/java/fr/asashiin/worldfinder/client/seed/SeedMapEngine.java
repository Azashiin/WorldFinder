package fr.asashiin.worldfinder.client.seed;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import fr.asashiin.worldfinder.client.finder.FinderResult;
import fr.asashiin.worldfinder.client.finder.StructurePreset;
import fr.asashiin.worldfinder.client.finder.WorldDimension;
import fr.asashiin.worldfinder.client.screen.BiomePalette;
import net.minecraft.core.BlockPos;

public interface SeedMapEngine {
	String name();

	Optional<String> biomeIdAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ);

	default String[] biomeIdsAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ, int sampleStep, int width, int height) {
		String[] biomes = new String[width * height];
		for (int localZ = 0; localZ < height; localZ++) {
			for (int localX = 0; localX < width; localX++) {
				int sampleX = blockX + localX * sampleStep;
				int sampleZ = blockZ + localZ * sampleStep;
				biomes[localZ * width + localX] = biomeIdAt(seed, dimension, sampleX, blockY, sampleZ).orElse("");
			}
		}
		return biomes;
	}

	default OptionalInt surfaceYAt(long seed, WorldDimension dimension, int blockX, int blockZ) {
		return OptionalInt.empty();
	}

	default int[] biomeColorsAt(long seed, WorldDimension dimension, int blockX, int blockY, int blockZ, int sampleStep, int width, int height) {
		int[] colors = new int[width * height];
		for (int localZ = 0; localZ < height; localZ++) {
			for (int localX = 0; localX < width; localX++) {
				int sampleX = blockX + localX * sampleStep;
				int sampleZ = blockZ + localZ * sampleStep;
				colors[localZ * width + localX] = biomeIdAt(seed, dimension, sampleX, blockY, sampleZ)
						.map(BiomePalette::color)
						.orElse(0xFF475569);
			}
		}
		return colors;
	}

	default int[] endTerrainColorsAt(long seed, int blockX, int blockZ, int sampleStep, int width, int height) {
		return biomeColorsAt(seed, WorldDimension.END, blockX, 64, blockZ, sampleStep, width, height);
	}

	List<FinderResult> structuresInView(long seed, WorldDimension dimension, StructurePreset preset, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ);

	List<FinderResult> strongholds(long seed, BlockPos origin);

	default List<FinderResult> oreVeinsInView(long seed, WorldDimension dimension, boolean copper, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
		return List.of();
	}

	boolean isSlimeChunk(long seed, int chunkX, int chunkZ);
}
