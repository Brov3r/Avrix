package com.avrix.benchmark;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.mixin.MixinTransformer;
import com.avrix.plugins.DefaultPluginManager;
import com.avrix.plugins.DependencyResolver;
import com.avrix.plugins.PluginManifestParser;
import com.avrix.plugins.PluginScanner;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import test.game.TargetGameService;
import test.game.TargetGameServiceMixin;
import test.loader.TestGameProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Lightweight performance benchmark for Avrix runtime paths.
 * <p>
 * Run with {@code ./gradlew benchmark}. This is intentionally not a JMH benchmark so it does not add build
 * dependencies; results are comparative and should be used for regression detection rather than absolute claims.
 */
public final class AvrixBenchmark {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASURED_ITERATIONS = 8;

    private AvrixBenchmark() {
        throw new UnsupportedOperationException("Benchmark utility cannot be instantiated");
    }

    /**
     * Runs all benchmark scenarios.
     *
     * @param args ignored
     * @throws Exception if fixture setup or cleanup fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== Avrix Benchmark ===");
        System.out.printf("Java: %s | OS: %s %s%n",
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"));
        System.out.println();

        benchmarkDependencyResolver(100);
        benchmarkDependencyResolver(500);
        benchmarkDependencyResolver(1000);

        Path pluginRoot = Files.createTempDirectory("avrix-bench-plugins-");
        try {
            benchmarkPluginLoading(pluginRoot, 20);
            benchmarkPluginLoading(pluginRoot, 50);
            benchmarkPluginLoading(pluginRoot, 100);
            benchmarkPluginLoading(pluginRoot, 250);
            benchmarkRealisticPluginLoading(pluginRoot, 50);
            benchmarkRealisticPluginLoading(pluginRoot, 100);
            benchmarkRealisticPluginLoading(pluginRoot, 250);
            benchmarkRealisticPluginLoading(pluginRoot, 500);
        } finally {
            deleteRecursively(pluginRoot);
        }

        benchmarkMixinClassLoading();
    }

    private static void benchmarkDependencyResolver(int pluginCount) {
        List<Metadata> candidates = new ArrayList<>(pluginCount);
        for (int i = 0; i < pluginCount; i++) {
            Metadata.Builder builder = new Metadata.Builder()
                    .schema(Constants.METADATA_SCHEMA)
                    .id("plugin-%04d".formatted(i))
                    .name("Plugin " + i)
                    .version("1.0.0");
            if (i > 0) {
                builder.dependencies(Map.of("plugin-%04d".formatted(i - 1), ">=1.0.0"));
            }
            candidates.add(builder.build());
        }

        DependencyResolver resolver = new DependencyResolver(List.of());
        measure("resolver chain", pluginCount, () -> resolver.resolve(candidates));
    }

    private static void benchmarkPluginLoading(Path root, int pluginCount) throws Exception {
        Path pluginsDirectory = root.resolve("plugins-" + pluginCount);
        Files.createDirectories(pluginsDirectory);
        for (int i = 0; i < pluginCount; i++) {
            createLibraryPluginJar(pluginsDirectory.resolve("plugin-%04d.jar".formatted(i)), i);
        }

        measure("load metadata-only plugins", pluginCount, () -> {
            loadPlugins(pluginsDirectory, pluginCount);
        });
    }

    private static void benchmarkRealisticPluginLoading(Path root, int pluginCount) throws Exception {
        Path pluginsDirectory = root.resolve("realistic-plugins-" + pluginCount);
        Files.createDirectories(pluginsDirectory);
        for (int i = 0; i < pluginCount; i++) {
            createRealisticPluginJar(pluginsDirectory.resolve("realistic-%04d.jar".formatted(i)), i);
        }

        measure("load realistic plugins", pluginCount, () -> {
            BenchmarkRuntime.reset();
            loadPlugins(pluginsDirectory, pluginCount);

            if (BenchmarkRuntime.completed() != pluginCount) {
                throw new IllegalStateException(
                        "Expected " + pluginCount + " plugin initializers, got " + BenchmarkRuntime.completed()
                );
            }
            if (BenchmarkRuntime.payloadBytes() == 0) {
                throw new IllegalStateException("Benchmark plugins did not read their resources");
            }
        });
    }

    private static void loadPlugins(Path pluginsDirectory, int expectedCount) {
        try (KnotClassLoader classLoader = new KnotClassLoader(new java.net.URL[0], ClassLoader.getSystemClassLoader())) {
            LoaderConfig config = new LoaderConfig(
                    pluginsDirectory,
                    Constants.METADATA_NAME,
                    Constants.PLUGIN_EXTENSION,
                    Constants.PLUGIN_ICON_NAME,
                    Constants.DEFAULT_GAME_PROVIDER_ID,
                    Environment.BOTH,
                    true,
                    false
            );

            DefaultPluginManager manager = new DefaultPluginManager(
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
            manager.loadPlugins();
            manager.close();

            long loaded = manager.getLoadResults().stream()
                    .filter(result -> result.status() == com.avrix.plugins.PluginLoadStatus.LOADED)
                    .count();
            if (loaded != expectedCount) {
                throw new IllegalStateException("Expected " + expectedCount + " loaded plugins, got " + loaded);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void benchmarkMixinClassLoading() {
        java.net.URL[] classpathUrls = classpathUrls();
        measure("class load + mixin transform", 50, () -> {
            for (int i = 0; i < 50; i++) {
                try (KnotClassLoader classLoader = new KnotClassLoader(classpathUrls, ClassLoader.getSystemClassLoader())) {
                    MixinTransformer mixinTransformer = new MixinTransformer(classLoader);
                    mixinTransformer.addMixin(TargetGameServiceMixin.class.getName());
                    classLoader.setClassTransformer(mixinTransformer);
                    Class.forName(TargetGameService.class.getName(), true, classLoader);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void measure(String name, int workload, Runnable action) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            action.run();
        }

        long[] samples = new long[MEASURED_ITERATIONS];
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            long start = System.nanoTime();
            action.run();
            samples[i] = System.nanoTime() - start;
        }

        java.util.Arrays.sort(samples);
        double medianMs = samples[samples.length / 2] / 1_000_000.0;
        double p95Ms = samples[(int) Math.min(samples.length - 1, Math.floor(samples.length * 0.95))] / 1_000_000.0;
        double opsPerSecond = medianMs > 0 ? workload / (medianMs / 1000.0) : 0;

        System.out.printf("%-28s n=%-5d median=%8.2f ms p95=%8.2f ms ops/s=%10.1f%n",
                name, workload, medianMs, p95Ms, opsPerSecond);
    }

    private static java.net.URL[] classpathUrls() {
        String classpath = System.getProperty("java.class.path", "");
        return Stream.of(classpath.split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .filter(path -> !path.isBlank())
                .map(Path::of)
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(java.net.URL[]::new);
    }

    private static void createLibraryPluginJar(Path destination, int index) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            JarEntry entry = new JarEntry(Constants.METADATA_NAME);
            jar.putNextEntry(entry);
            String yaml = """
                    schema: %d
                    id: bench-plugin-%04d
                    name: Benchmark Plugin %d
                    version: 1.0.0
                    environment: both
                    """.formatted(Constants.METADATA_SCHEMA, index, index);
            jar.write(yaml.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    private static void createRealisticPluginJar(Path destination, int index) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            addClassEntry(jar, BenchmarkPlugin.class);
            addTextEntry(jar, "payload.txt", "payload-%04d-".formatted(index) + "x".repeat(1024));

            StringBuilder yaml = new StringBuilder("""
                    schema: %d
                    id: realistic-%04d
                    name: Realistic Benchmark Plugin %d
                    version: 1.0.0
                    environment: both
                    entrypoint: com.avrix.benchmark.BenchmarkPlugin
                    """.formatted(Constants.METADATA_SCHEMA, index, index));

            if (index > 0) {
                yaml.append("dependencies:\n");
                yaml.append("  realistic-%04d: \">=1.0.0\"\n".formatted(index - 1));
            }

            JarEntry entry = new JarEntry(Constants.METADATA_NAME);
            jar.putNextEntry(entry);
            jar.write(yaml.toString().getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    private static void addClassEntry(JarOutputStream jar, Class<?> type) throws IOException {
        String path = type.getName().replace('.', '/') + ".class";
        try (var stream = type.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Class resource not found: " + path);
            }
            jar.putNextEntry(new JarEntry(path));
            jar.write(stream.readAllBytes());
            jar.closeEntry();
        }
    }

    private static void addTextEntry(JarOutputStream jar, String path, String content) throws IOException {
        jar.putNextEntry(new JarEntry(path));
        jar.write(content.getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
