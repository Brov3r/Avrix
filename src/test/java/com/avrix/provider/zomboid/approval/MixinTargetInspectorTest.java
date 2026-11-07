package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for mixin target inspection without class loading.
 */
class MixinTargetInspectorTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldReadTargetsFromClassLiteral() throws Exception {
        Path jar = createJar(
                "schema: 1\nid: test\nname: Test\nversion: 1.0.0\nmixins: [test.game.TargetGameServiceMixin]\n",
                "test/game/TargetGameServiceMixin.class"
        );

        MixinTargetInspector.Result result = MixinTargetInspector.inspect(
                jar,
                List.of("test.game.TargetGameServiceMixin")
        );

        assertThat(result.mixinClasses()).containsExactly("test.game.TargetGameServiceMixin");
        assertThat(result.targetClasses()).containsExactly("test.game.TargetGameService");
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void shouldReportMissingMixinClass() throws Exception {
        Path jar = createJar(
                "schema: 1\nid: test\nname: Test\nversion: 1.0.0\nmixins: [missing.Mixin]\n"
        );

        MixinTargetInspector.Result result = MixinTargetInspector.inspect(jar, List.of("missing.Mixin"));

        assertThat(result.mixinClasses()).containsExactly("missing.Mixin");
        assertThat(result.targetClasses()).isEmpty();
        assertThat(result.issues()).anyMatch(issue -> issue.contains("Mixin class not found"));
    }

    @Test
    void shouldReturnEmptyResultForPluginWithoutMixins() throws Exception {
        Path jar = createJar("schema: 1\nid: test\nname: Test\nversion: 1.0.0\n");

        MixinTargetInspector.Result result = MixinTargetInspector.inspect(jar, List.of());

        assertThat(result.mixinClasses()).isEmpty();
        assertThat(result.targetClasses()).isEmpty();
        assertThat(result.issues()).isEmpty();
    }

    private Path createJar(String metadata, String... classEntries) throws IOException {
        Path jar = tempDir.resolve("plugin.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("metadata.yml"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();

            for (String classEntry : classEntries) {
                output.putNextEntry(new JarEntry(classEntry));
                output.write(readResource(classEntry));
                output.closeEntry();
            }
        }
        return jar;
    }

    private byte[] readResource(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing test resource: " + name);
            }
            return input.readAllBytes();
        }
    }
}
