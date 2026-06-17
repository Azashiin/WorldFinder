package fr.asashiin.worldfinder.client.seedmap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Async cache pattern adapted from xpple/SeedMapper's SeedMapCache.
 */
public final class SeedMapCache<K, V> {
	private final Map<K, Long> pendingCalculations = new ConcurrentHashMap<>();
	private final AtomicLong generation = new AtomicLong();
	private final Map<K, V> cache;
	private final SeedMapExecutor executor;
	private final Consumer<K> postInsert;

	public SeedMapCache(Map<K, V> cache, SeedMapExecutor executor) {
		this(cache, executor, key -> {
		});
	}

	public SeedMapCache(Map<K, V> cache, SeedMapExecutor executor, Consumer<K> postInsert) {
		this.cache = cache;
		this.executor = executor;
		this.postInsert = postInsert;
	}

	public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
		synchronized (cache) {
			V value = cache.get(key);
			if (value != null) {
				return value;
			}
		}
		long submittedGeneration = generation.get();
		if (pendingCalculations.putIfAbsent(key, submittedGeneration) != null) {
			return null;
		}
		executor.submit(() -> {
			if (submittedGeneration != generation.get()) {
				return null;
			}
			return mappingFunction.apply(key);
		}).thenAccept(result -> {
			if (result != null && submittedGeneration == generation.get()) {
				synchronized (cache) {
					cache.put(key, result);
					postInsert.accept(key);
				}
			}
			pendingCalculations.remove(key, submittedGeneration);
		});
		return null;
	}

	public V get(K key) {
		synchronized (cache) {
			return cache.get(key);
		}
	}

	public boolean isPending(K key) {
		return pendingCalculations.containsKey(key);
	}

	public void clearPending() {
		generation.incrementAndGet();
		pendingCalculations.clear();
	}
}
