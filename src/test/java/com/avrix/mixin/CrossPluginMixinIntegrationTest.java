package com.avrix.mixin;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import com.avrix.core.ServiceManager;
import com.avrix.plugins.DefaultPluginManager;
import com.avrix.plugins.PluginData;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import test.plugin.BasePluginService;
import test.plugin.BasePluginServiceMixin;
import test.plugin.PluginAEntrypoint;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test validating cross-plugin mixin transformations
 * orchestrated by {@link DefaultPluginManager} and {@link KnotClassLoader}.
 */
@DisplayName("Cross-Plugin Mixin Transformation Integration Tests")
class CrossPluginMixinIntegrationTest {

    private static final int SCHEMA = Constants.METADATA_SCHEMA;

    @TempDir(cleanup = CleanupMode.ALWAYS)
    Path tempDir;

    private KnotClassLoader knotClassLoader;
    private DefaultPluginManager pluginManager;

    @BeforeEach
    void setUp() {
        // Disable default URL connection caching to avoid Windows file locks on temp JARs
        URLConnection.setDefaultUseCaches("jar", false);
        URLConnection.setDefaultUseCaches("file", false);

        ServiceManager.clear();
        MixinTransformer.reset();
        PluginAEntrypoint.executionResult = null;

        this.knotClassLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader());
        ServiceManager.register(KnotClassLoader.class, knotClassLoader);

        GameProvider dummyGameProvider = new DummyGameProvider();
        ServiceManager.register(GameProvider.class, dummyGameProvider);

        LoaderProvider loaderProvider = new DefaultLoaderProvider();
        ServiceManager.register(LoaderProvider.class, loaderProvider);

        MixinTransformer.init(knotClassLoader);

        this.pluginManager = new DefaultPluginManager();
        this.pluginManager.init();
    }

    @AfterEach
    void tearDown() throws Exception {
        ServiceManager.clear();
        MixinTransformer.reset();
        PluginAEntrypoint.executionResult = null;

        // Explicitly close KnotClassLoader to release Windows file handles on physical JARs
        if (knotClassLoader != null) {
            knotClassLoader.close();
            knotClassLoader = null;
        }

        // Suggest garbage collection to finalize underlying zip file handles on Windows
        System.gc();
    }

    @Test
    @DisplayName("Should successfully apply Mixin from Plugin B onto target class in Plugin A via load order")
    void shouldTransformPluginAUsingMixinFromPluginB() throws Throwable {
        // Build JAR for Plugin A (contains BasePluginService & PluginAEntrypoint)
        File pluginAJar = createPluginAJar();

        // Build JAR for Plugin B (contains BasePluginServiceMixin)
        File pluginBJar = createPluginBJar();

        // Define metadata with topological ordering (Plugin B must load before Plugin A)
        Metadata metaA = new Metadata.Builder()
                .schema(SCHEMA)
                .id("plugin-a")
                .name("Plugin A (Target)")
                .version("1.0.0")
                .entrypoint(PluginAEntrypoint.class.getName())
                .build();

        Metadata metaB = new Metadata.Builder()
                .schema(SCHEMA)
                .id("plugin-b")
                .name("Plugin B (Mixin Provider)")
                .version("1.0.0")
                .mixins(BasePluginServiceMixin.class.getName())
                .loadBefore("plugin-a") // Guarantees Plugin B registers its mixin BEFORE Plugin A loads classes
                .build();

        // Resolve topological execution order
        List<Metadata> loadOrder = pluginManager.resolvePluginLoadOrder(List.of(metaA, metaB));

        assertThat(loadOrder)
                .extracting(Metadata::id)
                .containsExactly("plugin-b", "plugin-a");

        // Load Plugin B first (attaches JAR to KnotClassLoader and registers MixinTransformer)
        PluginData containerB = new PluginData(pluginBJar, null, null, metaB);
        pluginManager.loadPlugin(containerB);

        // Load Plugin A second (attaches JAR, instantiates entrypoint in KnotClassLoader, applies mixin)
        PluginData containerA = new PluginData(pluginAJar, null, null, metaA);
        pluginManager.loadPlugin(containerA);

        // Extract execution result from KnotClassLoader's defined PluginAEntrypoint
        Class<?> loadedEntrypointClass = knotClassLoader.loadClass(PluginAEntrypoint.class.getName());
        Field resultField = loadedEntrypointClass.getField("executionResult");
        String runtimeResult = (String) resultField.get(null);

        assertThat(runtimeResult)
                .isEqualTo("INTERCEPTED_BY_PLUGIN_B: InputData")
                .doesNotContain("ORIGINAL_A");

        // Direct reflective invocation through KnotClassLoader
        Class<?> transformedServiceClass = knotClassLoader.loadClass(BasePluginService.class.getName());
        Object serviceInstance = transformedServiceClass.getDeclaredConstructor().newInstance();

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle calculateHandle = lookup.findVirtual(
                transformedServiceClass,
                "calculateValue",
                MethodType.methodType(String.class, String.class)
        );

        String reflectiveResult = (String) calculateHandle.invoke(serviceInstance, "DirectCall");
        assertThat(reflectiveResult).isEqualTo("INTERCEPTED_BY_PLUGIN_B: DirectCall");
    }

    private File createPluginAJar() throws Exception {
        var jarPath = tempDir.resolve("plugin-a-1.0.0.jar");
        try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addClassEntry(jos, BasePluginService.class);
            addClassEntry(jos, PluginAEntrypoint.class);
            addManifest(jos);
        }
        return jarPath.toFile();
    }

    private File createPluginBJar() throws Exception {
        var jarPath = tempDir.resolve("plugin-b-1.0.0.jar");
        try (var jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            addClassEntry(jos, BasePluginServiceMixin.class);
            addManifest(jos);
        }
        return jarPath.toFile();
    }

    private void addClassEntry(JarOutputStream jos, Class<?> clazz) throws Exception {
        String resourcePath = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                jos.putNextEntry(new JarEntry(resourcePath));
                jos.write(is.readAllBytes());
                jos.closeEntry();
            }
        }
    }

    private void addManifest(JarOutputStream jos) throws Exception {
        jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
        jos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }

    private static final class DummyGameProvider implements GameProvider {
        @Override
        public String getId() {
            return "dummy-game";
        }

        @Override
        public void init() {
        }

        @Override
        public void launch(String[] args) {
        }

        @Override
        public String[] getLaunchArgs() {
            return new String[0];
        }

        @Override
        public String getName() {
            return "Dummy Game";
        }

        @Override
        public String getNormalizedVersion() {
            return "1.0.0";
        }

        @Override
        public String getRawVersion() {
            return "1.0.0";
        }

        @Override
        public Path getLaunchDirectory() {
            return Paths.get(".").toAbsolutePath().normalize();
        }

        @Override
        public Environment getEnvironment() {
            return Environment.BOTH;
        }

        @Override
        public String getEntrypoint() {
            return "com.dummy.Main";
        }

        @Override
        public List<String> getAuthors() {
            return List.of("Author");
        }

        @Override
        public String getLicense() {
            return "MIT";
        }

        @Override
        public List<String> getContacts() {
            return List.of();
        }
    }
}