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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests classpath duplicate detection performed while plugin archives are attached.
 */
@DisplayName("DefaultPluginManager classpath conflict tests")
class DefaultPluginManagerClasspathConflictTest {

    @TempDir
    Path tempRoot;

    private Path pluginsDirectory;
    private KnotClassLoader classLoader;
    private DefaultPluginManager manager;

    @BeforeEach
    void setUp() {
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
                mixin -> {
                },
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
        }
    }

    @Test
    @DisplayName("Should report a conflict with the first attached plugin as winner")
    void shouldReportConflictWithFirstAttachedWinner() {
        createPluginJar(pluginsDirectory.resolve("alpha.jar"), "alpha", List.of("shared/Shared.class"), false);
        createPluginJar(pluginsDirectory.resolve("beta.jar"), "beta", List.of("shared/Shared.class"), false);

        manager.loadPlugins();

        assertThat(manager.getClasspathConflicts())
                .singleElement()
                .satisfies(conflict -> {
                    assertThat(conflict.resourcePath()).isEqualTo("shared/Shared.class");
                    assertThat(conflict.pluginIds()).containsExactly("alpha", "beta");
                    assertThat(conflict.winnerPluginId()).isEqualTo("alpha");
                });
    }

    @Test
    @DisplayName("Should report no conflict when plugins provide unique classes")
    void shouldReportNoConflictForUniqueClasses() {
        createPluginJar(pluginsDirectory.resolve("alpha.jar"), "alpha", List.of("alpha/Only.class"), false);
        createPluginJar(pluginsDirectory.resolve("beta.jar"), "beta", List.of("beta/Only.class"), false);

        manager.loadPlugins();

        assertThat(manager.getClasspathConflicts()).isEmpty();
    }

    @Test
    @DisplayName("Should ignore parent-delegated and infrastructure classes")
    void shouldIgnoreParentDelegatedClasses() {
        List<String> entries = List.of(
                "org/slf4j/Logger.class",
                "com/avrix/Main.class",
                "java/lang/String.class",
                "module-info.class",
                "META-INF/MANIFEST.MF"
        );
        createPluginJar(pluginsDirectory.resolve("alpha.jar"), "alpha", entries, false);
        createPluginJar(pluginsDirectory.resolve("beta.jar"), "beta", entries, false);

        manager.loadPlugins();

        assertThat(manager.getPlugins()).containsKeys("alpha", "beta");
        assertThat(manager.getClasspathConflicts()).isEmpty();
    }

    @Test
    @DisplayName("Should detect conflicts for direct loads using the attach order")
    void shouldDetectConflictForDirectLoads() {
        Path first = createPluginJar(
                tempRoot.resolve("direct-a.jar"),
                "direct-a",
                List.of("shared/Direct.class"),
                false
        );
        Path second = createPluginJar(
                tempRoot.resolve("direct-b.jar"),
                "direct-b",
                List.of("shared/Direct.class"),
                false
        );

        manager.loadPlugin(new PluginData(first, null, null, metadata("direct-a")));
        manager.loadPlugin(new PluginData(second, null, null, metadata("direct-b")));

        assertThat(manager.getClasspathConflicts())
                .singleElement()
                .satisfies(conflict -> {
                    assertThat(conflict.pluginIds()).containsExactly("direct-a", "direct-b");
                    assertThat(conflict.winnerPluginId()).isEqualTo("direct-a");
                });
    }

    @Test
    @DisplayName("Should reset conflicts at the start of a new loadPlugins run")
    void shouldResetConflictsOnNewLoad() throws IOException {
        Path alpha = createPluginJar(
                pluginsDirectory.resolve("alpha.jar"),
                "alpha",
                List.of("shared/Shared.class"),
                false
        );
        Path beta = createPluginJar(
                pluginsDirectory.resolve("beta.jar"),
                "beta",
                List.of("shared/Shared.class"),
                false
        );

        manager.loadPlugins();
        assertThat(manager.getClasspathConflicts()).isNotEmpty();

        Files.delete(beta);
        manager.loadPlugins();

        assertThat(alpha).exists();
        assertThat(manager.getClasspathConflicts()).isEmpty();
    }

    @Test
    @DisplayName("Should return an immutable snapshot sorted by resource path")
    void shouldReturnImmutableSortedSnapshot() {
        List<String> entries = List.of("z/Late.class", "a/Early.class");
        createPluginJar(pluginsDirectory.resolve("alpha.jar"), "alpha", entries, false);
        createPluginJar(pluginsDirectory.resolve("beta.jar"), "beta", entries, false);

        manager.loadPlugins();

        List<ClasspathConflict> conflicts = manager.getClasspathConflicts();
        assertThat(conflicts)
                .extracting(ClasspathConflict::resourcePath)
                .containsExactly("a/Early.class", "z/Late.class");
        assertThatThrownBy(() -> conflicts.add(
                new ClasspathConflict("b/B.class", List.of("alpha", "beta"), "alpha")
        )).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> conflicts.getFirst().pluginIds().add("gamma"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should tolerate multi-release and class-free archives")
    void shouldTolerateMultiReleaseAndClassFreeArchives() {
        createPluginJar(pluginsDirectory.resolve("empty.jar"), "empty", List.of(), false);
        createPluginJar(
                pluginsDirectory.resolve("multi-one.jar"),
                "multi-one",
                List.of("META-INF/versions/11/com/example/Foo.class"),
                true
        );
        createPluginJar(
                pluginsDirectory.resolve("multi-two.jar"),
                "multi-two",
                List.of("META-INF/versions/11/com/example/Foo.class"),
                true
        );

        manager.loadPlugins();

        assertThat(manager.getPlugins()).containsKeys("empty", "multi-one", "multi-two");
        assertThat(manager.getClasspathConflicts()).isEmpty();
    }

    private static Metadata metadata(String id) {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(id)
                .name(id)
                .version("1.0.0")
                .environment(Environment.BOTH)
                .build();
    }

    private static Path createPluginJar(
            Path destination,
            String id,
            List<String> classEntries,
            boolean multiRelease
    ) {
        try {
            Files.createDirectories(destination.getParent());
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
                if (multiRelease) {
                    jar.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                    jar.write(("Manifest-Version: 1.0\r\nMulti-Release: true\r\n\r\n")
                            .getBytes(StandardCharsets.UTF_8));
                    jar.closeEntry();
                }

                for (String classEntry : classEntries) {
                    jar.putNextEntry(new JarEntry(classEntry));
                    jar.closeEntry();
                }

                jar.putNextEntry(new JarEntry(Constants.METADATA_NAME));
                jar.write("""
                        schema: %d
                        id: %s
                        name: %s
                        version: 1.0.0
                        environment: both
                        """.formatted(Constants.METADATA_SCHEMA, id, id).getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
            return destination;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create plugin archive: " + destination, e);
        }
    }
}
