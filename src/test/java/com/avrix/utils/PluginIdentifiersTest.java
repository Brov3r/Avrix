package com.avrix.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for shared plugin identifier validation.
 */
@DisplayName("PluginIdentifiers Unit Tests")
class PluginIdentifiersTest {

    @Test
    @DisplayName("Should accept canonical identifiers and Java class names")
    void shouldAcceptValidValues() {
        assertThat(PluginIdentifiers.isValid("plugin-id")).isTrue();
        assertThat(PluginIdentifiers.isValid("plugin.name_1")).isTrue();
        assertThat(PluginIdentifiers.isValidJavaName("com.example.Outer$Inner")).isTrue();
    }

    @Test
    @DisplayName("Should reject uppercase, whitespace, control characters, and overlong identifiers")
    void shouldRejectInvalidIdentifiers() {
        assertThat(PluginIdentifiers.isValid(null)).isFalse();
        assertThat(PluginIdentifiers.isValid("   ")).isFalse();
        assertThat(PluginIdentifiers.isValid("Uppercase")).isFalse();
        assertThat(PluginIdentifiers.isValid("bad id")).isFalse();
        assertThat(PluginIdentifiers.isValid("bad\u0000id")).isFalse();
        assertThat(PluginIdentifiers.isValid("a".repeat(65))).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid Java class names")
    void shouldRejectInvalidJavaNames() {
        assertThat(PluginIdentifiers.isValidJavaName(null)).isFalse();
        assertThat(PluginIdentifiers.isValidJavaName(" ")).isFalse();
        assertThat(PluginIdentifiers.isValidJavaName("com.example..Main")).isFalse();
        assertThat(PluginIdentifiers.isValidJavaName("invalid name")).isFalse();
    }

    @Test
    @DisplayName("Should throw descriptive exceptions from requireValid")
    void shouldThrowDescriptiveExceptions() {
        assertThatThrownBy(() -> PluginIdentifiers.requireValid(null, "Plugin 'id'"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Plugin 'id'");
        assertThatThrownBy(() -> PluginIdentifiers.requireValid(" ", "Plugin 'id'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
        assertThatThrownBy(() -> PluginIdentifiers.requireValid("bad id", "Plugin 'id'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot contain whitespace");
        assertThatThrownBy(() -> PluginIdentifiers.requireValid("Uppercase", "Plugin 'id'"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must match identifier pattern");
    }
}
