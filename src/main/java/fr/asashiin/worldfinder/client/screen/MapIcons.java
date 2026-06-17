package fr.asashiin.worldfinder.client.screen;

import java.util.HashMap;
import java.util.Map;

import fr.asashiin.worldfinder.client.finder.FinderResult;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MapIcons {
	private static final Map<String, SpriteIcon> MARKER_SPRITE_CACHE = new HashMap<>();

	private MapIcons() {
	}

	public record SpriteIcon(Identifier atlas, Identifier texture, boolean rawTexture) {
	}

	private static SpriteIcon item(String path) {
		return new SpriteIcon(TextureAtlas.LOCATION_ITEMS, Identifier.withDefaultNamespace("item/" + path), false);
	}

	private static SpriteIcon block(String path) {
		return new SpriteIcon(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/" + path), false);
	}

	private static SpriteIcon gui(String path) {
		return new SpriteIcon(null, Identifier.fromNamespaceAndPath("worldfinder", "textures/gui/icons/" + path + ".png"), true);
	}

	public static SpriteIcon markerSprite(FinderResult marker) {
		String key = marker.label() + "\u0000" + marker.note();
		SpriteIcon cached = MARKER_SPRITE_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		SpriteIcon icon = markerSpriteUncached(marker);
		MARKER_SPRITE_CACHE.put(key, icon);
		return icon;
	}

	private static SpriteIcon markerSpriteUncached(FinderResult marker) {
		String normalizedLabel = marker.label().toLowerCase();
		String normalizedNote = marker.note().toLowerCase();
		if (normalizedLabel.contains("end city") && (normalizedNote.contains("ship") || normalizedNote.contains("elytra"))) {
			return item("elytra");
		}
		return labelSprite(marker.label());
	}

	public static SpriteIcon labelSprite(String label) {
		String normalized = label.toLowerCase();
		if (normalized.contains("blaze spawner")) return item("blaze_rod");
		if (normalized.equals("spawn") || normalized.contains("spawn point") || normalized.contains("world spawn")) return item("compass_00");
		if (normalized.contains("stronghold")) return item("ender_eye");
		if (normalized.contains("abandoned village")) return gui("zombie");
		if (normalized.contains("village")) return gui("village");
		if (normalized.contains("ancient")) return item("echo_shard");
		if (normalized.contains("trial")) return item("trial_key");
		if (normalized.contains("bastion treasure")) return gui("buried_treasure");
		if (normalized.contains("treasure")) return gui("buried_treasure");
		if (normalized.contains("geode")) return block("amethyst_cluster");
		if (normalized.contains("copper ore vein")) return block("raw_copper_block");
		if (normalized.contains("iron ore vein")) return item("raw_iron");
		if (normalized.contains("mineshaft")) return block("rail");
		if (normalized.contains("outpost")) return item("crossbow_standby");
		if (normalized.contains("desert")) return block("chiseled_sandstone");
		if (normalized.contains("jungle")) return block("mossy_cobblestone");
		if (normalized.contains("swamp")) return item("cauldron");
		if (normalized.contains("igloo")) return block("blue_ice");
		if (normalized.contains("monument") || normalized.contains("ocean")) return item("prismarine_shard");
		if (normalized.contains("shipwreck")) return item("oak_boat");
		if (normalized.contains("mansion")) return item("totem_of_undying");
		if (normalized.contains("portal")) return block("obsidian");
		if (normalized.contains("trail")) return item("brush");
		if (normalized.contains("bastion hoglin")) return block("crimson_fungus");
		if (normalized.contains("bastion bridge")) return block("gilded_blackstone");
		if (normalized.contains("bastion housing")) return block("polished_blackstone_bricks");
		if (normalized.contains("fortress")) return item("nether_brick");
		if (normalized.contains("bastion")) return gui("bastion_remnant");
		if (normalized.contains("end city")) return block("purpur_block");
		if (normalized.contains("end gateway")) return item("ender_pearl");
		return item("filled_map");
	}

	public static ItemStack marker(FinderResult marker) {
		String normalizedLabel = marker.label().toLowerCase();
		String normalizedNote = marker.note().toLowerCase();
		if (normalizedLabel.contains("end city") && (normalizedNote.contains("ship") || normalizedNote.contains("elytra"))) {
			return new ItemStack(Items.ELYTRA);
		}
		return label(marker.label());
	}

	public static ItemStack label(String label) {
		String normalized = label.toLowerCase();
		if (normalized.contains("blaze spawner")) return new ItemStack(Items.BLAZE_ROD);
		if (normalized.equals("spawn") || normalized.contains("spawn point") || normalized.contains("world spawn")) return new ItemStack(Items.COMPASS);
		if (normalized.contains("stronghold")) return new ItemStack(Items.ENDER_EYE);
		if (normalized.contains("abandoned village")) return new ItemStack(Items.ZOMBIE_HEAD);
		if (normalized.contains("village")) return new ItemStack(Items.EMERALD);
		if (normalized.contains("ancient")) return new ItemStack(Items.ECHO_SHARD);
		if (normalized.contains("trial")) return new ItemStack(Items.TRIAL_KEY);
		if (normalized.contains("bastion treasure")) return new ItemStack(Items.CHEST);
		if (normalized.contains("treasure")) return new ItemStack(Items.CHEST);
		if (normalized.contains("geode")) return new ItemStack(Items.AMETHYST_CLUSTER);
		if (normalized.contains("copper ore vein")) return new ItemStack(Items.RAW_COPPER_BLOCK);
		if (normalized.contains("iron ore vein")) return new ItemStack(Items.RAW_IRON_BLOCK);
		if (normalized.contains("mineshaft")) return new ItemStack(Items.RAIL);
		if (normalized.contains("outpost")) return new ItemStack(Items.CROSSBOW);
		if (normalized.contains("desert")) return new ItemStack(Items.CHISELED_SANDSTONE);
		if (normalized.contains("jungle")) return new ItemStack(Items.MOSSY_COBBLESTONE);
		if (normalized.contains("swamp")) return new ItemStack(Items.CAULDRON);
		if (normalized.contains("igloo")) return new ItemStack(Items.BLUE_ICE);
		if (normalized.contains("monument") || normalized.contains("ocean")) return new ItemStack(Items.PRISMARINE_SHARD);
		if (normalized.contains("shipwreck")) return new ItemStack(Items.OAK_BOAT);
		if (normalized.contains("mansion")) return new ItemStack(Items.TOTEM_OF_UNDYING);
		if (normalized.contains("portal")) return new ItemStack(Items.OBSIDIAN);
		if (normalized.contains("trail")) return new ItemStack(Items.BRUSH);
		if (normalized.contains("bastion hoglin")) return new ItemStack(Items.CRIMSON_FUNGUS);
		if (normalized.contains("bastion bridge")) return new ItemStack(Items.GILDED_BLACKSTONE);
		if (normalized.contains("bastion housing")) return new ItemStack(Items.POLISHED_BLACKSTONE_BRICKS);
		if (normalized.contains("fortress")) return new ItemStack(Items.NETHER_BRICK);
		if (normalized.contains("bastion")) return new ItemStack(Items.GILDED_BLACKSTONE);
		if (normalized.contains("end city")) return new ItemStack(Items.PURPUR_BLOCK);
		if (normalized.contains("end gateway")) return new ItemStack(Items.ENDER_PEARL);
		return new ItemStack(Items.FILLED_MAP);
	}
}
