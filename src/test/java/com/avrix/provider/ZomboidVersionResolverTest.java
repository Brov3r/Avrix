package com.avrix.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidVersionResolver}.
 */
@DisplayName("ZomboidVersionResolver Unit Tests")
class ZomboidVersionResolverTest {

    @Test
    @DisplayName("Should normalize raw semantic versions")
    void shouldNormalizeRawVersion() {
        ZomboidVersionResolver resolver = new ZomboidVersionResolver();

        assertThat(resolver.resolveNormalizedVersion("42.21.0 4a0e9546ec")).isEqualTo("42.21.0");
        assertThat(resolver.resolveNormalizedVersion("42.21+unstable.0")).isEqualTo("42.21.0");
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
}
