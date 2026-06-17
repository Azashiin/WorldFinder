package fr.asashiin.worldfinder.client.seed;

public final class SeedMapEngines {
	private static final SeedMapEngine VANILLA = new VanillaSeedMapEngine();
	private static final SeedMapEngine CURRENT = CubiomesSeedMapEngine.isAvailable() ? new CubiomesSeedMapEngine(VANILLA) : VANILLA;

	private SeedMapEngines() {
	}

	public static SeedMapEngine current() {
		return CURRENT;
	}
}
