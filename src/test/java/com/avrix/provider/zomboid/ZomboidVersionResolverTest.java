package com.avrix.provider.zomboid;

import com.avrix.provider.GameVersionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidVersionResolver}.
 */
@DisplayName("ZomboidVersionResolver Unit Tests")
class ZomboidVersionResolverTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should normalize raw semantic versions")
    void shouldNormalizeRawVersion() {
        ZomboidVersionResolver resolver = new ZomboidVersionResolver();

        assertThat(resolver.resolveNormalizedVersion("42.21.0 4a0e9546ec")).isEqualTo("42.21.0");
        assertThat(resolver.resolveNormalizedVersion("42.21+unstable.0")).isEqualTo("42.21.0");
        assertThat(resolver.resolveNormalizedVersion("42.21-dev.0")).isEqualTo("42.21.0");
        assertThat(resolver.resolveNormalizedVersion("42.21.1")).isEqualTo("42.21.1");
    }

    @Test
    @DisplayName("Should fall back to 0.0.0 for invalid versions")
    void shouldFallBackForInvalidVersion() {
        ZomboidVersionResolver resolver = new ZomboidVersionResolver();

        assertThat(resolver.resolveNormalizedVersion(null)).isEqualTo("0.0.0");
        assertThat(resolver.resolveNormalizedVersion("invalid")).isEqualTo("0.0.0");
    }

    @Test
    @DisplayName("Should return a fallback raw version when game classes are unavailable")
    void shouldReturnFallbackRawVersion() {
        ClassLoader emptyLoader = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return null;
            }
        };

        assertThat(new ZomboidVersionResolver().resolveRawVersion(emptyLoader)).isEqualTo("0.0.0");
    }

    @Test
    @DisplayName("Should resolve version components from the game archive")
    void shouldResolveVersionFromArchive() throws Exception {
        Path archive = VersionJarFixture.createGameArchive(tempDir, 42, 21, "", 0, "4a0e9546ec");

        GameVersionInfo version = new ZomboidVersionResolver().resolve(archive);

        assertThat(version.major()).isEqualTo(42);
        assertThat(version.minor()).isEqualTo(21);
        assertThat(version.patch()).isZero();
        assertThat(version.suffix()).isEmpty();
        assertThat(version.revision()).isEqualTo("4a0e9546ec");
        assertThat(version.raw()).isEqualTo("42.21.0 4a0e9546ec");
        assertThat(version.normalized()).isEqualTo("42.21.0");
    }

    @Test
    @DisplayName("Should read a non-zero patch from getGameAndBuildVersion")
    void shouldReadNonZeroPatch() throws Exception {
        Path archive = VersionJarFixture.createGameArchive(tempDir, 42, 21, "", 1, "abcdef1234");

        GameVersionInfo version = new ZomboidVersionResolver().resolve(archive);

        assertThat(version.patch()).isEqualTo(1);
        assertThat(version.raw()).isEqualTo("42.21.1 abcdef1234");
        assertThat(version.normalized()).isEqualTo("42.21.1");
    }

    @Test
    @DisplayName("Should preserve a version suffix")
    void shouldPreserveVersionSuffix() throws Exception {
        Path archive = VersionJarFixture.createGameArchive(tempDir, 42, 21, "+unstable", 0, "abcdef1234");

        GameVersionInfo version = new ZomboidVersionResolver().resolve(archive);

        assertThat(version.suffix()).isEqualTo("+unstable");
        assertThat(version.raw()).isEqualTo("42.21+unstable.0 abcdef1234");
        assertThat(version.normalized()).isEqualTo("42.21.0");
    }

    @Test
    @DisplayName("Should read the revision from GitVersion.REVISION")
    void shouldReadRevisionFromConstantValue() throws Exception {
        Path archive = VersionJarFixture.createGameArchive(tempDir, 42, 21, "", 0, "deadbeef");

        GameVersionInfo version = new ZomboidVersionResolver().resolve(archive);

        assertThat(version.revision()).isEqualTo("deadbeef");
    }

    @Test
    @DisplayName("Should not load game classes when resolving from a classloader")
    void shouldNotLoadGameClasses() throws Exception {
        Path archive = VersionJarFixture.createGameArchive(tempDir, 42, 21, "", 0, "4a0e9546ec");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{archive.toUri().toURL()}, null) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException("Game class must not be loaded: " + name);
            }
        }) {
            GameVersionInfo version = new ZomboidVersionResolver().resolve(loader);

            assertThat(version.raw()).isEqualTo("42.21.0 4a0e9546ec");
            assertThat(version.normalized()).isEqualTo("42.21.0");
        }
    }

    @Test
    @DisplayName("Should fall back when the game archive is missing")
    void shouldFallBackWhenArchiveIsMissing() {
        GameVersionInfo version = new ZomboidVersionResolver().resolve(tempDir.resolve("missing.jar"));

        assertThat(version.raw()).isEqualTo("0.0.0");
        assertThat(version.normalized()).isEqualTo("0.0.0");
    }
}
