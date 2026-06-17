package fr.asashiin.worldfinder.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import fr.asashiin.worldfinder.WorldFinderMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class WorldFinderKeyMappings {
	private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
			Identifier.fromNamespaceAndPath(WorldFinderMod.MOD_ID, "controls")
	);

	public static final KeyMapping OPEN = new KeyMapping(
			"key.worldfinder.open",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_M,
			CATEGORY
	);

	private WorldFinderKeyMappings() {
	}

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORY);
		event.register(OPEN);
	}
}
