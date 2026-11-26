package com.avrix.mixin;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.ServiceManager;
import com.avrix.plugins.DefaultPluginManager;
import com.avrix.plugins.PluginData;
import com.avrix.plugins.PluginManifestParser;
import com.avrix.plugins.PluginScanner;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import test.loader.TestGameProvider;
import test.mixin.game.RegistrationOnlyMixin;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that generated Mixin configurations carry the real plugin identifier as their Fabric mod ID.
 */
class MixinOwnerDecorationTest {

    @TempDir
    Path tempRoot;

    /**
     * Verifies that a mixin registered through the plugin manager is decorated with the manifest plugin ID.
     *
     * @throws Exception if the fixture archive or plugin manager cannot be created
     */
    @Test
    void shouldDecorateConfigWithPluginIdFromMetadata() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            ServiceManager.clear();
            ServiceManager.register(KnotClassLoader.class, loader);
            MixinTransformer transformer = new MixinTransformer(loader);

            Path pluginsDirectory = tempRoot.resolve("plugins");
            Files.createDirectories(pluginsDirectory);
            Path pluginJar = createArchive(pluginsDirectory.resolve("owner-metadata.jar"));

            LoaderConfig config = LoaderConfig.defaults()
                    .withPluginsDirectory(pluginsDirectory)
                    .withEnvironment(Environment.BOTH);
            DefaultPluginManager manager = new DefaultPluginManager(
                    config,
                    loader,
                    new TestGameProvider(),
                    new DefaultLoaderProvider(),
                    transformer,
                    new PluginScanner(config),
                    new PluginManifestParser()
            );

            try {
                manager.init();

                Metadata metadata = new Metadata.Builder()
                        .schema(Constants.METADATA_SCHEMA)
                        .id("owner.metadata.plugin")
                        .name("Owner metadata plugin")
                        .version("1.0.0")
                        .environment(Environment.BOTH)
                        .mixins(List.of(RegistrationOnlyMixin.class.getName()))
                        .build();

                Set<Config> configsBefore = Set.copyOf(Mixins.getConfigs());
                manager.loadPlugin(new PluginData(pluginJar, null, null, metadata));

                assertThat(FabricUtil.getModId(newConfig(configsBefore).getConfig()))
                        .isEqualTo(metadata.id());
            } finally {
                manager.close();
                ServiceManager.clear();
            }
        }
    }

    /**
     * Verifies that the legacy owner-less registration keeps Mixin's default unknown-owner label.
     *
     * @throws Exception if the test class loader cannot be closed
     */
    @Test
    void shouldKeepUnknownOwnerForLegacyRegistration() throws Exception {
        try (KnotClassLoader loader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            ServiceManager.clear();
            ServiceManager.register(KnotClassLoader.class, loader);
            MixinTransformer transformer = new MixinTransformer(loader);

            Set<Config> configsBefore = Set.copyOf(Mixins.getConfigs());
            transformer.addMixin(RegistrationOnlyMixin.class.getName());

            assertThat(FabricUtil.getModId(newConfig(configsBefore).getConfig()))
                    .isEqualTo("(unknown)");
        } finally {
            ServiceManager.clear();
        }
    }

    private static Config newConfig(Set<Config> configsBefore) {
        return Mixins.getConfigs().stream()
                .filter(config -> !configsBefore.contains(config))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No mixin configuration was registered"));
    }

    private static Path createArchive(Path destination) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            jar.putNextEntry(new JarEntry("owner-metadata.txt"));
            jar.write("owner metadata fixture".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return destination;
    }
}
