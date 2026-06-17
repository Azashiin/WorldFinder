package fr.asashiin.worldfinder.client.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.core.BlockPos;

public final class JourneyMapWaypoints {
	private static final String MOD_ID = "worldfinder";

	private JourneyMapWaypoints() {
	}

	public static boolean isAvailable() {
		try {
			Class.forName("journeymap.api.client.impl.ClientAPI");
			Class.forName("journeymap.api.client.waypoint.ClientWaypointFactoryImpl");
			Class.forName("journeymap.api.v2.common.waypoint.Waypoint");
			return true;
		} catch (ClassNotFoundException ignored) {
			return false;
		}
	}

	public static boolean add(String name, BlockPos pos, String dimension, int color) {
		try {
			Class<?> factoryClass = Class.forName("journeymap.api.client.waypoint.ClientWaypointFactoryImpl");
			Method createWaypoint = factoryClass.getMethod(
					"createWaypoint",
					String.class,
					BlockPos.class,
					String.class,
					String.class,
					boolean.class,
					boolean.class,
					int.class,
					boolean.class
			);
			Object waypoint = createWaypoint.invoke(null, MOD_ID, pos, name, dimension, false, true, color, true);

			Class<?> waypointClass = Class.forName("journeymap.api.v2.common.waypoint.Waypoint");
			waypointClass.getMethod("setEnabled", boolean.class).invoke(waypoint, true);
			waypointClass.getMethod("setPersistent", boolean.class).invoke(waypoint, false);
			waypointClass.getMethod("setShowBeacon", boolean.class).invoke(waypoint, true);
			waypointClass.getMethod("setShowInWorld", boolean.class).invoke(waypoint, true);
			waypointClass.getMethod("setShowOnMap", boolean.class).invoke(waypoint, true);

			Class<?> clientApiClass = Class.forName("journeymap.api.client.impl.ClientAPI");
			Field instanceField = clientApiClass.getField("INSTANCE");
			Object clientApi = instanceField.get(null);
			Method addWaypoint = clientApiClass.getMethod("addWaypoint", String.class, waypointClass);
			addWaypoint.invoke(clientApi, MOD_ID, waypoint);
			return true;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}
}
