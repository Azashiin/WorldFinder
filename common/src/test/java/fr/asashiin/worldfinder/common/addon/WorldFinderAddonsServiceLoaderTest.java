package fr.asashiin.worldfinder.common.addon;

import fr.asashiin.worldfinder.api.WorldFinderAddon;
import fr.asashiin.worldfinder.api.WorldFinderRegistrar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldFinderAddonsServiceLoaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void brokenServiceDoesNotPreventLaterProvidersFromLoading() throws IOException {
        Path serviceFile = temporaryDirectory.resolve(
                "META-INF/services/" + WorldFinderAddon.class.getName()
        );
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, String.join(System.lineSeparator(),
                "not.present.InvalidWorldFinderAddon",
                BrokenServiceAddon.class.getName(),
                FirstServiceAddon.class.getName(),
                SecondServiceAddon.class.getName()
        ));
        List<AddonIssue> issues = new ArrayList<>();

        try (URLClassLoader classLoader = new URLClassLoader(
                new java.net.URL[]{temporaryDirectory.toUri().toURL()},
                getClass().getClassLoader()
        )) {
            List<WorldFinderAddon> addons = WorldFinderAddons.discoverServices(classLoader, issues::add);

            assertEquals(List.of("service_first", "service_second"),
                    addons.stream().map(WorldFinderAddon::id).toList());
        }
        assertEquals(2, issues.size());
        assertEquals(AddonIssue.Operation.SERVICE_DISCOVERY, issues.getFirst().operation());
        assertEquals(AddonIssue.Operation.SERVICE_INSTANTIATION, issues.get(1).operation());
        assertTrue(issues.get(1).sourceId().contains("BrokenServiceAddon"));
    }

    public static final class BrokenServiceAddon implements WorldFinderAddon {
        public BrokenServiceAddon() {
            throw new IllegalStateException("broken constructor");
        }

        @Override
        public String id() {
            return "never_loaded";
        }

        @Override
        public void register(WorldFinderRegistrar registrar) {
        }
    }

    public static final class FirstServiceAddon implements WorldFinderAddon {
        public FirstServiceAddon() {
        }

        @Override
        public String id() {
            return "service_first";
        }

        @Override
        public void register(WorldFinderRegistrar registrar) {
        }
    }

    public static final class SecondServiceAddon implements WorldFinderAddon {
        public SecondServiceAddon() {
        }

        @Override
        public String id() {
            return "service_second";
        }

        @Override
        public void register(WorldFinderRegistrar registrar) {
        }
    }
}
