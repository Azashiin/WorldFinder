package fr.asashiin.worldfinder.client;

import fr.asashiin.worldfinder.WorldFinderMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WorldFinderMod.MOD_ID, dist = Dist.CLIENT)
public final class WorldFinderClient {
	public WorldFinderClient(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(WorldFinderKeyMappings::registerKeyMappings);
		modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
	}
}
