package fr.asashiin.worldfinder.client.maprender;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.renderer.texture.DynamicTexture;

public final class ColorTileTextureManager<K, G extends Enum<G>> {
	private final Map<G, LinkedHashMap<K, Tile>> textureCaches;
	private final LinkedHashMap<K, int[]> pendingUploads;
	private final Function<K, G> groupSelector;
	private final int maxTexturesPerGroup;
	private final int maxPendingUploads;
	private final int uploadsPerFrame;

	public ColorTileTextureManager(Class<G> groupType, Function<K, G> groupSelector, int maxTexturesPerGroup, int maxPendingUploads, int uploadsPerFrame) {
		this.textureCaches = new EnumMap<>(groupType);
		this.pendingUploads = new LinkedHashMap<>(maxPendingUploads, 0.75F, true);
		this.groupSelector = groupSelector;
		this.maxTexturesPerGroup = maxTexturesPerGroup;
		this.maxPendingUploads = maxPendingUploads;
		this.uploadsPerFrame = uploadsPerFrame;
	}

	public Tile get(K key) {
		return textureCache(groupSelector.apply(key)).get(key);
	}

	public boolean contains(K key) {
		return textureCache(groupSelector.apply(key)).containsKey(key);
	}

	public void queueUpload(K key, int[] colors) {
		if (contains(key) || pendingUploads.containsKey(key)) {
			return;
		}
		pendingUploads.put(key, colors);
		while (pendingUploads.size() > maxPendingUploads) {
			K eldest = pendingUploads.keySet().iterator().next();
			pendingUploads.remove(eldest);
		}
	}

	public int uploadPending(TextureFactory<K> textureFactory) {
		int uploaded = 0;
		while (uploaded < uploadsPerFrame && !pendingUploads.isEmpty()) {
			K key = pendingUploads.keySet().iterator().next();
			int[] colors = pendingUploads.remove(key);
			if (contains(key) || colors == null || colors.length == 0) {
				continue;
			}
			LinkedHashMap<K, Tile> textureCache = textureCache(groupSelector.apply(key));
			textureCache.put(key, new Tile(textureFactory.create(key, colors)));
			trim(textureCache);
			uploaded++;
		}
		return uploaded;
	}

	public void close(K key) {
		Tile removed = textureCache(groupSelector.apply(key)).remove(key);
		if (removed != null) {
			removed.close();
		}
	}

	public void removeIf(Predicate<K> predicate) {
		pendingUploads.keySet().removeIf(predicate);
		for (LinkedHashMap<K, Tile> textureCache : textureCaches.values()) {
			textureCache.entrySet().removeIf(entry -> {
				if (!predicate.test(entry.getKey())) {
					return false;
				}
				entry.getValue().close();
				return true;
			});
		}
	}

	public void clearPendingUploads() {
		pendingUploads.clear();
	}

	public void closeAll() {
		for (LinkedHashMap<K, Tile> textureCache : textureCaches.values()) {
			for (Tile tile : textureCache.values()) {
				tile.close();
			}
		}
		textureCaches.clear();
		pendingUploads.clear();
	}

	private LinkedHashMap<K, Tile> textureCache(G group) {
		return textureCaches.computeIfAbsent(group, ignored -> new LinkedHashMap<>(maxTexturesPerGroup, 0.75F, true));
	}

	private void trim(LinkedHashMap<K, Tile> textureCache) {
		while (textureCache.size() > maxTexturesPerGroup) {
			K eldest = textureCache.keySet().iterator().next();
			Tile removed = textureCache.remove(eldest);
			if (removed != null) {
				removed.close();
			}
		}
	}

	@FunctionalInterface
	public interface TextureFactory<K> {
		DynamicTexture create(K key, int[] colors);
	}

	public record Tile(DynamicTexture texture) {
		public void close() {
			texture.close();
		}
	}
}
