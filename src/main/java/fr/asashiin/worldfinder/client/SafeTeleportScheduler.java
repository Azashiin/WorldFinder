package fr.asashiin.worldfinder.client;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class SafeTeleportScheduler {
	private static final Queue<ScheduledScan> SCANS = new ArrayDeque<>();
	private static int tick;
	private static int suppressTeleportFeedbackUntilTick;

	private SafeTeleportScheduler() {
	}

	public static void schedule(String targetDimension, int blockX, int blockZ, int minY, int maxY, boolean nearby) {
		int bottom = Math.max(-64, Math.min(minY, maxY));
		int top = Math.min(384, Math.max(minY, maxY));
		suppressTeleportFeedback();
		SCANS.add(new ScheduledScan(tick + 6, targetDimension, blockX, blockZ, bottom, top, nearby));
		SCANS.add(new ScheduledScan(tick + 16, targetDimension, blockX, blockZ, bottom, top, nearby));
		SCANS.add(new ScheduledScan(tick + 32, targetDimension, blockX, blockZ, bottom, top, nearby));
	}

	public static void suppressTeleportFeedback() {
		suppressTeleportFeedbackUntilTick = Math.max(suppressTeleportFeedbackUntilTick, tick + 100);
	}

	public static boolean shouldSuppressTeleportFeedback(Component message) {
		if (tick > suppressTeleportFeedbackUntilTick || message == null) {
			return false;
		}
		if (message.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith("commands.teleport.")) {
			return true;
		}
		String text = message.getString().toLowerCase(Locale.ROOT);
		return text.contains("teleport") || text.contains("téléport");
	}

	public static void tick(Minecraft minecraft) {
		tick++;
		while (!SCANS.isEmpty() && SCANS.peek().runAtTick() <= tick) {
			sendScan(minecraft, SCANS.remove());
		}
	}

	private static void sendScan(Minecraft minecraft, ScheduledScan scan) {
		if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) {
			return;
		}
		suppressTeleportFeedback();
		int[][] offsets = scan.nearby()
				? new int[][] { { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, { 0, 0 } }
				: new int[][] { { 0, 0 } };
		for (int[] offset : offsets) {
			String x = centeredCoordinate(scan.blockX() + offset[0]);
			String z = centeredCoordinate(scan.blockZ() + offset[1]);
			for (int y = scan.minY(); y <= scan.maxY(); y++) {
				String positioned = "execute in " + scan.targetDimension() + " positioned " + x + " " + y + " " + z + " ";
				minecraft.player.connection.sendCommand(positioned
						+ "if block ~ ~ ~ minecraft:air"
						+ " if block ~ ~1 ~ minecraft:air"
						+ " unless block ~ ~-1 ~ minecraft:air"
						+ " unless block ~ ~-1 ~ minecraft:cave_air"
						+ " unless block ~ ~-1 ~ minecraft:void_air"
						+ " unless block ~ ~-1 ~ minecraft:water"
						+ " unless block ~ ~-1 ~ minecraft:lava"
						+ " unless block ~ ~-1 ~ minecraft:bedrock"
						+ " unless block ~ ~-1 ~ minecraft:powder_snow"
						+ " unless block ~ ~-1 ~ minecraft:fire"
						+ " unless block ~ ~-1 ~ minecraft:soul_fire"
						+ " unless block ~ ~-1 ~ minecraft:cactus"
						+ " unless block ~ ~-1 ~ minecraft:magma_block"
						+ " unless block ~ ~-1 ~ minecraft:campfire"
						+ " unless block ~ ~-1 ~ minecraft:soul_campfire"
						+ " unless block ~ ~-1 ~ minecraft:pointed_dripstone"
						+ " run tp @s ~ ~ ~");
			}
		}
	}

	private static String centeredCoordinate(int blockCoordinate) {
		return Double.toString(blockCoordinate + 0.5D);
	}

	private record ScheduledScan(int runAtTick, String targetDimension, int blockX, int blockZ, int minY, int maxY, boolean nearby) {
	}
}
