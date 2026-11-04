package com.avrix.loader;

import com.avrix.api.GameProvider;
import com.avrix.provider.ZomboidConstants;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceConfigurationError;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Branch-focused unit tests for {@link GameProviderFactory} service discovery failures.
 */
@DisplayName("GameProviderFactory Branch Tests")
class GameProviderFactoryBranchesTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should wrap ServiceConfigurationError from a broken service descriptor")
    void shouldWrapServiceConfigurationError() throws Exception {
        Path servicesDirectory = tempDir.resolve("META-INF").resolve("services");
        Files.createDirectories(servicesDirectory);
        Files.writeString(
                servicesDirectory.resolve(GameProvider.class.getName()),
                "missing.provider.NotRegistered" + System.lineSeparator()
        );

        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{tempDir.toUri().toURL()},
                getClass().getClassLoader()
        )) {
            assertThatThrownBy(() -> GameProviderFactory.load(ZomboidConstants.PROVIDER_ID, classLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to discover GameProvider implementations from")
                    .hasCauseInstanceOf(ServiceConfigurationError.class);
        }
    }
}
