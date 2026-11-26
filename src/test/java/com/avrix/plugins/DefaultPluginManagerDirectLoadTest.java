package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;
import test.plugin.LifecyclePlugin;
import test.plugin.PhaseTracker;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests direct plugin dependency validation in {@link DefaultPluginManager}.
 */
@DisplayName("DefaultPluginManager direct load tests")
class DefaultPluginManagerDirectLoadTest {

    @TempDir
    Path tempRoot;

    private final List<String> registeredMixins = new ArrayList<>();

    private Path pluginsDirectory;
    private KnotClassLoader classLoader;
    private DefaultPluginManager manager;

    @BeforeEach
    void setUp() throws IOException {
        PhaseTracker.reset();
        pluginsDirectory = tempRoot.resolve("plugins");
        classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader());

        LoaderConfig config = LoaderConfig.defaults()
                .withPluginsDirectory(pluginsDirectory)
                .withEnvironment(Environment.BOTH);

        manager = new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                new DefaultLoaderProvider(),
                mixin -> registeredMixins.add(mixin),
                new PluginScanner(config),
                new PluginManifestParser()
        );
        manager.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        try {
            if (manager != null) {
                manager.close();
            }
        } finally {
            if (classLoader != null) {
                classLoader.close();
            }
            PhaseTracker.reset();
        }
    }

    /**
     * Verifies that a direct load with a missing dependency does not attach the archive, register mixins, or run the
     * entrypoint, and that a retry succeeds after the dependency is loaded.
     *
     * @throws Exception if a fixture archive cannot be written
     */
    @Test
    @DisplayName("Should reject a direct load before side effects and allow retry after loading the dependency")
    void shouldRejectDirectLoadBeforeSideEffects() throws Exception {
        Path baseJar = createPluginJar(pluginsDirectory.resolve("base.jar"), "base", "");
        Path dependentJar = createPluginJar(
                pluginsDirectory.resolve("dependent.jar"),
                "dependent",
                LifecyclePlugin.class.getName(),
                LifecyclePlugin.class
        );
        Metadata baseMetadata = pluginMetadata("base", "", Map.of(), List.of());
        Metadata dependentMetadata = pluginMetadata(
                "dependent",
                LifecyclePlugin.class.getName(),
                Map.of("base", "*"),
                List.of("example.DependentMixin")
        );

        Throwable failure = catchThrowable(() -> manager.loadPlugin(
                new PluginData(dependentJar, null, null, dependentMetadata)));

        assertThat(failure)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load plugin: dependent");
        assertThat(failure.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Required dependencies are not loaded: base");
        assertThat(manager.getPlugins()).doesNotContainKey("dependent");
        assertThat(PhaseTracker.events()).isEmpty();
        assertThat(registeredMixins).isEmpty();
        assertThat(hasRegisteredUrl(dependentJar)).isFalse();

        manager.loadPlugin(new PluginData(baseJar, null, null, baseMetadata));
        manager.loadPlugin(new PluginData(dependentJar, null, null, dependentMetadata));

        assertThat(manager.getPlugins()).containsKeys("base", "dependent");
        assertThat(PhaseTracker.events()).containsExactly("init:lifecycle");
        assertThat(registeredMixins).containsExactly("example.DependentMixin");
        assertThat(hasRegisteredUrl(dependentJar)).isTrue();
    }

    private boolean hasRegisteredUrl(Path jar) {
        Path expected = jar.toAbsolutePath().normalize();
        for (URL url : classLoader.getURLs()) {
            try {
                if ("file".equalsIgnoreCase(url.getProtocol())
                        && Path.of(url.toURI()).toAbsolutePath().normalize().equals(expected)) {
                    return true;
                }
            } catch (URISyntaxException ignored) {
                // Only file URLs from local plugin archives are compared.
            }
        }
        return false;
    }

    private static Metadata pluginMetadata(
            String id,
            String entrypoint,
            Map<String, String> dependencies,
            List<String> mixins
    ) {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(id)
                .name(id + " plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .entrypoint(entrypoint)
                .dependencies(dependencies)
                .mixins(mixins)
                .build();
    }

    private static Path createPluginJar(
            Path destination,
            String id,
            String entrypoint,
            Class<?>... classes
    ) throws IOException {
        Files.createDirectories(destination.getParent());

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            for (Class<?> clazz : classes) {
                String classPath = clazz.getName().replace('.', '/') + ".class";
                try (var classBytes = clazz.getClassLoader().getResourceAsStream(classPath)) {
                    if (classBytes == null) {
                        throw new IllegalStateException("Class resource not found: " + classPath);
                    }
                    jar.putNextEntry(new JarEntry(classPath));
                    jar.write(classBytes.readAllBytes());
                    jar.closeEntry();
                }
            }

            StringBuilder metadata = new StringBuilder("""
                    schema: %d
                    id: %s
                    name: %s plugin
                    version: 1.0.0
                    environment: both
                    """.formatted(Constants.METADATA_SCHEMA, id, id));
            if (entrypoint != null && !entrypoint.isBlank()) {
                metadata.append("entrypoint: ").append(entrypoint).append('\n');
            }

            jar.putNextEntry(new JarEntry(Constants.METADATA_NAME));
            jar.write(metadata.toString().getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        return destination;
    }
}
