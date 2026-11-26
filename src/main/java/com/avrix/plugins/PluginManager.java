package com.avrix.plugins;

import com.avrix.core.Metadata;
import com.github.zafarkhaja.semver.Version;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the discovery, dependency resolution, and loading of Avrix plugins.
 */
public interface PluginManager {

    /**
     * Initializes the plugin manager.
     */
    void init();

    /**
     * Discovers and loads all available plugins.
     */
    void loadPlugins();

    /**
     * Loads and initializes a specific plugin.
     * <p>
     * This method bypasses discovery-time approval. Callers are responsible for passing only trusted containers.
     *
     * @param container the plugin data container
     */
    void loadPlugin(PluginData container);

    /**
     * Returns a map of all loaded plugins, keyed by their unique ID.
     *
     * @return map of plugin IDs to their data containers
     */
    Map<String, PluginData> getPlugins();

    /**
     * Finds a loaded plugin or core component by its identifier.
     *
     * @param id canonical plugin identifier
     * @return optional containing the matching descriptor, or empty when the ID is not loaded
     * @throws NullPointerException     if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    default Optional<PluginData> findPlugin(String id) {
        return Optional.ofNullable(getPlugins().get(requirePluginId(id)));
    }

    /**
     * Returns the most recent load result recorded for a plugin identifier.
     * <p>
     * Results are scanned from the end of {@link #getLoadResults()}, so repeated attempts for the same ID resolve
     * to the latest outcome.
     *
     * @param id canonical plugin identifier
     * @return optional containing the last matching result, or empty when no attempt was recorded
     * @throws NullPointerException     if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    default Optional<PluginLoadResult> findLoadResult(String id) {
        String pluginId = requirePluginId(id);
        List<PluginLoadResult> results = getLoadResults();
        for (int index = results.size() - 1; index >= 0; index--) {
            PluginLoadResult result = results.get(index);
            if (pluginId.equals(result.pluginId())) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether a plugin or core component with the given identifier is currently loaded.
     *
     * @param id canonical plugin identifier
     * @return {@code true} when the identifier resolves to a loaded plugin or core component
     * @throws NullPointerException     if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    default boolean isLoaded(String id) {
        return findPlugin(id).isPresent();
    }

    /**
     * Checks whether a loaded plugin satisfies a semantic version constraint.
     * <p>
     * The constraint uses the same syntax as plugin dependencies. A missing plugin returns {@code false}; an invalid
     * constraint is rejected before the lookup.
     *
     * @param id                canonical plugin identifier
     * @param versionConstraint semantic version constraint such as {@code ">=1.0.0"} or {@code "^2.1.0"}
     * @return {@code true} when the plugin is loaded and its declared version satisfies the constraint
     * @throws NullPointerException     if {@code id} or {@code versionConstraint} is {@code null}
     * @throws IllegalArgumentException if {@code id} or {@code versionConstraint} is blank, or the constraint syntax is
     *                                  invalid
     */
    default boolean isLoaded(String id, String versionConstraint) {
        String pluginId = requirePluginId(id);
        String constraint = Objects.requireNonNull(versionConstraint, "Version constraint cannot be null").strip();
        if (constraint.isEmpty()) {
            throw new IllegalArgumentException("Version constraint cannot be blank");
        }
        SemVerConstraints.validate(constraint);

        Optional<PluginData> plugin = findPlugin(pluginId);
        if (plugin.isEmpty()) {
            return false;
        }

        try {
            Version version = Version.parse(plugin.get().metadata().version());
            return version.satisfies(constraint);
        } catch (RuntimeException _) {
            return false;
        }
    }

    /**
     * Returns every loaded plugin backed by a physical archive, excluding synthetic core components.
     *
     * @return immutable list of user plugins sorted by identifier
     */
    default List<PluginData> getUserPlugins() {
        return getPlugins().values().stream()
                .filter(plugin -> plugin.getPluginPath().isPresent())
                .sorted(Comparator.comparing(PluginData::id))
                .toList();
    }

    /**
     * Returns classpath conflicts detected while plugin archives were attached.
     * <p>
     * Each conflict lists every plugin that provides the same class resource and the plugin whose copy wins through the
     * first-wins classpath order.
     *
     * @return immutable list of conflicts sorted by resource path, or an empty list when detection is unsupported
     */
    default List<ClasspathConflict> getClasspathConflicts() {
        return List.of();
    }

    /**
     * Returns the results of the most recent plugin load attempt.
     *
     * @return immutable list of load results
     */
    default List<PluginLoadResult> getLoadResults() {
        return List.of();
    }

    /**
     * Shuts down loaded plugins and releases plugin manager resources.
     */
    default void close() {
    }

    /**
     * Resolves the correct loading order for plugins based on their dependencies.
     *
     * @param candidates the list of plugin metadata to sort
     * @return a sorted list representing the correct load order
     */
    List<Metadata> resolvePluginLoadOrder(List<Metadata> candidates);

    /**
     * Normalizes and validates a plugin identifier used for lookups.
     *
     * @param id candidate plugin identifier
     * @return the trimmed identifier
     * @throws NullPointerException     if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is blank
     */
    private static String requirePluginId(String id) {
        Objects.requireNonNull(id, "Plugin ID cannot be null");
        String pluginId = id.strip();
        if (pluginId.isEmpty()) {
            throw new IllegalArgumentException("Plugin ID cannot be blank");
        }
        return pluginId;
    }
}
