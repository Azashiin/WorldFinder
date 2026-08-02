package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.CancellationToken;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Bounded, thread-safe cache that generates biome tiles on background worker threads.
 *
 * <p>Requests for the same key share one {@link CompletableFuture}. Entries are retained in
 * least-recently-used order, and an unfinished eldest entry is cancelled when the configured
 * capacity is exceeded. Callers should close the cache when it is no longer needed so that its
 * worker threads are stopped.</p>
 */
public final class AsyncBiomeTileCache implements AutoCloseable {
    private final int maximumEntries;
    private final BiomeTileGenerator generator;
    private final ExecutorService executor;
    private final Map<BiomeTileKey, TileTask> entries;

    /**
     * Creates an asynchronous tile cache.
     *
     * @param maximumEntries maximum number of retained keys; must be positive
     * @param workerCount number of background generator threads; must be positive
     * @param generator non-null function used to generate cache misses
     * @throws IllegalArgumentException if {@code maximumEntries} or {@code workerCount} is not positive
     * @throws NullPointerException if {@code generator} is {@code null}
     */
    public AsyncBiomeTileCache(int maximumEntries, int workerCount, BiomeTileGenerator generator) {
        if (maximumEntries <= 0 || workerCount <= 0) {
            throw new IllegalArgumentException("Cache size and worker count must be positive");
        }
        this.maximumEntries = maximumEntries;
        this.generator = Objects.requireNonNull(generator, "generator");
        AtomicInteger workerIndex = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable,
                    "World Finder biome worker " + workerIndex.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(workerCount, factory);
        this.entries = new LinkedHashMap<>(maximumEntries, 0.75F, true);
    }

    /**
     * Returns the existing computation for a key or schedules a new one.
     *
     * @param key non-null tile key
     * @return shared future for the requested tile
     */
    public synchronized CompletableFuture<BiomeTile> request(BiomeTileKey key) {
        Objects.requireNonNull(key, "key");
        TileTask existing = entries.get(key);
        if (existing != null) {
            return existing.future();
        }
        AtomicBoolean cancellationRequested = new AtomicBoolean();
        CancellationToken cancellationToken = () -> cancellationRequested.get()
                || Thread.currentThread().isInterrupted();
        CompletableFuture<BiomeTile> created = CompletableFuture.supplyAsync(
                () -> generator.generate(key, cancellationToken), executor);
        TileTask task = new TileTask(created, cancellationRequested);
        entries.put(key, task);
        trim();
        return created;
    }

    /**
     * Looks up a successfully generated tile without waiting.
     *
     * @param key tile key to inspect
     * @return the completed tile, or an empty optional when it is absent, pending, cancelled, or failed
     */
    public synchronized Optional<BiomeTile> ready(BiomeTileKey key) {
        return Optional.ofNullable(getReady(key));
    }

    /**
     * Performs an allocation-free lookup suitable for render loops.
     *
     * @param key tile key to inspect
     * @return the completed tile, or {@code null} when it is absent, pending, cancelled, or failed
     */
    public synchronized BiomeTile getReady(BiomeTileKey key) {
        TileTask task = entries.get(key);
        if (task == null || !task.future().isDone()) {
            return null;
        }
        CompletableFuture<BiomeTile> future = task.future();
        if (future.isCompletedExceptionally() || future.isCancelled()) {
            entries.remove(key);
            return null;
        }
        return future.getNow(null);
    }

    /**
     * Cancels and removes pending entries whose keys match a predicate.
     *
     * <p>Completed entries are retained even when their keys match.</p>
     *
     * @param predicate non-null predicate selecting pending entries
     * @return number of futures cancelled and removed
     */
    public synchronized int cancelPending(Predicate<BiomeTileKey> predicate) {
        int cancelled = 0;
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BiomeTileKey, TileTask> entry = iterator.next();
            TileTask task = entry.getValue();
            if (!task.future().isDone() && predicate.test(entry.getKey())) {
                task.cancel();
                iterator.remove();
                cancelled++;
            }
        }
        return cancelled;
    }

    /** Cancels every pending entry and removes all entries from the cache. */
    public synchronized void clear() {
        entries.values().forEach(TileTask::cancel);
        entries.clear();
    }

    /**
     * Returns the number of retained keys, including pending computations.
     *
     * @return current entry count
     */
    public synchronized int size() {
        return entries.size();
    }

    private void trim() {
        while (entries.size() > maximumEntries) {
            BiomeTileKey eldest = entries.keySet().iterator().next();
            TileTask removed = entries.remove(eldest);
            if (removed != null && !removed.future().isDone()) {
                removed.cancel();
            }
        }
    }

    /** Stops the worker pool after clearing all retained entries. */
    @Override
    public synchronized void close() {
        clear();
        executor.shutdownNow();
    }

    private record TileTask(
            CompletableFuture<BiomeTile> future,
            AtomicBoolean cancellationRequested
    ) {
        private void cancel() {
            cancellationRequested.set(true);
            future.cancel(true);
        }
    }

}
