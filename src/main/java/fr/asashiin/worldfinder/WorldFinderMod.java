package fr.asashiin.worldfinder;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(WorldFinderMod.MOD_ID)
public class WorldFinderMod {
	public static final String MOD_ID = "worldfinder";
	public static final Logger LOGGER = LogUtils.getLogger();

	public WorldFinderMod(IEventBus modEventBus, ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, WorldFinderConfig.SPEC);
	}
}
