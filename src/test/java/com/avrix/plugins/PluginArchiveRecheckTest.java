package com.avrix.plugins;

import com.avrix.api.GameProvider;
import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginArchive;
import com.avrix.api.PluginSource;
import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the plugin manager checks an approved archive hash again before attaching it.
 */
@DisplayName("Plugin archive integrity tests")
class PluginArchiveRecheckTest {

    @TempDir
    Path tempRoot;

    private final MixinRegistrar mixinRegistrar = mixinName -> {
    };

    private KnotClassLoader classLoader;
    private ClassLoader originalContextClassLoader;

    @AfterEach
    void tearDown() throws IOException {
        if (classLoader != null) {
            classLoader.close();
        }
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }

    @Test
    @DisplayName("Should skip a plugin whose archive changed after approval")
    void shouldSkipArchiveChangedAfterApproval() throws IOException {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);

        Path pluginsDirectory = tempRoot.resolve("plugins");
        Path archivePath = tempRoot.resolve("recheck-plugin.jar");
        createPluginJar(archivePath);

        LoaderConfig config = new LoaderConfig(
                pluginsDirectory,
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                Constants.AUTO_PROVIDER_ID,
                Environment.BOTH,
                false,
                false
        );

        PluginSource source = () -> List.of(new PluginArchive(
                archivePath.toFile(),
                "recheck-mod",
                "Recheck Mod",
                PluginArchive.SOURCE_MOD,
                java.util.Map.of(),
                false
        ));

        PluginApprovalGate mutatingGate = new PluginApprovalGate() {
            /** {@inheritDoc} */
            @Override
            public Status evaluate(PluginArchive archive, Metadata metadata) {
                return Status.APPROVED;
            }

            /** {@inheritDoc} */
            @Override
            public Status evaluate(PluginArchive archive, Metadata metadata, String sha256) {
                try {
                    Files.writeString(archive.file().toPath(), "changed", StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot modify test archive", e);
                }
                return Status.APPROVED;
            }
        };

        DefaultPluginManager manager = new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                new DefaultLoaderProvider(),
                mixinRegistrar,
                new PluginScanner(config, List.of(source)),
                new PluginManifestParser(),
                mutatingGate
        );

        manager.init();
        manager.loadPlugins();

        assertThat(manager.getPlugins()).doesNotContainKey("recheck-plugin");
    }

    private static void createPluginJar(Path destination) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            JarEntry entry = new JarEntry(Constants.METADATA_NAME);
            jar.putNextEntry(entry);
            String yaml = """
                    schema: %d
                    id: recheck-plugin
                    name: Recheck Plugin
                    version: 1.0.0
                    environment: both
                    """.formatted(Constants.METADATA_SCHEMA);
            jar.write(yaml.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    /**
     * Minimal game provider used by the integrity test.
     */
    private static final class TestGameProvider implements GameProvider {
        /** {@inheritDoc} */
        @Override
        public String getId() {
            return "recheck-game";
        }

        /** {@inheritDoc} */
        @Override
        public void init() {
        }

        /** {@inheritDoc} */
        @Override
        public void launch(String[] args) {
        }

        /** {@inheritDoc} */
        @Override
        public String[] getLaunchArgs() {
            return new String[0];
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return "Recheck Game";
        }

        /** {@inheritDoc} */
        @Override
        public String getNormalizedVersion() {
            return "1.0.0";
        }

        /** {@inheritDoc} */
        @Override
        public String getRawVersion() {
            return "1.0.0";
        }

        /** {@inheritDoc} */
        @Override
        public Path getLaunchDirectory() {
            return Paths.get(".").toAbsolutePath().normalize();
        }

        /** {@inheritDoc} */
        @Override
        public Environment getEnvironment() {
            return Environment.BOTH;
        }

        /** {@inheritDoc} */
        @Override
        public String getEntrypoint() {
            return "com.dummy.Main";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getAuthors() {
            return List.of("Test Author");
        }

        /** {@inheritDoc} */
        @Override
        public String getLicense() {
            return "MIT";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getContacts() {
            return List.of();
        }
    }
}
