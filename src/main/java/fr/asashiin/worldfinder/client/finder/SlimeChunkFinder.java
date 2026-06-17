package fr.asashiin.worldfinder.client.finder;

import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class SlimeChunkFinder {
	private SlimeChunkFinder() {
	}

	public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
		return WorldgenRandom.seedSlimeChunk(chunkX, chunkZ, seed, 987234911L).nextInt(10) == 0;
	}
}
