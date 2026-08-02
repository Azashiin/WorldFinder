package fr.asashiin.worldfinder.common.map;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.api.world.CancellationToken;
import fr.asashiin.worldfinder.api.world.BiomeSamplingMode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AsyncBiomeTileCacheTest {
    @Test
    void propagatesCancellationIntoRunningGenerators() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch observedCancellation = new CountDownLatch(1);
        BiomeTileGenerator generator = new BiomeTileGenerator() {
            @Override
            public BiomeTile generate(BiomeTileKey key) {
                throw new AssertionError("The cache must use the cancellable generator path");
            }

            @Override
            public BiomeTile generate(BiomeTileKey key, CancellationToken token) {
                started.countDown();
                try {
                    while (true) {
                        token.throwIfCancellationRequested();
                        Thread.onSpinWait();
                    }
                } catch (CancellationException expected) {
                    observedCancellation.countDown();
                    throw expected;
                }
            }
        };

        try (AsyncBiomeTileCache cache = new AsyncBiomeTileCache(2, 1, generator)) {
            var future = cache.request(key(0));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertEquals(1, cache.cancelPending(ignored -> true));
            assertTrue(future.isCancelled());
            assertTrue(observedCancellation.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void cancelsQueuedTilesWithoutDiscardingCompletedTiles() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        try (AsyncBiomeTileCache cache = new AsyncBiomeTileCache(4, 1, key -> {
            if (key.tileX() == 0) {
                firstStarted.countDown();
                try {
                    releaseFirst.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            return new BiomeTile(key, new int[key.tileSize() * key.tileSize()]);
        })) {
            var running = cache.request(key(0));
            var queued = cache.request(key(1));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

            assertEquals(1, cache.cancelPending(tileKey -> tileKey.tileX() == 1));
            assertTrue(queued.isCancelled());
            releaseFirst.countDown();
            running.get(2, TimeUnit.SECONDS);

            assertTrue(cache.ready(key(0)).isPresent());
            assertTrue(cache.request(key(1)).get(2, TimeUnit.SECONDS) != null);
        }
    }

    @Test
    void deduplicatesRequestsAndBoundsTheCache() throws Exception {
        AtomicInteger generations = new AtomicInteger();
        try (AsyncBiomeTileCache cache = new AsyncBiomeTileCache(2, 1, key -> {
            generations.incrementAndGet();
            return new BiomeTile(key, new int[key.tileSize() * key.tileSize()]);
        })) {
            BiomeTileKey first = key(0);
            cache.request(first).get(2, TimeUnit.SECONDS);
            cache.request(first).get(2, TimeUnit.SECONDS);
            cache.request(key(1)).get(2, TimeUnit.SECONDS);
            cache.request(key(2)).get(2, TimeUnit.SECONDS);

            assertEquals(3, generations.get());
            assertEquals(2, cache.size());
            assertTrue(cache.ready(key(2)).isPresent());
        }
    }

    @Test
    void cancelledSurfaceWorkCannotPublishIntoANewFixedHeightView() throws Exception {
        CountDownLatch surfaceStarted = new CountDownLatch(1);
        CountDownLatch releaseSurface = new CountDownLatch(1);
        BiomeTileKey surface = verticalKey(BiomeSamplingMode.SURFACE, 64);
        BiomeTileKey fixed = verticalKey(BiomeSamplingMode.FIXED_Y, -32);

        BiomeTileGenerator generator = new BiomeTileGenerator() {
            @Override
            public BiomeTile generate(BiomeTileKey key) {
                throw new AssertionError("The cache must use the cancellable generator path");
            }

            @Override
            public BiomeTile generate(BiomeTileKey key, CancellationToken token) {
                if (key.samplingMode() == BiomeSamplingMode.SURFACE) {
                    surfaceStarted.countDown();
                    try {
                        while (!releaseSurface.await(5, TimeUnit.MILLISECONDS)) {
                            token.throwIfCancellationRequested();
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    token.throwIfCancellationRequested();
                }
                return new BiomeTile(key, new int[key.tileSize() * key.tileSize()]);
            }
        };
        try (AsyncBiomeTileCache cache = new AsyncBiomeTileCache(4, 2, generator)) {
            var obsolete = cache.request(surface);
            assertTrue(surfaceStarted.await(2, TimeUnit.SECONDS));
            assertEquals(1, cache.cancelPending(key -> key.equals(surface)));
            BiomeTile current = cache.request(fixed).get(2, TimeUnit.SECONDS);
            releaseSurface.countDown();

            assertTrue(obsolete.isCancelled());
            assertFalse(cache.ready(surface).isPresent());
            assertEquals(fixed, current.key());
            assertEquals(fixed, cache.ready(fixed).orElseThrow().key());
        }
    }

    private static BiomeTileKey key(int tileX) {
        return new BiomeTileKey(1L, WorldDimension.OVERWORLD, 64, 4, tileX, 0, 4);
    }

    private static BiomeTileKey verticalKey(BiomeSamplingMode mode, int sampleY) {
        return new BiomeTileKey(
                1L, WorldDimension.OVERWORLD, sampleY, mode,
                4, 0, 0, 4);
    }
}
