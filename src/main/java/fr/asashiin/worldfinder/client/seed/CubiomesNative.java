package fr.asashiin.worldfinder.client.seed;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import fr.asashiin.worldfinder.WorldFinderMod;

final class CubiomesNative {
	private static boolean attempted;
	private static boolean loaded;

	private CubiomesNative() {
	}

	static synchronized boolean load() {
		if (attempted) {
			return loaded;
		}
		attempted = true;
		String libraryName = System.mapLibraryName("cubiomes");
		try (InputStream input = CubiomesNative.class.getClassLoader().getResourceAsStream(libraryName)) {
			if (input == null) {
				WorldFinderMod.LOGGER.warn("Cubiomes native library {} was not found in the mod jar", libraryName);
				return false;
			}
			Path tempFile = Files.createTempFile("worldfinder-" + libraryName, "");
			Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
			System.load(tempFile.toAbsolutePath().toString());
			loaded = true;
			return true;
		} catch (IOException | UnsatisfiedLinkError | RuntimeException exception) {
			WorldFinderMod.LOGGER.warn("Unable to load Cubiomes native backend; falling back to vanilla backend", exception);
			return false;
		}
	}
}
