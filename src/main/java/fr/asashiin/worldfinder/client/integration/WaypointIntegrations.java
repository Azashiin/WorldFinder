package fr.asashiin.worldfinder.client.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import fr.asashiin.worldfinder.client.finder.WorldDimension;
import fr.asashiin.worldfinder.client.integration.JourneyMapWaypoints;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public final class WaypointIntegrations {
	private WaypointIntegrations() {
	}

	public enum Target {
		JOURNEYMAP("JourneyMap"),
		XAERO("Xaero's Map"),
		FTB_CHUNKS("FTB Chunks");

		private final String label;

		Target(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	public static List<Target> availableTargets() {
		return detectAvailableTargets(null, null);
	}

	public static List<Target> availableTargets(WorldDimension dimension, ResourceKey<Level> currentDimension) {
		return detectAvailableTargets(dimension, currentDimension);
	}

	public static List<String> diagnostics() {
		List<String> lines = new ArrayList<>();
		lines.add("JourneyMap: " + yesNo(JourneyMapWaypoints.isAvailable()) + (isModLoaded("journeymap") && !JourneyMapWaypoints.isAvailable() ? " (installed; API not supported)" : ""));
		lines.add("Xaero waypoints: " + yesNo(isXaeroWaypointAvailable()) + (isXaeroWorldMapOnly() ? " (World Map only; Minimap/Better PVP required)" : ""));
		lines.add("FTB Chunks: " + yesNo(isFtbChunksWaypointAvailable()) + (isModLoaded("ftbchunks") && !isFtbChunksWaypointAvailable() ? " (installed; API not supported)" : ""));
		return lines;
	}

	private static List<Target> detectAvailableTargets(WorldDimension dimension, ResourceKey<Level> currentDimension) {
		List<Target> targets = new ArrayList<>(3);
		if (JourneyMapWaypoints.isAvailable()) {
			targets.add(Target.JOURNEYMAP);
		}
		if (isXaeroWaypointAvailable() && canXaeroUseDimension(dimension, currentDimension)) {
			targets.add(Target.XAERO);
		}
		if (isFtbChunksWaypointAvailable()) {
			targets.add(Target.FTB_CHUNKS);
		}
		return List.copyOf(targets);
	}

	public static boolean add(Target target, String name, BlockPos pos, WorldDimension dimension, ResourceKey<Level> currentDimension, int color) {
		return switch (target) {
			case JOURNEYMAP -> JourneyMapWaypoints.add(name, pos, dimension.configName(), color);
			case XAERO -> canXaeroUseDimension(dimension, currentDimension) && addXaero(name, pos);
			case FTB_CHUNKS -> addFtbChunks(name, pos, dimension.levelKey(), color);
		};
	}

	private static boolean canXaeroUseDimension(WorldDimension dimension, ResourceKey<Level> currentDimension) {
		return dimension == null || currentDimension != null && dimension.levelKey().equals(currentDimension);
	}

	private static boolean addXaero(String name, BlockPos pos) {
		try {
			Class<?> modulesClass = Class.forName("xaero.hud.minimap.BuiltInHudModules");
			Object minimapModule = modulesClass.getField("MINIMAP").get(null);
			Method getCurrentSession = minimapModule.getClass().getMethod("getCurrentSession");
			Object session = getCurrentSession.invoke(minimapModule);
			if (session == null) {
				return false;
			}

			Object worldManager = session.getClass().getMethod("getWorldManager").invoke(session);
			Object world = worldManager.getClass().getMethod("getCurrentWorld").invoke(worldManager);
			if (world == null) {
				return false;
			}

			Object waypointSet = world.getClass().getMethod("getCurrentWaypointSet").invoke(world);
			Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
			Object waypoint = waypointClass
					.getConstructor(int.class, int.class, int.class, String.class, String.class, int.class)
					.newInstance(pos.getX(), pos.getY(), pos.getZ(), name, initials(name), 11);
			waypointClass.getMethod("setTemporary", boolean.class).invoke(waypoint, true);
			waypointClass.getMethod("setYIncluded", boolean.class).invoke(waypoint, true);
			waypointSet.getClass().getMethod("add", waypointClass, boolean.class).invoke(waypointSet, waypoint, true);

			Object waypointSession = session.getClass().getMethod("getWaypointSession").invoke(session);
			waypointSession.getClass().getMethod("setSetChangedTime", long.class).invoke(waypointSession, System.currentTimeMillis());
			return true;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	private static boolean addFtbChunks(String name, BlockPos pos, ResourceKey<Level> dimension, int color) {
		try {
			Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
			Object clientApi = apiClass.getMethod("clientApi").invoke(null);
			Object optional = clientApi.getClass().getMethod("getWaypointManager", ResourceKey.class).invoke(clientApi, dimension);
			if (!(optional instanceof Optional<?> managers) || managers.isEmpty()) {
				return false;
			}
			Object manager = managers.get();
			Class<?> managerClass = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager");
			Object waypoint = managerClass.getMethod("addTransientWaypointAt", BlockPos.class, String.class).invoke(manager, pos, name);
			if (waypoint == null) {
				return false;
			}
			Class<?> waypointClass = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
			waypointClass.getMethod("setColor", int.class).invoke(waypoint, color);
			return true;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	private static boolean isModLoaded(String modId) {
		try {
			return ModList.get().isLoaded(modId);
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	private static boolean isXaeroWaypointAvailable() {
		return classExists("xaero.hud.minimap.BuiltInHudModules")
				&& classExists("xaero.common.minimap.waypoints.Waypoint");
	}

	private static boolean isFtbChunksWaypointAvailable() {
		return classExists("dev.ftb.mods.ftbchunks.api.FTBChunksAPI")
				&& classExists("dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager")
				&& classExists("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
	}

	private static boolean isXaeroWorldMapOnly() {
		return isModLoaded("xaeroworldmap") && !isXaeroWaypointAvailable();
	}

	private static boolean classExists(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}

	private static String initials(String name) {
		String cleaned = name == null ? "" : name.replace("WF ", "").trim();
		if (cleaned.isEmpty()) {
			return "W";
		}
		String[] parts = cleaned.split("\\s+");
		StringBuilder builder = new StringBuilder(2);
		for (String part : parts) {
			if (!part.isBlank()) {
				builder.append(Character.toUpperCase(part.charAt(0)));
			}
			if (builder.length() >= 2) {
				break;
			}
		}
		if (builder.isEmpty()) {
			return cleaned.substring(0, Math.min(2, cleaned.length())).toUpperCase(Locale.ROOT);
		}
		return builder.toString();
	}

	private static String yesNo(boolean value) {
		return value ? "yes" : "no";
	}
}
