package com.avrix.loader;

import com.avrix.api.GameProvider;
import com.avrix.provider.zomboid.ZomboidConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.loader.FatalGameProvider;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
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

    @Test
    @DisplayName("Should propagate a fatal cause of a service discovery failure")
    void shouldPropagateFatalServiceConfigurationErrorCause() throws Exception {
        VirtualMachineError fatal = new SimulatedVmFailure();
        FatalGameProvider.failure = fatal;
        Path root = tempDir.resolve("fatal");
        Path servicesDirectory = root.resolve("META-INF").resolve("services");
        Files.createDirectories(servicesDirectory);
        Files.writeString(
                servicesDirectory.resolve(GameProvider.class.getName()),
                FatalGameProvider.class.getName() + System.lineSeparator()
        );

        try (URLClassLoader classLoader = serviceIsolatedClassLoader(root)) {
            assertThatThrownBy(() -> GameProviderFactory.load(ZomboidConstants.PROVIDER_ID, classLoader))
                    .isSameAs(fatal);
        } finally {
            FatalGameProvider.failure = null;
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
            public Enumeration<URL> getResources(String name) throws IOException {
                if (serviceResourceName().equals(name)) {
                    return Collections.emptyEnumeration();
                }
                return super.getResources(name);
            }
        };

        return new URLClassLoader(new URL[]{root.toUri().toURL()}, parent);
    }

    private static String serviceResourceName() {
        return "META-INF/services/" + GameProvider.class.getName();
    }

    private static final class SimulatedVmFailure extends VirtualMachineError {
        private SimulatedVmFailure() {
            super("simulated fatal service discovery failure");
        }
    }
}
