package com.avrix.plugins;

import com.avrix.core.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies mandatory YAML safety checks and strict environment validation.
 */
public class ManifestSafetyTest {

    private static final String HEADER = "schema: 1\nid: safe-plugin\nname: Safe Plugin\nversion: 1.0.0\n";

    @TempDir
    private Path directory;

    /**
     * Verifies that nested duplicate keys are rejected in both supported file formats.
     *
     * @param jar whether the manifest is stored inside a JAR
     * @throws IOException if fixture files cannot be created
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldRejectNestedDuplicateKeys(boolean jar) throws IOException {
        Path source = write(HEADER + "dependencies:\n  library: '*'\n  library: '>=1.0.0'\n", jar);
        assertThatThrownBy(() -> parse(new PluginManifestParser(), source, jar))
                .isInstanceOf(IOException.class).hasMessageContaining("duplicate key");
    }

    /**
     * Verifies strict rejection while preserving the existing lenient constructor contract.
     *
     * @throws IOException if fixture files cannot be created or parsed
     */
    @Test
    public void shouldRejectUnknownEnvironmentInStrictMode() throws IOException {
        Path source = write(HEADER + "environment: cliet\n", false);
        assertThat(new PluginManifestParser().fromYaml(source).environment()).isEqualTo(Environment.BOTH);
        assertThatThrownBy(() -> new PluginManifestParser(true).fromYaml(source))
                .isInstanceOf(IOException.class).hasMessageContaining("Unknown metadata environment [cliet]");
    }

    /**
     * Verifies that explicit valid environments remain available in strict mode.
     *
     * @param environment valid environment token
     * @throws IOException if fixture files cannot be created or parsed
     */
    @ParameterizedTest
    @ValueSource(strings = {"client", "server", "both", "'*'"})
    public void shouldAcceptExplicitEnvironment(String environment) throws IOException {
        Path source = write(HEADER + "environment: " + environment + "\n", false);
        assertThat(new PluginManifestParser(true).fromYaml(source).id()).isEqualTo("safe-plugin");
    }

    /**
     * Verifies that custom Java object tags cannot instantiate objects while parsing metadata.
     *
     * @throws IOException if fixture files cannot be created
     */
    @Test
    public void shouldRejectJavaObjectTags() throws IOException {
        Path source = write(HEADER + "description: !!java.net.URL ['https://example.invalid']\n", false);
        assertThatThrownBy(() -> new PluginManifestParser().fromYaml(source))
                .isInstanceOf(IOException.class).hasMessageContaining("java.net.URL");
    }

    /**
     * Verifies that an incompatible embedded parser cannot disable duplicate-key rejection silently.
     *
     * @throws ReflectiveOperationException if the compatibility fixture cannot be configured
     */
    @Test
    public void shouldFailClosedWhenEmbeddedParserIsUnavailable() throws ReflectiveOperationException {
        var loader = YamlConfigurationLoader.builder().build();
        var holder = YamlConfigurationLoader.class.getDeclaredField("yaml");
        holder.setAccessible(true);
        holder.set(loader, ThreadLocal.withInitial(() -> null));
        var configure = PluginManifestParser.class.getDeclaredMethod("enableDuplicateKeyDetection",
                YamlConfigurationLoader.class);
        configure.setAccessible(true);

        assertThatThrownBy(() -> configure.invoke(null, loader))
                .isInstanceOf(InvocationTargetException.class)
                .cause().isInstanceOf(SerializationException.class)
                .hasMessageContaining("Required duplicate YAML key detection is unavailable");
    }

    private Path write(String yaml, boolean jar) throws IOException {
        Path path = directory.resolve(jar ? "plugin.jar" : "metadata.yml");
        if (jar) {
            try (var output = new JarOutputStream(Files.newOutputStream(path))) {
                output.putNextEntry(new JarEntry("metadata.yml"));
                output.write(yaml.getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        } else {
            Files.writeString(path, yaml, StandardCharsets.UTF_8);
        }
        return path;
    }

    private void parse(PluginManifestParser parser, Path source, boolean jar) throws IOException {
        if (jar) {
            parser.fromJarFile(source, "metadata.yml");
        } else {
            parser.fromYaml(source);
        }
    }
}
