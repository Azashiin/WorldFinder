package fr.asashiin.worldfinder.client;

import fr.asashiin.worldfinder.WorldFinderMod;
import fr.asashiin.worldfinder.client.screen.WorldFinderMapScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = WorldFinderMod.MOD_ID, value = Dist.CLIENT)
public final class WorldFinderClientEvents {
	private WorldFinderClientEvents() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		SafeTeleportScheduler.tick(client);
		while (WorldFinderKeyMappings.OPEN.consumeClick()) {
			client.setScreen(new WorldFinderMapScreen(client.screen));
		}
	}

	@SubscribeEvent
	public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		WorldFinderClientCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onClientChatReceived(ClientChatReceivedEvent.System event) {
		if (!event.isOverlay() && SafeTeleportScheduler.shouldSuppressTeleportFeedback(event.getMessage())) {
			event.setCanceled(true);
		}
	}
}
