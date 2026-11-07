package com.avrix.plugins;

import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginArchive;
import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.DefaultLoaderProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import test.loader.TestGameProvider;
import test.plugin.PartiallyInitializedPlugin;
import test.plugin.PhaseTracker;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies cleanup, reporting, and fail-fast behavior around irreversible plugin operations.
 */
public class PluginLifecycleSafetyTest {

    @TempDir
    private Path directory;

    /** Clears fixture events between tests. */
    @AfterEach
    public void reset() {
        PhaseTracker.reset();
    }

    /**
     * Verifies cleanup exactly once after failed initialization, with or without registered mixins.
     *
     * @param withMixins whether unsafe transformer state requires aborting startup
     * @throws IOException if fixture archives cannot be written or closed
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldCleanUpFailedInitialization(boolean withMixins) throws IOException {
        jar("failing", "entrypoint: " + PartiallyInitializedPlugin.class.getName() + "\n"
                + (withMixins ? "mixins: [example.Mixin]\n" : ""));
        try (var loader = loader()) {
            var manager = manager(loader, name -> {}, false);
            manager.init();
            if (withMixins) {
                assertThatThrownBy(manager::loadPlugins).hasMessageContaining("Plugin load failed: failing");
            } else {
                manager.loadPlugins();
            }
            manager.close();
            assertThat(PhaseTracker.events()).containsExactly("init:partial", "shutdown:partial");
        }
    }

    /**
     * Verifies that preflight failures leave the active registrar untouched.
     *
     * @throws IOException if fixture archives cannot be written or closed
     */
    @Test
    public void shouldValidateWholePluginBeforeRegistration() throws IOException {
        jar("broken", "mixins: [example.Valid, example.Broken]\n");
        jar("independent", "");
        List<String> registered = new ArrayList<>();
        MixinRegistrar registrar = new MixinRegistrar() {
            /** {@inheritDoc} */
            @Override
            public void addMixin(String name) {
                registered.add(name);
            }

            /** {@inheritDoc} */
            @Override
            public void validateMixins(Collection<String> names) {
                throw new IllegalArgumentException("Invalid mixin declaration");
            }
        };
        try (var loader = loader()) {
            var manager = manager(loader, registrar, false);
            manager.init();
            manager.loadPlugins();
            assertThat(registered).isEmpty();
            assertThat(manager.getPlugins()).containsKey("independent").doesNotContainKey("broken");
            manager.close();
        }
    }

    /**
     * Verifies that partial registration is fatal even in lenient mode.
     *
     * @throws IOException if fixture archives cannot be written or closed
     */
    @Test
    public void shouldAbortAfterPartialRegistration() throws IOException {
        jar("broken", "mixins: [example.Valid, example.Broken]\n");
        List<String> registered = new ArrayList<>();
        try (var loader = loader()) {
            var manager = manager(loader, name -> {
                registered.add(name);
                if (name.endsWith("Broken")) {
                    throw new IllegalStateException("Registration failure");
                }
            }, false);
            manager.init();
            assertThatThrownBy(manager::loadPlugins).hasMessageContaining("Plugin preparation failed: broken");
            assertThat(registered).containsExactly("example.Valid", "example.Broken");
            assertThat(manager.getLoadResults()).singleElement()
                    .extracting(PluginLoadResult::status).isEqualTo(PluginLoadStatus.FAILED);
            manager.close();
        }
    }

    /**
     * Verifies reporting and strict behavior for an archive changed after approval.
     *
     * @param strict whether startup must fail
     * @throws IOException if fixture archives cannot be written or closed
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldReportIntegrityFailure(boolean strict) throws IOException {
        Path path = jar("changed", "");
        var config = config(strict).withPluginsDirectory(directory.resolve("trusted"));
        PluginApprovalGate gate = (archive, metadata) -> {
            try {
                Files.writeString(path, "changed", StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return PluginApprovalGate.Status.APPROVED;
        };
        var archive = new PluginArchive(path.toFile(), "mod", "Mod", PluginArchive.SOURCE_MOD, java.util.Map.of(), false);
        try (var loader = loader()) {
            var manager = new DefaultPluginManager(config, loader, new TestGameProvider(), new DefaultLoaderProvider(),
                    name -> {}, new PluginScanner(config, List.of(() -> List.of(archive))), new PluginManifestParser(), gate);
            manager.init();
            if (strict) {
                assertThatThrownBy(manager::loadPlugins).hasMessageContaining("Plugin preparation failed: changed");
            } else {
                manager.loadPlugins();
            }
            assertThat(manager.getLoadResults()).singleElement().satisfies(result -> {
                assertThat(result.status()).isEqualTo(PluginLoadStatus.FAILED);
                assertThat(result.message()).contains("changed between approval and load");
            });
            manager.close();
        }
    }

    /**
     * Verifies that concurrent readers cannot republish a stale snapshot after a load or close.
     *
     * @throws Exception if fixture setup or concurrent work fails
     */
    @Test
    public void shouldPublishCurrentSnapshots() throws Exception {
        Path archive = jar("snapshot", "");
        try (var loader = loader(); var executor = Executors.newFixedThreadPool(2)) {
            var manager = manager(loader, name -> {}, false);
            manager.init();
            var reader = executor.submit(() -> {
                for (int i = 0; i < 10000; i++) {
                    manager.getPlugins();
                }
            });
            manager.loadPlugin(new PluginData(archive, null, null, new Metadata.Builder()
                    .id("snapshot").name("Snapshot").version("1.0.0").build()));
            reader.get(10, TimeUnit.SECONDS);
            assertThat(manager.getPlugins()).containsKey("snapshot");
            manager.close();
            assertThat(manager.getPlugins()).isEmpty();
        }
    }

    private DefaultPluginManager manager(KnotClassLoader loader, MixinRegistrar registrar, boolean strict) {
        var config = config(strict);
        return new DefaultPluginManager(config, loader, new TestGameProvider(), new DefaultLoaderProvider(), registrar,
                new PluginScanner(config), new PluginManifestParser());
    }

    /**
     * Verifies that lenient startup cannot turn a fatal JVM error into an ordinary failed-plugin report.
     *
     * @throws IOException if fixture archives cannot be written or closed
     */
    @Test
    public void shouldPropagateFatalEntrypointFailure() throws IOException {
        jar("fatal", "entrypoint: test.plugin.FatalPlugin\n");
        try (var loader = loader()) {
            var manager = manager(loader, name -> {}, false);
            manager.init();
            try {
                assertThatThrownBy(manager::loadPlugins).isInstanceOf(InternalError.class)
                        .hasMessage("Simulated fatal plugin failure");
                assertThat(manager.getLoadResults()).isEmpty();
            } finally {
                manager.close();
            }
        }
    }

    private LoaderConfig config(boolean strict) {
        return LoaderConfig.defaults().withPluginsDirectory(directory).withEnvironment(Environment.BOTH).withStrict(strict);
    }

    private KnotClassLoader loader() {
        return new KnotClassLoader(new URL[0], getClass().getClassLoader());
    }

    private Path jar(String id, String extra) throws IOException {
        Path path = directory.resolve(id + ".jar");
        try (var output = new java.util.jar.JarOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new java.util.jar.JarEntry("metadata.yml"));
            output.write(("schema: 1\nid: " + id + "\nname: " + id + "\nversion: 1.0.0\n" + extra)
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return path;
    }
}
