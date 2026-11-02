package com.avrix.plugins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PluginLoadResult} factories and validation.
 */
@DisplayName("PluginLoadResult Unit Tests")
class PluginLoadResultTest {

    @Test
    @DisplayName("Should create a loaded result")
    void shouldCreateLoadedResult() {
        PluginLoadResult result = PluginLoadResult.loaded("example-plugin");

        assertThat(result.pluginId()).isEqualTo("example-plugin");
        assertThat(result.status()).isEqualTo(PluginLoadStatus.LOADED);
        assertThat(result.message()).isEqualTo("Loaded");
        assertThat(result.error()).isNull();
        assertThat(result.errorCause()).isEmpty();
    }

    @Test
    @DisplayName("Should create a failed result from an exception with a message")
    void shouldCreateFailedResultWithMessage() {
        RuntimeException failure = new RuntimeException("boom");

        PluginLoadResult result = PluginLoadResult.failed("example-plugin", failure);

        assertThat(result.pluginId()).isEqualTo("example-plugin");
        assertThat(result.status()).isEqualTo(PluginLoadStatus.FAILED);
        assertThat(result.message()).isEqualTo("RuntimeException: boom");
        assertThat(result.error()).isSameAs(failure);
        assertThat(result.errorCause()).containsSame(failure);
    }

    @Test
    @DisplayName("Should omit the message separator when the exception has no message")
    void shouldCreateFailedResultWithoutExceptionMessage() {
        IllegalStateException failure = new IllegalStateException();

        PluginLoadResult result = PluginLoadResult.failed("example-plugin", failure);

        assertThat(result.message()).isEqualTo("IllegalStateException");
        assertThat(result.errorCause()).containsSame(failure);
    }

    @Test
    @DisplayName("Should create a skipped result")
    void shouldCreateSkippedResult() {
        PluginLoadResult result = PluginLoadResult.skipped("example-plugin", "Disabled by configuration");

        assertThat(result.pluginId()).isEqualTo("example-plugin");
        assertThat(result.status()).isEqualTo(PluginLoadStatus.SKIPPED);
        assertThat(result.message()).isEqualTo("Disabled by configuration");
        assertThat(result.error()).isNull();
        assertThat(result.errorCause()).isEmpty();
    }

    @Test
    @DisplayName("Should replace a null message with an empty string")
    void shouldReplaceNullMessage() {
        PluginLoadResult result = new PluginLoadResult("example-plugin", PluginLoadStatus.LOADED, null, null);

        assertThat(result.message()).isEmpty();
    }

    @Test
    @DisplayName("Should reject null plugin IDs and statuses")
    void shouldRejectNullIdentifiers() {
        assertThatThrownBy(() -> new PluginLoadResult(null, PluginLoadStatus.LOADED, "message", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Plugin ID");

        assertThatThrownBy(() -> new PluginLoadResult("example-plugin", null, "message", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Plugin load status");
    }

    @Test
    @DisplayName("Should reject a null failure cause")
    void shouldRejectNullFailureCause() {
        assertThatThrownBy(() -> PluginLoadResult.failed("example-plugin", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Failure cause");
    }
}
