package com.avrix.provider;

import com.avrix.core.Environment;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import test.support.IsolatedResourceClassLoader;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Branch-focused unit tests for {@link DefaultLoaderProvider} resource fallback paths.
 */
@DisplayName("DefaultLoaderProvider Branch Tests")
class DefaultLoaderProviderBranchesTest {

    private static final String PROPERTIES_RESOURCE = "avrix.properties";

    @Test
    @DisplayName("Should fall back to constants when the properties resource is missing")
    void shouldFallBackWhenPropertiesResourceIsMissing() throws Exception {
        LoaderProvider provider = isolatedProvider(null, false);

        assertThat(provider.getId()).isEqualTo(Constants.LOADER_ID);
        assertThat(provider.getName()).isEqualTo(Constants.LOADER_NAME);
        assertThat(provider.getVersion()).isEqualTo(Constants.LOADER_VERSION);
        assertThat(provider.getLicense()).isEqualTo(Constants.LOADER_LICENSE);
        assertThat(provider.getAuthors()).containsExactly(Constants.LOADER_AUTHOR);
        assertThat(provider.getContacts()).containsExactly(Constants.LOADER_CONTACTS);
    }

    @Test
    @DisplayName("Should fall back to constants when the properties resource is unreadable")
    void shouldFallBackWhenPropertiesResourceIsUnreadable() throws Exception {
        LoaderProvider provider = isolatedProvider(null, true);

        assertThat(provider.getId()).isEqualTo(Constants.LOADER_ID);
        assertThat(provider.getName()).isEqualTo(Constants.LOADER_NAME);
        assertThat(provider.getVersion()).isEqualTo(Constants.LOADER_VERSION);
        assertThat(provider.getLicense()).isEqualTo(Constants.LOADER_LICENSE);
        assertThat(provider.getMetadata().environment()).isEqualTo(Environment.BOTH);
    }

    @Test
    @DisplayName("Should trim properties and drop blank author and contact entries")
    void shouldTrimPropertiesAndDropBlankEntries() throws Exception {
        String properties = """
                avrix.id=custom-loader
                avrix.name=Custom Loader
                avrix.version=9.8.7
                avrix.license=Apache-2.0
                avrix.authors=Alice, , Bob ,
                avrix.contacts= , https://example.test ,
                """;

        LoaderProvider provider = isolatedProvider(properties, false);

        assertThat(provider.getId()).isEqualTo("custom-loader");
        assertThat(provider.getName()).isEqualTo("Custom Loader");
        assertThat(provider.getVersion()).isEqualTo("9.8.7");
        assertThat(provider.getLicense()).isEqualTo("Apache-2.0");
        assertThat(provider.getAuthors()).containsExactly("Alice", "Bob");
        assertThat(provider.getContacts()).containsExactly("https://example.test");
    }

    @Test
    @DisplayName("Should expose a stable metadata instance with immutable collections")
    void shouldExposeStableMetadataInstance() {
        DefaultLoaderProvider provider = new DefaultLoaderProvider();

        assertThat(provider.getMetadata()).isSameAs(provider.getMetadata());
        assertThat(provider.getMetadata().authors()).isEqualTo(provider.getAuthors());
        assertThat(provider.getMetadata().contacts()).isEqualTo(provider.getContacts());
        assertThatThrownBy(() -> provider.getMetadata().authors().add("intruder"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> provider.getMetadata().contacts().add("https://intruder.test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static LoaderProvider isolatedProvider(String content, boolean failOnRead) throws Exception {
        IsolatedResourceClassLoader classLoader = new IsolatedResourceClassLoader(
                DefaultLoaderProvider.class,
                PROPERTIES_RESOURCE,
                content,
                failOnRead
        );
        Class<?> providerType = classLoader.loadIsolatedClass();
        Constructor<?> constructor = providerType.getDeclaredConstructor();
        return (LoaderProvider) constructor.newInstance();
    }
}
