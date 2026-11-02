package com.avrix.provider;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the default methods declared by {@link GameProvider}.
 */
@DisplayName("GameProvider Default Methods Unit Tests")
class GameProviderDefaultsTest {

    @Test
    @DisplayName("Should synthesize metadata from provider accessors")
    void shouldSynthesizeMetadataFromAccessors() {
        DefaultsProvider provider = new DefaultsProvider();
        Metadata metadata = provider.getMetadata();

        assertThat(metadata.id()).isEqualTo("defaults-provider");
        assertThat(metadata.name()).isEqualTo("Defaults Game");
        assertThat(metadata.version()).isEqualTo("2.0.0");
        assertThat(metadata.environment()).isEqualTo(Environment.SERVER);
        assertThat(metadata.authors()).containsExactly("Author One", "Author Two");
        assertThat(metadata.license()).isEqualTo("Apache-2.0");
        assertThat(metadata.contacts()).containsExactly("https://example.test/defaults");
        assertThat(metadata.entrypoint()).isEqualTo("test.Defaults");
    }

    @Test
    @DisplayName("Should return empty library paths and provider arguments by default")
    void shouldReturnEmptyCollectionsByDefault() {
        DefaultsProvider provider = new DefaultsProvider();

        assertThat(provider.getJavaLibsPath()).isEmpty();
        assertThat(provider.getNativeLibsPath()).isEmpty();
        assertThat(provider.getProviderArgs()).isEmpty();
    }

    @Test
    @DisplayName("Should provide a no-op stream redirection by default")
    void shouldProvideNoOpStreamRedirection() {
        DefaultsProvider provider = new DefaultsProvider();

        assertThatCode(provider::redirectSystemStreamsToLogger).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should delegate init with a class loader to the no-argument init")
    void shouldDelegateClassLoaderInitToNoArgumentInit() throws IOException {
        DefaultsProvider provider = new DefaultsProvider();

        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            provider.init(classLoader);
        }

        assertThat(provider.initCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("Should expose immutable default collections")
    void shouldExposeImmutableDefaultCollections() {
        DefaultsProvider provider = new DefaultsProvider();

        assertThatThrownBy(() -> provider.getJavaLibsPath().add(Path.of("library.jar")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.getNativeLibsPath().add(Path.of("native.dll")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.getProviderArgs().put("key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Minimal provider that relies exclusively on interface default methods where possible.
     */
    private static final class DefaultsProvider implements GameProvider {
        private int initCalls;

        /** {@inheritDoc} */
        @Override
        public void init() {
            initCalls++;
        }

        /** {@inheritDoc} */
        @Override
        public void launch(String[] args) {
        }

        /** {@inheritDoc} */
        @Override
        public String[] getLaunchArgs() {
            return new String[0];
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return "Defaults Game";
        }

        /** {@inheritDoc} */
        @Override
        public String getId() {
            return "defaults-provider";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getAuthors() {
            return List.of("Author One", "Author Two");
        }

        /** {@inheritDoc} */
        @Override
        public String getLicense() {
            return "Apache-2.0";
        }

        /** {@inheritDoc} */
        @Override
        public List<String> getContacts() {
            return List.of("https://example.test/defaults");
        }

        /** {@inheritDoc} */
        @Override
        public String getNormalizedVersion() {
            return "2.0.0";
        }

        /** {@inheritDoc} */
        @Override
        public String getRawVersion() {
            return "2.0.0";
        }

        /** {@inheritDoc} */
        @Override
        public Path getLaunchDirectory() {
            return Path.of(".");
        }

        /** {@inheritDoc} */
        @Override
        public String getEntrypoint() {
            return "test.Defaults";
        }

        /** {@inheritDoc} */
        @Override
        public Environment getEnvironment() {
            return Environment.SERVER;
        }
    }
}
