package com.avrix.benchmark;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.LoaderConfig;
import com.avrix.core.Metadata;
import com.avrix.core.MixinRegistrar;
import com.avrix.mixin.MixinTransformer;
import com.avrix.plugins.DefaultPluginManager;
import com.avrix.plugins.DependencyResolver;
import com.avrix.plugins.PluginLoadStatus;
import com.avrix.plugins.PluginManifestParser;
import com.avrix.plugins.PluginScanner;
import com.avrix.provider.DefaultLoaderProvider;
import com.avrix.utils.Constants;
import test.game.TargetGameService;
import test.game.TargetGameServiceMixin;
import test.loader.TestGameProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Lightweight performance benchmark for Avrix runtime paths.
 * <p>
 * Run with {@code ./gradlew benchmark} for the full suite, or {@code ./gradlew benchmark -PpluginCount=1000} to run
 * every plugin-loading scenario at a fixed plugin count. This is intentionally not a JMH benchmark so it does not add
 * build dependencies; results are comparative and should be used for regression detection rather than absolute claims.
 * <p>
 * The realistic scenarios include unique entrypoint classes, JAR resources, dependency chains, plugin-local mixins,
 * and game-code mixins followed by actual bytecode transformation.
 */
public final class AvrixBenchmark {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASURED_ITERATIONS = 8;

    private AvrixBenchmark() {
        throw new UnsupportedOperationException("Benchmark utility cannot be instantiated");
    }

    /**
     * Runs the full benchmark suite, or a fixed-count run when a plugin count is supplied.
     * <p>
     * Usage: no arguments for the full suite, or a positive plugin count such as {@code 1000}.
     * The {@code --plugins=<count>} form is accepted as well.
     *
     * @param args optional plugin count
     * @throws Exception if fixture setup or cleanup fails
     */
    public static void main(String[] args) throws Exception {
        printHeader();

        if (args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printUsage();
            return;
        }

        Integer requestedCount = parsePluginCount(args);
        if (requestedCount == null) {
            runFullBenchmark();
        } else {
            runCountBenchmark(requestedCount);
        }
    }

    private static void runFullBenchmark() throws Exception {
        benchmarkDependencyResolver(100);
        benchmarkDependencyResolver(500);
        benchmarkDependencyResolver(1000);

        Path pluginRoot = Files.createTempDirectory("avrix-bench-plugins-");
        try {
            benchmarkPluginLoading(pluginRoot, 20);
            benchmarkPluginLoading(pluginRoot, 100);
            benchmarkPluginLoading(pluginRoot, 250);
            benchmarkPluginLoading(pluginRoot, 500);
            benchmarkPluginLoading(pluginRoot, 1000);

            benchmarkResourcePluginLoading(pluginRoot, 50);
            benchmarkResourcePluginLoading(pluginRoot, 100);
            benchmarkResourcePluginLoading(pluginRoot, 250);
            benchmarkResourcePluginLoading(pluginRoot, 500);
            benchmarkResourcePluginLoading(pluginRoot, 1000);

            benchmarkRealWorldPluginLoading(pluginRoot, 50);
            benchmarkRealWorldPluginLoading(pluginRoot, 100);
            benchmarkRealWorldPluginLoading(pluginRoot, 250);
            benchmarkRealWorldPluginLoading(pluginRoot, 500);
            benchmarkRealWorldPluginLoading(pluginRoot, 1000);

            benchmarkMixinPluginLoading(pluginRoot, 50);
            benchmarkMixinPluginLoading(pluginRoot, 100);
            benchmarkMixinPluginLoading(pluginRoot, 250);
            benchmarkMixinPluginLoading(pluginRoot, 500);
            benchmarkMixinPluginLoading(pluginRoot, 1000);
        } finally {
            deleteRecursively(pluginRoot);
        }

        benchmarkMixinClassLoading(50);
        benchmarkMixinClassLoading(250);
        benchmarkMixinClassLoading(1000);
    }

    private static void runCountBenchmark(int pluginCount) throws Exception {
        System.out.printf(Locale.ROOT, "Mode: plugin count = %d (all plugin-loading scenarios)%n%n", pluginCount);
        benchmarkDependencyResolver(pluginCount);

        Path pluginRoot = Files.createTempDirectory("avrix-bench-plugins-");
        try {
            benchmarkPluginLoading(pluginRoot, pluginCount);
            benchmarkResourcePluginLoading(pluginRoot, pluginCount);
            benchmarkRealWorldPluginLoading(pluginRoot, pluginCount);
            benchmarkMixinPluginLoading(pluginRoot, pluginCount);
        } finally {
            deleteRecursively(pluginRoot);
        }
    }

    private static Integer parsePluginCount(String[] args) {
        if (args.length == 0) {
            return null;
        }
        if (args.length > 1) {
            throw new IllegalArgumentException("Expected at most one plugin count argument." + System.lineSeparator()
                    + usage());
        }

        String value = args[0].strip();
        String prefix = "--plugins=";
        if (value.startsWith(prefix)) {
            value = value.substring(prefix.length()).strip();
        }

        try {
            int pluginCount = Integer.parseInt(value);
            if (pluginCount <= 0) {
                throw new IllegalArgumentException("Plugin count must be positive: " + pluginCount);
            }
            return pluginCount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid plugin count: " + value + System.lineSeparator() + usage());
        }
    }

    private static void printHeader() {
        System.out.println("=== Avrix Benchmark ===");
        System.out.printf(Locale.ROOT, "Java: %s | OS: %s %s%n",
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"));
        System.out.println();
    }

    private static void printUsage() {
        System.out.println(usage());
    }

    private static String usage() {
        return """
                Usage:
                  ./gradlew benchmark                    # full suite
                  ./gradlew benchmark --args="1000"      # all plugin-loading scenarios at 1000 plugins
                  ./gradlew benchmark -PpluginCount=1000 # same, via a Gradle property
                """;
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
        BenchmarkFixtures.createLibraryPlugins(pluginsDirectory, pluginCount);

        measure("load metadata-only plugins", pluginCount, () -> {
            loadPlugins(pluginsDirectory, pluginCount, new URL[0], false);
        });
    }

    private static void benchmarkResourcePluginLoading(Path root, int pluginCount) throws Exception {
        Path pluginsDirectory = root.resolve("realistic-plugins-" + pluginCount);
        BenchmarkFixtures.createResourcePlugins(pluginsDirectory, pluginCount);

        measure("load resource plugins", pluginCount, () -> {
            BenchmarkRuntime.reset();
            loadPlugins(pluginsDirectory, pluginCount, new URL[0], false);

            if (BenchmarkRuntime.resourcePlugins() != pluginCount) {
                throw new IllegalStateException(
                        "Expected " + pluginCount + " resource plugin initializers, got "
                                + BenchmarkRuntime.resourcePlugins()
                );
            }
            if (BenchmarkRuntime.resourceBytes() == 0) {
                throw new IllegalStateException("Benchmark plugins did not read their resources");
            }
        });
    }

    private static void benchmarkMixinPluginLoading(Path root, int pluginCount) throws Exception {
        BenchmarkFixtures.MixinFixture fixture = BenchmarkFixtures.createMixinPlugins(root, pluginCount);
        URL[] initialUrls = {fixture.gameJar().toUri().toURL()};

        measure("load mixin plugins", pluginCount, () -> {
            BenchmarkRuntime.reset();
            loadPlugins(fixture.pluginsDirectory(), pluginCount, initialUrls, true);

            if (BenchmarkRuntime.mixinPlugins() != pluginCount) {
                throw new IllegalStateException(
                        "Expected " + pluginCount + " mixin plugin initializers, got "
                                + BenchmarkRuntime.mixinPlugins()
                );
            }
            if (BenchmarkRuntime.mixinTraceEntries() == 0) {
                throw new IllegalStateException("Benchmark mixins did not transform their targets");
            }
        });
    }

    private static void benchmarkRealWorldPluginLoading(Path root, int pluginCount) throws Exception {
        BenchmarkFixtures.RealWorldFixture fixture = BenchmarkFixtures.createRealWorldPlugins(root, pluginCount);
        URL[] initialUrls = {fixture.gameJar().toUri().toURL()};

        measure("load real-world plugins", pluginCount, () -> {
            BenchmarkRuntime.reset();
            loadPlugins(fixture.pluginsDirectory(), pluginCount, initialUrls, true);

            if (BenchmarkRuntime.realWorldPlugins() != pluginCount) {
                throw new IllegalStateException(
                        "Expected " + pluginCount + " real-world plugin initializers, got "
                                + BenchmarkRuntime.realWorldPlugins()
                );
            }
            if (BenchmarkRuntime.realWorldResourceBytes() == 0) {
                throw new IllegalStateException("Real-world plugins did not read their resources");
            }
            if (BenchmarkRuntime.selfMixinApplications() != pluginCount) {
                throw new IllegalStateException(
                        "Expected " + pluginCount + " self mixin applications, got "
                                + BenchmarkRuntime.selfMixinApplications()
                );
            }
            if (BenchmarkRuntime.gameMixinApplications() < pluginCount) {
                throw new IllegalStateException(
                        "Expected at least " + pluginCount + " game mixin applications, got "
                                + BenchmarkRuntime.gameMixinApplications()
                );
            }
        });
    }

    private static void loadPlugins(
            Path pluginsDirectory,
            int expectedCount,
            URL[] initialUrls,
            boolean transformMixins
    ) {
        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], ClassLoader.getSystemClassLoader())) {
            for (URL initialUrl : initialUrls) {
                classLoader.addURL(initialUrl);
            }

            MixinRegistrar mixinRegistrar = mixin -> {
            };
            if (transformMixins) {
                MixinTransformer mixinTransformer = new MixinTransformer(classLoader);
                classLoader.setClassTransformer(mixinTransformer);
                mixinRegistrar = mixinTransformer;
            }

            LoaderConfig config = new LoaderConfig(
                    pluginsDirectory,
                    Constants.METADATA_NAME,
                    Constants.PLUGIN_EXTENSION,
                    Constants.PLUGIN_ICON_NAME,
                    Constants.AUTO_PROVIDER_ID,
                    Environment.BOTH,
                    true,
                    false
            );

            DefaultPluginManager manager = new DefaultPluginManager(
                    config,
                    classLoader,
                    new TestGameProvider(),
                    new DefaultLoaderProvider(),
                    mixinRegistrar,
                    new PluginScanner(config),
                    new PluginManifestParser()
            );

            manager.init();
            manager.loadPlugins();
            manager.close();

            List<com.avrix.plugins.PluginLoadResult> results = manager.getLoadResults();
            long loaded = results.stream()
                    .filter(result -> result.status() == PluginLoadStatus.LOADED)
                    .count();
            long failed = results.stream()
                    .filter(result -> result.status() == PluginLoadStatus.FAILED)
                    .count();

            if (failed != 0) {
                throw new IllegalStateException("Expected no failed plugins, got " + failed);
            }
            if (loaded != expectedCount) {
                throw new IllegalStateException("Expected " + expectedCount + " loaded plugins, got " + loaded);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void benchmarkMixinClassLoading(int iterations) {
        URL[] classpathUrls = classpathUrls();
        measure("class load + mixin transform", iterations, () -> {
            for (int i = 0; i < iterations; i++) {
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

        Arrays.sort(samples);
        double medianMs = samples[samples.length / 2] / 1_000_000.0;
        double p95Ms = samples[(int) Math.min(samples.length - 1, Math.floor(samples.length * 0.95))] / 1_000_000.0;
        double opsPerSecond = medianMs > 0 ? workload / (medianMs / 1000.0) : 0;

        System.out.printf(Locale.ROOT, "%-28s n=%-5d median=%8.2f ms p95=%8.2f ms ops/s=%10.1f%n",
                name, workload, medianMs, p95Ms, opsPerSecond);
    }

    private static URL[] classpathUrls() {
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
                .toArray(URL[]::new);
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
