package com.avrix.provider;

/**
 * Structured Project Zomboid version information resolved without loading game classes.
 *
 * @param major      major game version component
 * @param minor      minor game version component
 * @param patch      patch/build component
 * @param suffix     version suffix appended to the minor component by the game
 * @param revision   Git revision embedded in the game archive
 * @param raw        raw game version string
 * @param normalized normalized SemVer-like version string
 */
public record GameVersionInfo(
        int major,
        int minor,
        int patch,
        String suffix,
        String revision,
        String raw,
        String normalized
) {

    /**
     * Creates a normalized version descriptor, replacing null string components with empty defaults.
     *
     * @param major      major game version component
     * @param minor      minor game version component
     * @param patch      patch/build component
     * @param suffix     version suffix appended to the minor component by the game
     * @param revision   Git revision embedded in the game archive
     * @param raw        raw game version string
     * @param normalized normalized SemVer-like version string
     */
    public GameVersionInfo {
        suffix = suffix == null ? "" : suffix;
        revision = revision == null ? "" : revision;
        raw = raw == null ? "" : raw;
        normalized = normalized == null ? "0.0.0" : normalized;
    }

    /**
     * Creates a fallback version descriptor used when the game archive cannot be parsed.
     *
     * @return fallback version information
     */
    public static GameVersionInfo fallback() {
        return new GameVersionInfo(0, 0, 0, "", "", "0.0.0", "0.0.0");
    }
}
