package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.world.BiomeRegionQuery;
import fr.asashiin.worldfinder.api.world.ResolverResult;
import fr.asashiin.worldfinder.api.world.WorldgenContext;
import fr.asashiin.worldfinder.api.world.WorldgenResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonIsolationTest {
    @Test
    void cancellationIsNeverConvertedIntoAnAddonFailure() {
        CancellationException cancellation = new CancellationException("obsolete tile");
        WorldgenResolver resolver = new WorldgenResolver() {
            @Override
            public String id() {
                return "test:cancelling";
            }

            @Override
            public boolean supports(WorldgenContext context) {
                return true;
            }

            @Override
            public ResolverResult<fr.asashiin.worldfinder.api.world.BiomeRegion> resolveBiomeRegion(
                    BiomeRegionQuery query
            ) {
                throw cancellation;
            }
        };

        CancellationException thrown = assertThrows(CancellationException.class,
                () -> new AddonBiomeResolverChain(List.of(resolver), issue -> {
                    throw new AssertionError("Cancellation must not be reported");
                }).resolveBiomeRegion(new BiomeRegionQuery(
                        ApiContractTest.context(), 0, 0, 1, 1, 4, 64
                )));

        assertSame(cancellation, thrown);
    }

    @Test
    void interruptedFailureRestoresInterruptStatusAndEscapes() {
        Thread.interrupted();
        try {
            assertThrows(IllegalStateException.class,
                    () -> AddonIsolation.rethrowFatal(new InterruptedException("stop")));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void threadDeathEscapesWithoutCompileTimeDependency()
            throws ReflectiveOperationException {
        Throwable threadDeath;
        try {
            threadDeath = (Throwable) Class.forName("java.lang.ThreadDeath")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (InvocationTargetException exception) {
            throw new AssertionError(exception.getCause());
        }

        Error thrown = assertThrows(Error.class, () -> AddonIsolation.rethrowFatal(threadDeath));

        assertSame(threadDeath, thrown);
    }
}
