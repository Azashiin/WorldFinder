package fr.asashiin.worldfinder.client.seedmap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import fr.asashiin.worldfinder.WorldFinderMod;

/**
 * Async seed-map calculation executor, adapted from xpple/SeedMapper's SeedMapExecutor design.
 */
public final class SeedMapExecutor {
	private static final int MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

	private final List<CompletableFuture<?>> futures = new ArrayList<>();
	private final ExecutorService executor;

	public SeedMapExecutor(String threadNamePrefix) {
		this(threadNamePrefix, Math.min(8, MAX_THREADS));
	}

	public SeedMapExecutor(String threadNamePrefix, int threadCount) {
		AtomicInteger index = new AtomicInteger();
		this.executor = Executors.newFixedThreadPool(Math.max(1, Math.min(threadCount, MAX_THREADS)), task -> {
			Thread thread = new Thread(task, threadNamePrefix + " " + index.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
	}

	public <T> CompletableFuture<T> submit(Supplier<T> task) {
		CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
			try {
				return task.get();
			} catch (Throwable throwable) {
				WorldFinderMod.LOGGER.error("Seed map calculation failed", throwable);
				return null;
			}
		}, executor);
		synchronized (futures) {
			futures.add(future);
		}
		future.whenComplete((value, throwable) -> {
			synchronized (futures) {
				futures.remove(future);
			}
		});
		return future;
	}

	public void close() {
		synchronized (futures) {
			CompletableFuture.allOf(futures.stream()
					.map(future -> future.handle((value, throwable) -> null))
					.toArray(CompletableFuture[]::new))
					.thenRun(executor::shutdownNow);
			futures.clear();
		}
	}
}
