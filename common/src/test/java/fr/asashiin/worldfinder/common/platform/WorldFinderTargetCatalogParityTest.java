package fr.asashiin.worldfinder.common.platform;

import fr.asashiin.worldfinder.common.worldgen.VanillaGenerationProfile;
import fr.asashiin.worldfinder.common.worldgen.VanillaGenerationProfiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldFinderTargetCatalogParityTest {
    private static final Pattern TARGET = Pattern.compile(
            "\\\"minecraftVersion\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"apiFamily\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"worldgenBinding\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"generationSemantics\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*"
                    + "\\\"javaVersion\\\"\\s*:\\s*(\\d+)"
    );

    @Test
    void gradleCatalogMatchesTheRuntimeProfileAndSourceFamilyCatalogs() throws IOException {
        Map<String, CatalogTarget> catalog = readCatalog();
        assertEquals(VanillaGenerationProfiles.supportedVersions(), List.copyOf(catalog.keySet()));

        for (VanillaGenerationProfile profile : VanillaGenerationProfiles.profiles()) {
            String version = profile.minecraftVersion();
            CatalogTarget target = catalog.get(version);
            MinecraftVersionFamily apiFamily = MinecraftVersionFamily.find(version).orElseThrow();
            MinecraftWorldgenBindingFamily binding =
                    MinecraftWorldgenBindingFamily.find(version).orElseThrow();

            assertEquals(apiFamily.catalogId(), target.apiFamily(), version + " API family");
            assertEquals(apiFamily.javaVersion(), target.javaVersion(), version + " Java version");
            assertEquals(binding.name(), target.worldgenBinding(), version + " worldgen binding");
            assertEquals(profile.semanticsId().value(), target.generationSemantics(),
                    version + " generation semantics");
        }
    }

    private static Map<String, CatalogTarget> readCatalog() throws IOException {
        String configuredPath = System.getProperty("worldfinder.targetCatalog");
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("worldfinder.targetCatalog test property is missing");
        }
        String json = Files.readString(Path.of(configuredPath));
        Matcher matcher = TARGET.matcher(json);
        LinkedHashMap<String, CatalogTarget> targets = new LinkedHashMap<>();
        while (matcher.find()) {
            String version = matcher.group(1);
            CatalogTarget previous = targets.put(version, new CatalogTarget(
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    Integer.parseInt(matcher.group(5))
            ));
            if (previous != null) {
                throw new IllegalStateException("Duplicate target in JSON catalog: " + version);
            }
        }
        return Collections.unmodifiableMap(targets);
    }

    private record CatalogTarget(
            String apiFamily,
            String worldgenBinding,
            String generationSemantics,
            int javaVersion
    ) {
    }
}
