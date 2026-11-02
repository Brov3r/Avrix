package com.avrix.provider;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultLoaderProvider} descriptors and metadata synthesis.
 */
@DisplayName("DefaultLoaderProvider Unit Tests")
class DefaultLoaderProviderTest {

    @Test
    @DisplayName("Should expose loader identity, license, authors, and contacts")
    void shouldExposeLoaderIdentity() {
        DefaultLoaderProvider provider = new DefaultLoaderProvider();

        assertThat(provider.getId()).isEqualTo(Constants.LOADER_ID);
        assertThat(provider.getName()).isEqualTo(Constants.LOADER_NAME);
        assertThat(provider.getVersion()).isEqualTo(Constants.LOADER_VERSION);
        assertThat(provider.getLicense()).isEqualTo(Constants.LOADER_LICENSE);
        assertThat(provider.getAuthors()).contains(Constants.LOADER_AUTHOR);
        assertThat(provider.getContacts()).contains(Constants.LOADER_CONTACTS);
    }

    @Test
    @DisplayName("Should synthesize metadata matching the provider descriptor")
    void shouldSynthesizeMetadata() {
        DefaultLoaderProvider provider = new DefaultLoaderProvider();
        Metadata metadata = provider.getMetadata();

        assertThat(metadata.schema()).isEqualTo(Constants.METADATA_SCHEMA);
        assertThat(metadata.id()).isEqualTo(provider.getId());
        assertThat(metadata.name()).isEqualTo(provider.getName());
        assertThat(metadata.version()).isEqualTo(provider.getVersion());
        assertThat(metadata.license()).isEqualTo(provider.getLicense());
        assertThat(metadata.environment()).isEqualTo(Environment.BOTH);
        assertThat(metadata.authors()).containsExactlyElementsOf(provider.getAuthors());
        assertThat(metadata.contacts()).containsExactlyElementsOf(provider.getContacts());
        assertThat(metadata.entrypoint()).isEmpty();
    }

    @Test
    @DisplayName("Should load through the normal properties resource path")
    void shouldLoadWithPropertiesResourceAvailable() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("avrix.properties")) {
            assertThat(stream).as("avrix.properties should be present on the test classpath").isNotNull();
        }

        DefaultLoaderProvider provider = new DefaultLoaderProvider();

        assertThat(provider.getId()).isNotBlank();
        assertThat(provider.getName()).isNotBlank();
        assertThat(provider.getVersion()).isNotBlank();
        assertThat(provider.getLicense()).isNotBlank();
    }

    @Test
    @DisplayName("Should expose immutable author and contact lists")
    void shouldExposeImmutableCollections() {
        DefaultLoaderProvider provider = new DefaultLoaderProvider();

        assertThatThrownBy(() -> provider.getAuthors().add("Another Author"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.getContacts().add("https://example.test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
