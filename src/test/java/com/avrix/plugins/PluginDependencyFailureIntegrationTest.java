package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.DefaultLoaderProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;
import test.plugin.PhaseTracker;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Verifies dependency failure isolation across discovery, preparation, and initialization.
 */
public class PluginDependencyFailureIntegrationTest {

    @TempDir
    private Path pluginsDirectory;
    private final List<DefaultPluginManager> managers = new ArrayList<>();

    /** Cleans up plugin instances and shared fixture state. */
    @AfterEach
    public void resetEvents() {
        managers.forEach(DefaultPluginManager::close);
        PhaseTracker.reset();
    }

    /**
     * Verifies that failed entrypoints block transitive hard dependents.
     *
     * @throws IOException if a fixture archive cannot be written or closed
     */
    @Test
    public void shouldSkipTransitiveDependentsAfterEntrypointFailure() throws IOException {
        createJar("base", "entrypoint: test.plugin.ErrorThrowingPlugin\n");
        createJar("dependent", "dependencies: {base: '*'}\nentrypoint: test.plugin.LifecyclePlugin\n");
        createJar("transitive", "dependencies: {dependent: '*'}\nentrypoint: test.plugin.LifecyclePlugin\n");
        createJar("independent", "");

        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var manager = manager(loader, false, name -> {});
            manager.init();
            manager.loadPlugins();

            assertThat(manager.getLoadResults()).extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .containsExactlyInAnyOrder(
                            tuple("base", PluginLoadStatus.FAILED),
                            tuple("dependent", PluginLoadStatus.SKIPPED),
                            tuple("transitive", PluginLoadStatus.SKIPPED),
                            tuple("independent", PluginLoadStatus.LOADED));
            assertThat(manager.getLoadResults()).filteredOn(result -> result.pluginId().equals("dependent"))
                    .singleElement().extracting(PluginLoadResult::message).asString().contains("base");
            assertThat(PhaseTracker.events()).isEmpty();
        }
    }

    /**
     * Verifies that unsafe registration failures stop startup before dependent mixins are registered.
     *
     * @throws IOException if a fixture archive cannot be written or closed
     */
    @Test
    public void shouldSkipDependentMixinsAfterPreparationFailure() throws IOException {
        createJar("base", "mixins: [example.BrokenMixin]\n");
        createJar("dependent", "dependencies: {base: '*'}\nmixins: [example.DependentMixin]\n");
        createJar("independent", "");
        List<String> registered = new ArrayList<>();

        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var manager = manager(loader, false, name -> {
                 registered.add(name);
                 throw new IllegalStateException("Broken mixin");
            });
            manager.init();
            assertThatThrownBy(manager::loadPlugins).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Plugin preparation failed: base");

            assertThat(registered).containsExactly("example.BrokenMixin");
            assertThat(manager.getPlugins()).doesNotContainKeys("base", "dependent", "independent");
            assertThat(manager.getLoadResults()).extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                    .contains(tuple("base", PluginLoadStatus.FAILED));
        }
    }

    /**
     * Verifies lenient recovery and strict rejection of incomplete dependency graphs.
     *
     * @throws IOException if a fixture archive cannot be written or closed
     */
    @Test
    public void shouldLoadIndependentPluginAfterMissingDependencyButAbortInStrictMode() throws IOException {
        createJar("orphan", "dependencies: {missing: '*'}\n");
        createJar("independent", "");

        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var manager = manager(loader, false, name -> {});
            manager.init();
            manager.loadPlugins();
            assertThat(manager.getPlugins()).containsKey("independent").doesNotContainKey("orphan");
        }
        try (var loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            var manager = manager(loader, true, name -> {});
            manager.init();
            assertThatThrownBy(manager::loadPlugins).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to resolve plugin load order");
            assertThat(manager.getPlugins()).doesNotContainKeys("independent", "orphan");
        }
    }

    private DefaultPluginManager manager(KnotClassLoader loader, boolean strict, MixinRegistrar registrar) {
        LoaderConfig config = LoaderConfig.defaults().withPluginsDirectory(pluginsDirectory)
                .withEnvironment(Environment.BOTH).withStrict(strict);
        var manager = new DefaultPluginManager(config, loader, new TestGameProvider(), new DefaultLoaderProvider(),
                registrar, new PluginScanner(config), new PluginManifestParser());
        managers.add(manager);
        return manager;
    }

    private void createJar(String id, String extraMetadata) throws IOException {
        String metadata = "schema: 1\nid: " + id + "\nname: " + id + "\nversion: 1.0.0\n" + extraMetadata;
        try (var out = new JarOutputStream(Files.newOutputStream(pluginsDirectory.resolve(id + ".jar")))) {
            out.putNextEntry(new JarEntry("metadata.yml"));
            out.write(metadata.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }
}
