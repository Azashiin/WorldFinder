package fr.asashiin.worldfinder.common.worldgen;

import fr.asashiin.worldfinder.api.world.WorldgenProfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fail-closed contract between one compiled native adapter and verified runtime releases.
 *
 * <p>Known generation equivalence is intentionally insufficient. A release enters this contract
 * only after the adapter has compiled and its results have been verified against that exact game
 * runtime.</p>
 *
 * @param compiledAgainstVersion exact Minecraft version used to compile the adapter
 * @param verifiedRuntimeVersions exact runtime version to effective semantic identity
 */
public record NativeWorldgenContract(
        String compiledAgainstVersion,
        Map<String, GenerationSemanticsId> verifiedRuntimeVersions
) {
    /** Validates and defensively copies the verified runtime matrix. */
    public NativeWorldgenContract {
        compiledAgainstVersion = requireVersion(compiledAgainstVersion, "compiledAgainstVersion");
        Objects.requireNonNull(verifiedRuntimeVersions, "verifiedRuntimeVersions");
        LinkedHashMap<String, GenerationSemanticsId> copy = new LinkedHashMap<>();
        verifiedRuntimeVersions.forEach((version, semantics) -> copy.put(
                requireVersion(version, "verified runtime version"),
                Objects.requireNonNull(semantics, "verified runtime semantics")
        ));
        if (!copy.containsKey(compiledAgainstVersion)) {
            throw new IllegalArgumentException(
                    "Verified runtimes must include the compiled-against version "
                            + compiledAgainstVersion);
        }
        verifiedRuntimeVersions = Collections.unmodifiableMap(copy);
    }

    /**
     * Creates the initial exact-runtime contract for one supported vanilla release.
     *
     * @param minecraftVersion exact compiled and verified version
     * @return contract accepting only that runtime
     */
    public static NativeWorldgenContract exactVanilla(String minecraftVersion) {
        VanillaGenerationProfile profile = VanillaGenerationProfiles.require(minecraftVersion);
        return new NativeWorldgenContract(
                minecraftVersion,
                Map.of(minecraftVersion, profile.semanticsId())
        );
    }

    /**
     * Returns whether the native fallback accepts a public generation profile.
     *
     * @param profile declared resolver/cache profile
     * @return whether native generation is verified for the declaration
     */
    public boolean supports(WorldgenProfile profile) {
        try {
            requireSemantics(profile);
            return true;
        } catch (UnsupportedWorldgenProfileException ignored) {
            return false;
        }
    }

    /**
     * Validates a public profile and returns its internal native semantic identity.
     *
     * <p>The legacy unspecified profile aliases the compiled target only. Explicit profiles must
     * name an exact verified version, vanilla Normal, the vanilla profile ID, and no extra
     * generation properties.</p>
     *
     * @param profile declared resolver/cache profile
     * @return verified internal semantics
     * @throws UnsupportedWorldgenProfileException when native fallback is not proven safe
     */
    public GenerationSemanticsId requireSemantics(WorldgenProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (!profile.properties().isEmpty()) {
            throw unsupported(profile, "generation properties are not native");
        }
        if (!"minecraft:normal".equals(profile.worldPresetId())) {
            throw unsupported(profile, "only the vanilla Normal preset is native");
        }
        if (WorldgenProfile.UNSPECIFIED_PROFILE_ID.equals(profile.id())
                && WorldgenProfile.UNKNOWN_MINECRAFT_VERSION.equals(profile.minecraftVersion())) {
            return verifiedRuntimeVersions.get(compiledAgainstVersion);
        }
        if (!WorldgenProfile.VANILLA_PROFILE_ID.equals(profile.id())) {
            throw unsupported(profile, "profile id is not " + WorldgenProfile.VANILLA_PROFILE_ID);
        }
        GenerationSemanticsId semantics = verifiedRuntimeVersions.get(profile.minecraftVersion());
        if (semantics == null) {
            throw unsupported(profile, "runtime version was not verified by this adapter");
        }
        return semantics;
    }

    private UnsupportedWorldgenProfileException unsupported(
            WorldgenProfile profile,
            String reason
    ) {
        return new UnsupportedWorldgenProfileException(
                "Native worldgen adapter compiled for " + compiledAgainstVersion
                        + " refused " + profile.id() + '@' + profile.minecraftVersion()
                        + ": " + reason);
    }

    private static String requireVersion(String version, String name) {
        Objects.requireNonNull(version, name);
        if (version.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return version;
    }
}
