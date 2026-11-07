package com.avrix.provider.zomboid;

/**
 * Project Zomboid-specific constants used by the game provider.
 */
public final class ZomboidConstants {

    /**
     * Provider identifier registered through {@code META-INF/services}.
     */
    public static final String PROVIDER_ID = "project-zomboid";

    /**
     * Runtime marker file used by Project Zomboid Lua code to detect an active Avrix launch.
     */
    public static final String RUNTIME_MARKER_NAME = "avrix-active.marker";

    /**
     * Project Zomboid mod.info key listing plugin archives to load from an active mod.
     */
    public static final String JAR_PLUGINS_KEY = "jarplugins";

    /**
     * Steam application identifier used by Project Zomboid Workshop content.
     */
    public static final String WORKSHOP_APP_ID = "108600";

    /**
     * File name containing the list of active client mods.
     */
    public static final String ACTIVE_MODS_FILE_NAME = "default.txt";

    /**
     * System property containing the active dedicated-server name.
     */
    public static final String SERVER_NAME_PROPERTY = "avrix.server.name";

    /**
     * Default dedicated-server name used when no {@code -servername} argument is present.
     */
    public static final String DEFAULT_SERVER_NAME = "servertest";

    /**
     * System property selecting the Workshop plugin approval policy.
     */
    public static final String PLUGIN_POLICY_PROPERTY = "avrix.plugin.policy";

    /**
     * System property selecting the approval frontend: {@code auto}, {@code swing}, {@code console}, or {@code none}.
     */
    public static final String APPROVAL_FRONTEND_PROPERTY = "avrix.approval.frontend";

    /**
     * System property controlling how long the approval window waits for user input, in seconds.
     */
    public static final String APPROVAL_TIMEOUT_PROPERTY = "avrix.approval.timeout";

    /**
     * CLI argument prefix used by Project Zomboid to override the user cache directory.
     */
    public static final String CACHE_DIR_ARGUMENT_PREFIX = "-cachedir=";

    /**
     * System property carrying the cache directory override parsed from {@link #CACHE_DIR_ARGUMENT_PREFIX}.
     */
    public static final String CACHE_DIR_PROPERTY = "avrix.cachedir";

    /**
     * Prevents instantiation of this constant holder.
     */
    private ZomboidConstants() {
        throw new UnsupportedOperationException("Constants utility class cannot be instantiated");
    }
}
