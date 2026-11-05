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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Tests that pending approvals can be resolved and loaded in the same plugin-manager run.
 */
@DisplayName("Pending approval retry tests")
class PendingApprovalRetryTest {

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
    @DisplayName("Should load a pending plugin after flushPending resolves it")
    void shouldLoadPendingPluginAfterFlush() throws IOException {
        DefaultPluginManager manager = createManager(gateResolvingAfterFlush());

        manager.init();
        manager.loadPlugins();

        assertThat(manager.getPlugins()).containsKey("pending-plugin");
        assertThat(manager.getLoadResults())
                .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                .contains(tuple("pending-plugin", PluginLoadStatus.LOADED));
    }

    @Test
    @DisplayName("Should skip a plugin that remains pending after flushPending")
    void shouldSkipPluginThatRemainsPending() throws IOException {
        PluginApprovalGate pendingGate = new PluginApprovalGate() {
            /** {@inheritDoc} */
            @Override
            public Status evaluate(PluginArchive archive, Metadata metadata) {
                return Status.PENDING;
            }
        };
        DefaultPluginManager manager = createManager(pendingGate);

        manager.init();
        manager.loadPlugins();

        assertThat(manager.getPlugins()).doesNotContainKey("pending-plugin");
        assertThat(manager.getLoadResults())
                .extracting(PluginLoadResult::pluginId, PluginLoadResult::status)
                .contains(tuple("pending-plugin", PluginLoadStatus.SKIPPED));
    }

    private PluginApprovalGate gateResolvingAfterFlush() {
        AtomicBoolean flushed = new AtomicBoolean();
        return new PluginApprovalGate() {
            /** {@inheritDoc} */
            @Override
            public Status evaluate(PluginArchive archive, Metadata metadata) {
                return flushed.get() ? Status.APPROVED : Status.PENDING;
            }

            /** {@inheritDoc} */
            @Override
            public Status evaluate(PluginArchive archive, Metadata metadata, String sha256) {
                return evaluate(archive, metadata);
            }

            /** {@inheritDoc} */
            @Override
            public void flushPending() {
                flushed.set(true);
            }
        };
    }

    private DefaultPluginManager createManager(PluginApprovalGate gate) throws IOException {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);

        Path pluginsDirectory = tempRoot.resolve("plugins");
        Path archivePath = tempRoot.resolve("pending-plugin.jar");
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
                "pending-mod",
                "Pending Mod",
                PluginArchive.SOURCE_MOD,
                Map.of(),
                false
        ));

        return new DefaultPluginManager(
                config,
                classLoader,
                new TestGameProvider(),
                new DefaultLoaderProvider(),
                mixinRegistrar,
                new PluginScanner(config, List.of(source)),
                new PluginManifestParser(),
                gate
        );
    }

    private static void createPluginJar(Path destination) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            JarEntry entry = new JarEntry(Constants.METADATA_NAME);
            jar.putNextEntry(entry);
            String yaml = """
                    schema: %d
                    id: pending-plugin
                    name: Pending Plugin
                    version: 1.0.0
                    environment: both
                    """.formatted(Constants.METADATA_SCHEMA);
            jar.write(yaml.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    /**
     * Minimal game provider used by the pending approval tests.
     */
    private static final class TestGameProvider implements GameProvider {
        /** {@inheritDoc} */
        @Override
        public String getId() {
            return "pending-game";
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
            return "Pending Game";
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
