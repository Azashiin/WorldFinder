package fr.asashiin.worldfinder.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.asashiin.worldfinder.WorldFinderConfig;
import fr.asashiin.worldfinder.client.config.WorldFinderProfileConfig;
import fr.asashiin.worldfinder.client.integration.WaypointIntegrations;
import fr.asashiin.worldfinder.client.screen.WorldFinderMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class WorldFinderClientCommands {
	private WorldFinderClientCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("wf")
				.then(Commands.literal("open")
						.executes(context -> openMap(context.getSource())))
				.then(Commands.literal("reload")
						.executes(context -> reload(context.getSource())))
				.then(Commands.literal("clearcache")
						.executes(context -> clearCache(context.getSource())))
				.then(Commands.literal("profile")
						.executes(context -> profile(context.getSource())))
				.then(Commands.literal("integrations")
						.executes(context -> integrations(context.getSource())))
				.then(Commands.literal("filters")
						.then(Commands.literal("defaults")
								.executes(context -> resetDefaultFilters(context.getSource()))))
				.then(Commands.literal("seed")
						.then(Commands.literal("get")
								.executes(context -> getSeed(context.getSource())))
						.then(Commands.literal("clear")
								.executes(context -> clearSeed(context.getSource())))
						.then(Commands.literal("set")
								.then(Commands.argument("seed", StringArgumentType.word())
										.executes(context -> setSeed(context.getSource(), StringArgumentType.getString(context, "seed")))))));
	}

	private static int openMap(CommandSourceStack source) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.setScreen(new WorldFinderMapScreen(minecraft.screen)));
		success(source, "World Finder map opened.");
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof WorldFinderMapScreen screen) {
			screen.reloadFromCommand();
		} else {
			WorldFinderMapScreen.clearGlobalCaches();
		}
		success(source, "World Finder profile and map caches reloaded.");
		return 1;
	}

	private static int clearCache(CommandSourceStack source) {
		WorldFinderMapScreen.clearGlobalCaches();
		success(source, "World Finder map caches cleared.");
		return 1;
	}

	private static int profile(CommandSourceStack source) {
		WorldFinderProfileConfig profile = WorldFinderProfileConfig.resolve(Minecraft.getInstance());
		success(source, "World Finder profile: " + profile.file());
		return 1;
	}

	private static int integrations(CommandSourceStack source) {
		for (String line : WaypointIntegrations.diagnostics()) {
			success(source, line);
		}
		return 1;
	}

	private static int getSeed(CommandSourceStack source) {
		WorldFinderProfileConfig profile = WorldFinderProfileConfig.resolve(Minecraft.getInstance());
		String seed = profile.seed();
		success(source, seed.isBlank() ? "World Finder seed is empty for this profile." : "World Finder seed: " + seed);
		return 1;
	}

	private static int setSeed(CommandSourceStack source, String seed) {
		WorldFinderProfileConfig profile = WorldFinderProfileConfig.resolve(Minecraft.getInstance());
		profile.seed(seed);
		profile.save();
		WorldFinderMapScreen.clearGlobalCaches();
		success(source, "World Finder seed saved for this profile: " + seed);
		return 1;
	}

	private static int clearSeed(CommandSourceStack source) {
		WorldFinderProfileConfig profile = WorldFinderProfileConfig.resolve(Minecraft.getInstance());
		profile.seed("");
		profile.save();
		WorldFinderMapScreen.clearGlobalCaches();
		success(source, "World Finder seed cleared for this profile.");
		return 1;
	}

	private static int resetDefaultFilters(CommandSourceStack source) {
		WorldFinderProfileConfig profile = WorldFinderProfileConfig.resolve(Minecraft.getInstance());
		profile.enabledStructureFilters(WorldFinderConfig.DEFAULT_ENABLED_STRUCTURE_FILTERS);
		profile.save();
		WorldFinderMapScreen.clearGlobalCaches();
		success(source, "World Finder structure filters reset to early-game defaults.");
		return 1;
	}

	private static void success(CommandSourceStack source, String message) {
		source.sendSuccess(() -> Component.literal(message), false);
	}
}
