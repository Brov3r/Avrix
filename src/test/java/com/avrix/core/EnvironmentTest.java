package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Environment} parsing and compatibility rules.
 */
@DisplayName("Environment Unit Tests")
class EnvironmentTest {

    @Test
    @DisplayName("Should expose the lowercase string values")
    void shouldExposeStringValues() {
        assertThat(Environment.CLIENT.getValue()).isEqualTo("client");
        assertThat(Environment.SERVER.getValue()).isEqualTo("server");
        assertThat(Environment.BOTH.getValue()).isEqualTo("both");
    }

    @Test
    @DisplayName("Should map null and blank input to BOTH")
    void shouldMapNullAndBlankToBoth() {
        assertThat(Environment.fromString(null)).isEqualTo(Environment.BOTH);
        assertThat(Environment.fromString("")).isEqualTo(Environment.BOTH);
        assertThat(Environment.fromString("   ")).isEqualTo(Environment.BOTH);
    }

    @Test
    @DisplayName("Should parse client and server case-insensitively with surrounding whitespace")
    void shouldParseKnownValues() {
        assertThat(Environment.fromString("client")).isEqualTo(Environment.CLIENT);
        assertThat(Environment.fromString(" CLIENT ")).isEqualTo(Environment.CLIENT);
        assertThat(Environment.fromString("server")).isEqualTo(Environment.SERVER);
        assertThat(Environment.fromString("SERVER")).isEqualTo(Environment.SERVER);
        assertThat(Environment.fromString("both")).isEqualTo(Environment.BOTH);
    }

    @Test
    @DisplayName("Should map star and unknown values to BOTH")
    void shouldMapUnknownValuesToBoth() {
        assertThat(Environment.fromString("*")).isEqualTo(Environment.BOTH);
        assertThat(Environment.fromString("unknown")).isEqualTo(Environment.BOTH);
    }

    @Test
    @DisplayName("Should treat BOTH as compatible with every environment")
    void shouldTreatBothAsCompatibleWithEveryEnvironment() {
        assertThat(Environment.BOTH.isCompatibleWith(Environment.CLIENT)).isTrue();
        assertThat(Environment.BOTH.isCompatibleWith(Environment.SERVER)).isTrue();
        assertThat(Environment.BOTH.isCompatibleWith(Environment.BOTH)).isTrue();
    }

    @Test
    @DisplayName("Should require exact matches for client and server")
    void shouldRequireExactMatchesForConcreteEnvironments() {
        assertThat(Environment.CLIENT.isCompatibleWith(Environment.CLIENT)).isTrue();
        assertThat(Environment.CLIENT.isCompatibleWith(Environment.SERVER)).isFalse();
        assertThat(Environment.CLIENT.isCompatibleWith(Environment.BOTH)).isFalse();

        assertThat(Environment.SERVER.isCompatibleWith(Environment.SERVER)).isTrue();
        assertThat(Environment.SERVER.isCompatibleWith(Environment.CLIENT)).isFalse();
        assertThat(Environment.SERVER.isCompatibleWith(Environment.BOTH)).isFalse();
    }

    @Test
    @DisplayName("Should reject a null active environment")
    void shouldRejectNullActiveEnvironment() {
        assertThatThrownBy(() -> Environment.BOTH.isCompatibleWith(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Active environment");
    }
}
