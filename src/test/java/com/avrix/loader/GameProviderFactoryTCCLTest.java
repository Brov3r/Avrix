package com.avrix.loader;

import com.avrix.api.GameProvider;
import com.avrix.provider.ZomboidConstants;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.TestGameProvider;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GameProviderFactory} class loader selection and duplicate detection.
 */
@DisplayName("GameProviderFactory ClassLoader Unit Tests")
class GameProviderFactoryTCCLTest {

    @TempDir
    Path tempDir;

    private ClassLoader originalContextClassLoader;

    @BeforeEach
    void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }

    @Test
    @DisplayName("Should load the test provider through an explicit class loader")
    void shouldLoadTestProviderThroughExplicitClassLoader() {
        GameProvider provider = GameProviderFactory.load(
                TestGameProvider.ID,
                getClass().getClassLoader()
        );

        assertThat(provider).isInstanceOf(TestGameProvider.class);
        assertThat(provider.getId()).isEqualTo(TestGameProvider.ID);
    }

    @Test
    @DisplayName("Should discover both the test and default providers")
    void shouldDiscoverBothRegisteredProviders() {
        ClassLoader classLoader = getClass().getClassLoader();

        GameProvider testProvider = GameProviderFactory.load(TestGameProvider.ID, classLoader);
        GameProvider defaultProvider = GameProviderFactory.load(ZomboidConstants.PROVIDER_ID, classLoader);

        assertThat(testProvider).isInstanceOf(TestGameProvider.class);
        assertThat(defaultProvider.getId()).isEqualTo(ZomboidConstants.PROVIDER_ID);
    }

    @Test
    @DisplayName("Should load the provider through the current thread context class loader")
    void shouldLoadProviderThroughContextClassLoader() throws IOException {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader contextClassLoader = new URLClassLoader(
                new URL[0],
                getClass().getClassLoader()
        )) {
            Thread.currentThread().setContextClassLoader(contextClassLoader);

            GameProvider provider = GameProviderFactory.load(TestGameProvider.ID);

            assertThat(provider).isInstanceOf(TestGameProvider.class);
            assertThat(provider.getId()).isEqualTo(TestGameProvider.ID);
        }
    }

    @Test
    @DisplayName("Should fall back to the service class loader when the context class loader is null")
    void shouldFallBackWhenContextClassLoaderIsNull() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(null);

        GameProvider provider = GameProviderFactory.load(TestGameProvider.ID);

        assertThat(provider).isInstanceOf(TestGameProvider.class);
    }

    @Test
    @DisplayName("Should list both available provider IDs when the requested one is missing")
    void shouldListAvailableProviderIdsWhenMissing() {
        assertThatThrownBy(() -> GameProviderFactory.load("missing-provider", getClass().getClassLoader()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No GameProvider registered for ID [missing-provider]")
                .hasMessageContaining(TestGameProvider.ID)
                .hasMessageContaining(ZomboidConstants.PROVIDER_ID);
    }

    @Test
    @DisplayName("Should reject multiple providers registered for the same ID")
    void shouldRejectDuplicateProviderIds() throws IOException {
        Path servicesDirectory = tempDir.resolve("META-INF").resolve("services");
        Files.createDirectories(servicesDirectory);
        Files.writeString(
                servicesDirectory.resolve(GameProvider.class.getName()),
                TestGameProvider.class.getName() + System.lineSeparator()
                        + TestGameProvider.Duplicate.class.getName() + System.lineSeparator()
        );

        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                getClass().getClassLoader()
        )) {
            assertThatThrownBy(() -> GameProviderFactory.load(TestGameProvider.ID, classLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple GameProvider implementations registered for ID")
                    .hasMessageContaining(TestGameProvider.ID);
        }
    }

    @Test
    @DisplayName("Should select the only provider in auto mode")
    void shouldSelectSingleProviderInAutoMode() throws IOException {
        Path root = tempDir.resolve("auto-single");
        Path servicesDirectory = root.resolve("META-INF").resolve("services");
        Files.createDirectories(servicesDirectory);
        Files.writeString(
                servicesDirectory.resolve(GameProvider.class.getName()),
                TestGameProvider.class.getName() + System.lineSeparator()
        );

        try (URLClassLoader classLoader = serviceIsolatedClassLoader(root)) {
            GameProvider provider = GameProviderFactory.load(Constants.AUTO_PROVIDER_ID, classLoader);

            assertThat(provider).isInstanceOf(TestGameProvider.class);
            assertThat(provider.getId()).isEqualTo(TestGameProvider.ID);
        }
    }

    @Test
    @DisplayName("Should reject ambiguous auto mode when several providers are registered")
    void shouldRejectAmbiguousAutoMode() throws IOException {
        Path root = tempDir.resolve("auto-multiple");
        Path servicesDirectory = root.resolve("META-INF").resolve("services");
        Files.createDirectories(servicesDirectory);
        Files.writeString(
                servicesDirectory.resolve(GameProvider.class.getName()),
                TestGameProvider.class.getName() + System.lineSeparator()
                        + TestGameProvider.Duplicate.class.getName() + System.lineSeparator()
        );

        try (URLClassLoader classLoader = serviceIsolatedClassLoader(root)) {
            assertThatThrownBy(() -> GameProviderFactory.load(Constants.AUTO_PROVIDER_ID, classLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple GameProvider implementations registered")
                    .hasMessageContaining(TestGameProvider.ID);
        }
    }

    private URLClassLoader serviceIsolatedClassLoader(Path root) throws IOException {
        ClassLoader parent = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public URL getResource(String name) {
                if (serviceResourceName().equals(name)) {
                    return null;
                }
                return super.getResource(name);
            }

            @Override
            public java.util.Enumeration<URL> getResources(String name) throws IOException {
                if (serviceResourceName().equals(name)) {
                    return java.util.Collections.emptyEnumeration();
                }
                return super.getResources(name);
            }
        };

        return new URLClassLoader(new URL[]{root.toUri().toURL()}, parent);
    }

    private static String serviceResourceName() {
        return "META-INF/services/" + GameProvider.class.getName();
    }
}
