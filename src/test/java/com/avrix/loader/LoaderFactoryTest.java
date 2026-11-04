package com.avrix.loader;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.ServiceManager;
import com.avrix.plugins.PluginManager;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.provider.GameProvider;
import com.avrix.provider.LoaderProvider;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link LoaderFactory} runtime composition.
 * <p>
 * {@link LoaderFactory} discovers providers through the JVM system class loader. Gradle test workers keep the test
 * classpath in a child class loader, so the full wiring scenario runs in a forked JVM when the in-process system class
 * loader cannot see the test provider.
 */
@DisplayName("LoaderFactory Unit Tests")
class LoaderFactoryTest {

    @TempDir
    Path tempDir;

    private ClassLoader originalContextClassLoader;
    private String originalCacheDirectory;

    @BeforeEach
    void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        originalCacheDirectory = System.getProperty(Constants.CACHE_DIR_PROPERTY);
        System.setProperty(Constants.CACHE_DIR_PROPERTY, tempDir.resolve("cache").toString());
        ServiceManager.clear();
        TestGameProvider.reset();
    }

    @AfterEach
    void tearDown() {
        try {
            ServiceManager.clear();
            TestGameProvider.reset();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            restoreProperty(Constants.CACHE_DIR_PROPERTY, originalCacheDirectory);
        }
    }

    @Test
    @DisplayName("Should create, initialize, and close the runtime with the selected provider")
    void shouldCreateInitializeAndCloseRuntime() throws Exception {
        Path pluginsDirectory = tempDir.resolve("plugins");

        ScenarioResult result = canRunInProcess()
                ? runScenario(pluginsDirectory)
                : runScenarioInChild(pluginsDirectory);

        assertThat(result.initCalls()).isEqualTo(1);
        assertThat(result.redirectCalls()).isEqualTo(1);
        assertThat(result.launchCalls()).isEqualTo(1);
        assertThat(result.launchArg()).isEqualTo("-game");
        assertThat(result.gameName()).isEqualTo("Test Game");
        assertThat(result.contextClassLoaderIsKnot()).isTrue();
        assertThat(result.gameProviderClassName()).isEqualTo(TestGameProvider.class.getName());
        assertThat(result.loaderProviderClassName()).isEqualTo(DefaultLoaderProvider.class.getName());
        assertThat(result.pluginManagerClosed()).isTrue();
        assertThat(result.secondCloseSafe()).isTrue();
        assertThat(result.pluginsDirectoryCreated()).isTrue();
    }

    @Test
    @DisplayName("Should reject a null loader configuration")
    void shouldRejectNullConfiguration() {
        assertThatThrownBy(() -> LoaderFactory.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Loader config");
    }

    @Test
    @DisplayName("Should roll back services and the context class loader when provider discovery fails")
    void shouldRollBackWhenProviderDiscoveryFails() {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LoaderConfig config = new LoaderConfig(
                tempDir.resolve("plugins"),
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                "missing-provider",
                Environment.AUTO,
                true,
                false
        );

        assertThatThrownBy(() -> LoaderFactory.create(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No GameProvider registered");

        assertThat(ServiceManager.getRegisteredTypes()).isEmpty();
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(previous);
    }

    @Test
    @DisplayName("Should roll back services when provider initialization fails")
    void shouldRollBackWhenProviderInitializationFails() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                canRunInProcess(),
                "Test provider is not visible through the system class loader"
        );

        TestGameProvider.failInit = true;
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        LoaderConfig config = new LoaderConfig(
                tempDir.resolve("plugins"),
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                TestGameProvider.ID,
                Environment.AUTO,
                true,
                false
        );

        assertThatThrownBy(() -> LoaderFactory.create(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("simulated provider init failure");

        assertThat(ServiceManager.getRegisteredTypes()).isEmpty();
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(previous);
        assertThat(TestGameProvider.closeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should resolve AUTO environment from the active provider")
    void shouldResolveAutoEnvironmentFromProvider() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                canRunInProcess(),
                "Test provider is not visible through the system class loader"
        );

        TestGameProvider.environment = Environment.CLIENT;
        Path pluginsDirectory = tempDir.resolve("auto-plugins");
        writePluginJar(pluginsDirectory.resolve("client-mod.jar"), "client-mod", Environment.CLIENT);
        writePluginJar(pluginsDirectory.resolve("server-mod.jar"), "server-mod", Environment.SERVER);

        LoaderConfig config = new LoaderConfig(
                pluginsDirectory,
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                TestGameProvider.ID,
                Environment.AUTO,
                true,
                false
        );

        LoaderContext context = LoaderFactory.create(config);
        try {
            context.initialize();
            PluginManager pluginManager = ServiceManager.get(PluginManager.class);

            assertThat(pluginManager.getPlugins())
                    .containsKey("client-mod")
                    .doesNotContainKey("server-mod");
        } finally {
            context.close();
        }
    }

    @Test
    @DisplayName("Should unregister loader-owned services when the context is closed")
    void shouldUnregisterServicesWhenContextIsClosed() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                canRunInProcess(),
                "Test provider is not visible through the system class loader"
        );

        LoaderConfig config = new LoaderConfig(
                tempDir.resolve("plugins"),
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                TestGameProvider.ID,
                Environment.AUTO,
                true,
                false
        );

        LoaderContext context = LoaderFactory.create(config);
        context.close();

        assertThat(ServiceManager.getRegisteredTypes()).isEmpty();
    }

    @Test
    @DisplayName("Should prevent instantiation of the factory")
    void shouldPreventInstantiation() throws NoSuchMethodException {
        Constructor<LoaderFactory> constructor = LoaderFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    private static ScenarioResult runScenario(Path pluginsDirectory) {
        ServiceManager.clear();
        TestGameProvider.reset();

        LoaderConfig config = new LoaderConfig(
                pluginsDirectory,
                Constants.METADATA_NAME,
                Constants.PLUGIN_EXTENSION,
                Constants.PLUGIN_ICON_NAME,
                TestGameProvider.ID,
                Environment.AUTO,
                true,
                false
        );

        LoaderContext context = LoaderFactory.create(config);
        KnotClassLoader classLoader = ServiceManager.get(KnotClassLoader.class);
        PluginManager pluginManager = ServiceManager.get(PluginManager.class);
        GameProvider gameProvider = ServiceManager.get(GameProvider.class);
        LoaderProvider loaderProvider = ServiceManager.get(LoaderProvider.class);

        boolean contextClassLoaderIsKnot = Thread.currentThread().getContextClassLoader() == classLoader;
        int initCalls = TestGameProvider.initCalls;

        try {
            context.initialize();
            boolean pluginsDirectoryCreated = Files.isDirectory(pluginsDirectory);

            context.redirectSystemStreams();
            context.launch(new String[]{"-game"});

            String launchArg = TestGameProvider.lastLaunchArgs.length > 0
                    ? TestGameProvider.lastLaunchArgs[0]
                    : null;
            String gameName = context.getGameName();

            context.close();
            boolean pluginManagerClosed = isPluginManagerClosed(pluginManager);
            context.close();

            return new ScenarioResult(
                    initCalls,
                    TestGameProvider.redirectCalls,
                    TestGameProvider.launchCalls,
                    launchArg,
                    gameName,
                    contextClassLoaderIsKnot,
                    gameProvider.getClass().getName(),
                    loaderProvider.getClass().getName(),
                    pluginManagerClosed,
                    true,
                    pluginsDirectoryCreated
            );
        } finally {
            context.close();
        }
    }

    private static boolean isPluginManagerClosed(PluginManager pluginManager) {
        try {
            pluginManager.init();
            return false;
        } catch (IllegalStateException exception) {
            return exception.getMessage() != null && exception.getMessage().contains("closed");
        }
    }

    private static void writePluginJar(Path jarPath, String id, Environment environment) throws IOException {
        Files.createDirectories(jarPath.getParent());

        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            outputStream.putNextEntry(new JarEntry(Constants.METADATA_NAME));
            String metadata = """
                    schema: %d
                    id: %s
                    name: %s
                    version: 1.0.0
                    environment: %s
                    """.formatted(
                    Constants.METADATA_SCHEMA,
                    id,
                    id,
                    environment.getValue()
            );
            outputStream.write(metadata.getBytes(StandardCharsets.UTF_8));
            outputStream.closeEntry();
        }
    }

    private static boolean canRunInProcess() {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try {
            Class.forName(TestGameProvider.class.getName(), false, systemClassLoader);
            return systemClassLoader.getResource("META-INF/services/" + GameProvider.class.getName()) != null;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private ScenarioResult runScenarioInChild(Path pluginsDirectory) throws Exception {
        Path resultFile = tempDir.resolve("loader-factory-result.properties");
        Path outputFile = tempDir.resolve("loader-factory-child.log");

        List<String> command = List.of(
                javaExecutable(),
                "-D" + Constants.CACHE_DIR_PROPERTY + "=" + tempDir.resolve("child-cache"),
                "-cp",
                buildChildClasspath(),
                ChildMain.class.getName(),
                pluginsDirectory.toString(),
                resultFile.toString()
        );

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(outputFile.toFile());

        Process process = processBuilder.start();
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            fail("LoaderFactory child JVM did not finish within 120 seconds");
        }

        String output = Files.exists(outputFile)
                ? Files.readString(outputFile, StandardCharsets.UTF_8)
                : "";

        assertThat(process.exitValue())
                .as("LoaderFactory child JVM output:%n%s", output)
                .isZero();
        assertThat(resultFile).exists();

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(resultFile)) {
            properties.load(inputStream);
        }
        return ScenarioResult.fromProperties(properties);
    }

    private static String buildChildClasspath() {
        Set<String> entries = new LinkedHashSet<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (classLoader != null && !(classLoader instanceof URLClassLoader)) {
            classLoader = classLoader.getParent();
        }

        if (classLoader instanceof URLClassLoader urlClassLoader) {
            for (URL url : urlClassLoader.getURLs()) {
                addClasspathEntry(entries, url);
            }
        }

        addCodeSource(entries, LoaderFactoryTest.class);
        addCodeSource(entries, TestGameProvider.class);
        addCodeSource(entries, GameProvider.class);
        addCodeSource(entries, Constants.class);

        if (entries.isEmpty()) {
            String javaClassPath = System.getProperty("java.class.path");
            if (javaClassPath != null) {
                entries.addAll(List.of(javaClassPath.split(Pattern.quote(File.pathSeparator))));
            }
        }

        return String.join(File.pathSeparator, entries);
    }

    private static void addCodeSource(Set<String> entries, Class<?> type) {
        var codeSource = type.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            addClasspathEntry(entries, codeSource.getLocation());
        }
    }

    private static void addClasspathEntry(Set<String> entries, URL url) {
        if (!"file".equalsIgnoreCase(url.getProtocol())) {
            return;
        }

        try {
            entries.add(Path.of(url.toURI()).toString());
        } catch (Exception ignored) {
            // Malformed classpath entries cannot be forwarded to the child JVM.
        }
    }

    private static String javaExecutable() {
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
        String executable = windows ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    /**
     * Supplies the serialized result of the forked wiring scenario.
     *
     * @param initCalls                 number of provider init calls
     * @param redirectCalls             number of stream redirection calls
     * @param launchCalls               number of game launch calls
     * @param launchArg                 first game launch argument
     * @param gameName                  game name reported by the context
     * @param contextClassLoaderIsKnot  whether the context class loader is the runtime class loader
     * @param gameProviderClassName     selected game provider class name
     * @param loaderProviderClassName   selected loader provider class name
     * @param pluginManagerClosed       whether closing the context closed the plugin manager
     * @param secondCloseSafe           whether a repeated close returned normally
     * @param pluginsDirectoryCreated   whether initialization created the plugins directory
     */
    private record ScenarioResult(
            int initCalls,
            int redirectCalls,
            int launchCalls,
            String launchArg,
            String gameName,
            boolean contextClassLoaderIsKnot,
            String gameProviderClassName,
            String loaderProviderClassName,
            boolean pluginManagerClosed,
            boolean secondCloseSafe,
            boolean pluginsDirectoryCreated
    ) {

        private Properties toProperties() {
            Properties properties = new Properties();
            properties.setProperty("initCalls", Integer.toString(initCalls));
            properties.setProperty("redirectCalls", Integer.toString(redirectCalls));
            properties.setProperty("launchCalls", Integer.toString(launchCalls));
            properties.setProperty("launchArg", launchArg == null ? "" : launchArg);
            properties.setProperty("gameName", gameName);
            properties.setProperty("contextClassLoaderIsKnot", Boolean.toString(contextClassLoaderIsKnot));
            properties.setProperty("gameProviderClassName", gameProviderClassName);
            properties.setProperty("loaderProviderClassName", loaderProviderClassName);
            properties.setProperty("pluginManagerClosed", Boolean.toString(pluginManagerClosed));
            properties.setProperty("secondCloseSafe", Boolean.toString(secondCloseSafe));
            properties.setProperty("pluginsDirectoryCreated", Boolean.toString(pluginsDirectoryCreated));
            return properties;
        }

        private static ScenarioResult fromProperties(Properties properties) {
            String launchArg = properties.getProperty("launchArg", "");
            return new ScenarioResult(
                    Integer.parseInt(properties.getProperty("initCalls")),
                    Integer.parseInt(properties.getProperty("redirectCalls")),
                    Integer.parseInt(properties.getProperty("launchCalls")),
                    launchArg.isEmpty() ? null : launchArg,
                    properties.getProperty("gameName"),
                    Boolean.parseBoolean(properties.getProperty("contextClassLoaderIsKnot")),
                    properties.getProperty("gameProviderClassName"),
                    properties.getProperty("loaderProviderClassName"),
                    Boolean.parseBoolean(properties.getProperty("pluginManagerClosed")),
                    Boolean.parseBoolean(properties.getProperty("secondCloseSafe")),
                    Boolean.parseBoolean(properties.getProperty("pluginsDirectoryCreated"))
            );
        }
    }

    /**
     * Child JVM entry point used when the Gradle system class loader cannot see the test classpath.
     */
    public static final class ChildMain {

        /**
         * Prevents instantiation of the child entrypoint.
         *
         * @throws UnsupportedOperationException always
         */
        private ChildMain() {
            throw new UnsupportedOperationException("Child entrypoint cannot be instantiated");
        }

        /**
         * Runs the loader wiring scenario and writes its result.
         *
         * @param args plugins directory path followed by the result file path
         * @throws Exception if the scenario or result serialization fails
         */
        public static void main(String[] args) throws Exception {
            if (args.length != 2) {
                throw new IllegalArgumentException("Expected plugins directory and result file paths");
            }

            ScenarioResult result = runScenario(Path.of(args[0]));

            try (OutputStream outputStream = Files.newOutputStream(Path.of(args[1]))) {
                result.toProperties().store(outputStream, "LoaderFactory scenario result");
            }
        }
    }
}
