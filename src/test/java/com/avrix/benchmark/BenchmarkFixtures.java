package com.avrix.benchmark;

import com.avrix.utils.Constants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import test.benchmark.MixinPlugin;
import test.benchmark.MixinTargetTemplate;
import test.benchmark.MixinTemplate;
import test.benchmark.RealWorldPlugin;
import test.benchmark.ResourcePlugin;
import test.benchmark.SelfMixinTemplate;
import test.benchmark.SelfTargetTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Creates plugin archives used by the Avrix benchmark.
 * <p>
 * Generated entrypoint, target, and mixin classes are produced by remapping compiled test templates. This keeps every
 * synthetic plugin architecturally close to a real plugin: a unique entrypoint class, unique transformer classes, and
 * its own JAR resources.
 */
final class BenchmarkFixtures {

    private static final String GENERATED_PACKAGE = "test.benchmark.generated";
    private static final int MIXIN_TARGET_COUNT = 64;
    private static final int RESOURCE_PAYLOAD_SIZE = 1024;
    private static final int RESOURCE_CONFIG_SIZE = 512;
    private static final int RESOURCE_LANG_SIZE = 256;

    private BenchmarkFixtures() {
        throw new UnsupportedOperationException("Benchmark fixture utility cannot be instantiated");
    }

    /**
     * Creates metadata-only plugin archives.
     *
     * @param directory   destination directory
     * @param pluginCount number of archives
     * @throws IOException if an archive cannot be written
     */
    static void createLibraryPlugins(Path directory, int pluginCount) throws IOException {
        Files.createDirectories(directory);
        for (int i = 0; i < pluginCount; i++) {
            Path destination = directory.resolve("plugin-%04d.jar".formatted(i));
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
                String yaml = """
                        schema: %d
                        id: bench-plugin-%04d
                        name: Benchmark Plugin %d
                        version: 1.0.0
                        environment: both
                        """.formatted(Constants.METADATA_SCHEMA, i, i);
                addTextEntry(jar, Constants.METADATA_NAME, yaml);
            }
        }
    }

    /**
     * Creates realistic plugin archives with unique entrypoints, dependencies, and resources.
     *
     * @param directory   destination directory
     * @param pluginCount number of archives
     * @throws IOException if an archive cannot be written
     */
    static void createResourcePlugins(Path directory, int pluginCount) throws IOException {
        Files.createDirectories(directory);
        for (int i = 0; i < pluginCount; i++) {
            String entrypoint = GENERATED_PACKAGE + ".ResourcePlugin%04d".formatted(i);
            Path destination = directory.resolve("realistic-%04d.jar".formatted(i));

            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
                addGeneratedClass(jar, entrypoint, ResourcePlugin.class, Map.of());

                addTextEntry(jar, "payload.txt", "payload-%04d-".formatted(i) + "x".repeat(RESOURCE_PAYLOAD_SIZE));
                addTextEntry(jar, "config.yml", "id: realistic-%04d\n".formatted(i)
                        + "setting: " + "c".repeat(RESOURCE_CONFIG_SIZE) + "\n");
                addTextEntry(jar, "assets/lang/en_US.json",
                        "{\"message\":\"" + "l".repeat(RESOURCE_LANG_SIZE) + "\"}");

                StringBuilder yaml = new StringBuilder("""
                        schema: %d
                        id: realistic-%04d
                        name: Realistic Benchmark Plugin %d
                        version: 1.0.0
                        environment: both
                        entrypoint: %s
                        """.formatted(Constants.METADATA_SCHEMA, i, i, entrypoint));

                if (i > 0) {
                    yaml.append("dependencies:\n");
                    yaml.append("  realistic-%04d: \">=1.0.0\"\n".formatted(i - 1));
                }

                addTextEntry(jar, Constants.METADATA_NAME, yaml.toString());
            }
        }
    }

    /**
     * Creates mixin plugin archives together with the game JAR containing their transformation targets.
     *
     * @param root        root directory shared by benchmark fixtures
     * @param pluginCount number of mixin plugin archives
     * @return paths to the generated plugin directory and shared game JAR
     * @throws IOException if a fixture cannot be written
     */
    static MixinFixture createMixinPlugins(Path root, int pluginCount) throws IOException {
        Path gameJar = root.resolve("mixin-game.jar");
        List<String> targetClasses = createTargetClasses(gameJar, MIXIN_TARGET_COUNT);

        Path directory = root.resolve("mixin-plugins-" + pluginCount);
        Files.createDirectories(directory);

        for (int i = 0; i < pluginCount; i++) {
            int targetIndex = i % MIXIN_TARGET_COUNT;
            String targetClass = targetClasses.get(targetIndex);
            int expectedMixins = ((pluginCount - 1 - targetIndex) / MIXIN_TARGET_COUNT) + 1;

            String entrypoint = GENERATED_PACKAGE + ".MixinPlugin%04d".formatted(i);
            String mixinClass = GENERATED_PACKAGE + ".Mixin%04d".formatted(i);
            Path destination = directory.resolve("mixin-%04d.jar".formatted(i));

            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
                addGeneratedClass(jar, entrypoint, MixinPlugin.class, Map.of());
                addGeneratedClass(jar, mixinClass, MixinTemplate.class, Map.of(
                        internalName(MixinTargetTemplate.class), internalName(targetClass)
                ));

                String payload = "mix-%04d".formatted(i);
                addTextEntry(jar, "payload.txt", payload);
                addTextEntry(jar, "target.txt", targetClass);
                addTextEntry(jar, "expected-mixins.txt", Integer.toString(expectedMixins));

                String yaml = """
                        schema: %d
                        id: mixin-%04d
                        name: Mixin Benchmark Plugin %d
                        version: 1.0.0
                        environment: both
                        entrypoint: %s
                        mixins:
                          - %s
                        """.formatted(Constants.METADATA_SCHEMA, i, i, entrypoint, mixinClass);

                addTextEntry(jar, Constants.METADATA_NAME, yaml);
            }
        }

        return new MixinFixture(directory, gameJar);
    }

    /**
     * Creates plugins that load resources and declare mixins for both their own classes and shared game classes.
     *
     * @param root        root directory shared by benchmark fixtures
     * @param pluginCount number of real-world plugin archives
     * @return paths to the generated plugin directory and shared game JAR
     * @throws IOException if a fixture cannot be written
     */
    static RealWorldFixture createRealWorldPlugins(Path root, int pluginCount) throws IOException {
        Path gameJar = root.resolve("real-world-game.jar");
        List<String> gameTargetClasses = createTargetClasses(gameJar, MIXIN_TARGET_COUNT);

        Path directory = root.resolve("real-world-plugins-" + pluginCount);
        Files.createDirectories(directory);

        for (int i = 0; i < pluginCount; i++) {
            int gameTargetIndex = i % MIXIN_TARGET_COUNT;
            String gameTargetClass = gameTargetClasses.get(gameTargetIndex);
            int expectedGameMixins = ((pluginCount - 1 - gameTargetIndex) / MIXIN_TARGET_COUNT) + 1;

            String entrypoint = GENERATED_PACKAGE + ".RealWorldPlugin%04d".formatted(i);
            String selfTargetClass = GENERATED_PACKAGE + ".SelfTarget%04d".formatted(i);
            String selfMixinClass = GENERATED_PACKAGE + ".SelfMixin%04d".formatted(i);
            String gameMixinClass = GENERATED_PACKAGE + ".GameMixin%04d".formatted(i);
            Path destination = directory.resolve("real-world-%04d.jar".formatted(i));

            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
                addGeneratedClass(jar, entrypoint, RealWorldPlugin.class, Map.of());
                addGeneratedClass(jar, selfTargetClass, SelfTargetTemplate.class, Map.of());
                addGeneratedClass(jar, selfMixinClass, SelfMixinTemplate.class, Map.of(
                        internalName(SelfTargetTemplate.class), internalName(selfTargetClass)
                ));
                addGeneratedClass(jar, gameMixinClass, MixinTemplate.class, Map.of(
                        internalName(MixinTargetTemplate.class), internalName(gameTargetClass)
                ));

                addTextEntry(jar, "payload.txt", "payload-%04d-".formatted(i) + "x".repeat(RESOURCE_PAYLOAD_SIZE));
                addTextEntry(jar, "config.yml", "id: real-world-%04d\n".formatted(i)
                        + "setting: " + "c".repeat(RESOURCE_CONFIG_SIZE) + "\n");
                addTextEntry(jar, "assets/lang/en_US.json",
                        "{\"message\":\"" + "l".repeat(RESOURCE_LANG_SIZE) + "\"}");
                addTextEntry(jar, "self-target.txt", selfTargetClass);
                addTextEntry(jar, "game-target.txt", gameTargetClass);
                addTextEntry(jar, "expected-game-mixins.txt", Integer.toString(expectedGameMixins));

                String yaml = """
                        schema: %d
                        id: real-world-%04d
                        name: Real World Benchmark Plugin %d
                        version: 1.0.0
                        environment: both
                        entrypoint: %s
                        mixins:
                          - %s
                          - %s
                        """.formatted(
                        Constants.METADATA_SCHEMA,
                        i,
                        i,
                        entrypoint,
                        selfMixinClass,
                        gameMixinClass
                );

                addTextEntry(jar, Constants.METADATA_NAME, yaml);
            }
        }

        return new RealWorldFixture(directory, gameJar);
    }

    /**
     * Creates a game JAR containing generated mixin target classes.
     *
     * @param destination destination JAR path
     * @param targetCount number of target classes to generate
     * @return fully qualified target class names
     * @throws IOException if the JAR cannot be written
     */
    private static List<String> createTargetClasses(Path destination, int targetCount) throws IOException {
        List<String> targetClasses = new ArrayList<>(targetCount);
        for (int i = 0; i < targetCount; i++) {
            targetClasses.add(GENERATED_PACKAGE + ".Target%04d".formatted(i));
        }

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination))) {
            for (String targetClass : targetClasses) {
                addGeneratedClass(jar, targetClass, MixinTargetTemplate.class, Map.of());
            }
        }
        return List.copyOf(targetClasses);
    }

    private static void addGeneratedClass(
            JarOutputStream jar,
            String newClassName,
            Class<?> template,
            Map<String, String> additionalMappings
    ) throws IOException {
        Map<String, String> mappings = new LinkedHashMap<>(additionalMappings);
        mappings.put(internalName(template), internalName(newClassName));

        String entryName = newClassName.replace('.', '/') + ".class";
        jar.putNextEntry(new JarEntry(entryName));
        jar.write(remapClass(template, mappings));
        jar.closeEntry();
    }

    private static byte[] remapClass(Class<?> template, Map<String, String> mappings) throws IOException {
        String resource = template.getName().replace('.', '/') + ".class";
        try (InputStream in = template.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Class resource not found: " + resource);
            }

            ClassReader reader = new ClassReader(in.readAllBytes());
            ClassWriter writer = new ClassWriter(0);
            ClassRemapper remapper = new ClassRemapper(writer, new MapRemapper(mappings));
            reader.accept(remapper, 0);
            return writer.toByteArray();
        }
    }

    private static String internalName(Class<?> type) {
        return internalName(type.getName());
    }

    private static String internalName(String className) {
        return className.replace('.', '/');
    }

    private static void addTextEntry(JarOutputStream jar, String path, String content) throws IOException {
        jar.putNextEntry(new JarEntry(path));
        jar.write(content.getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
    }

    /**
     * Paths to the generated mixin plugin fixtures.
     *
     * @param pluginsDirectory directory containing mixin plugin archives
     * @param gameJar          JAR containing transformation target classes
     */
    record MixinFixture(Path pluginsDirectory, Path gameJar) {
    }

    /**
     * Paths to the generated real-world plugin fixtures.
     *
     * @param pluginsDirectory directory containing real-world plugin archives
     * @param gameJar          JAR containing shared game transformation targets
     */
    record RealWorldFixture(Path pluginsDirectory, Path gameJar) {
    }

    /**
     * ASM remapper backed by an explicit internal-name mapping.
     */
    private static final class MapRemapper extends Remapper {

        private final Map<String, String> mappings;

        private MapRemapper(Map<String, String> mappings) {
            super(Opcodes.ASM9);
            this.mappings = Map.copyOf(mappings);
        }

        /** {@inheritDoc} */
        @Override
        public String map(String internalName) {
            return mappings.getOrDefault(internalName, internalName);
        }
    }
}
