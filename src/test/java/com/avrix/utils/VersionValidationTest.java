package com.avrix.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for shared semantic-version validation.
 */
@DisplayName("VersionValidation Unit Tests")
class VersionValidationTest {

    @Test
    @DisplayName("Should validate semantic versions")
    void shouldValidateVersions() {
        assertThat(VersionValidation.isValidVersion("1.0.0")).isTrue();
        assertThat(VersionValidation.isValidVersion("42.21.0+build.1")).isTrue();
        assertThat(VersionValidation.isValidVersion("1.0")).isFalse();
        assertThat(VersionValidation.isValidVersion("not-a-version")).isFalse();
        assertThat(VersionValidation.isValidVersion(null)).isFalse();
        assertThat(VersionValidation.isValidVersion(" ")).isFalse();
    }

    @Test
    @DisplayName("Should validate dependency constraints")
    void shouldValidateConstraints() {
        assertThat(VersionValidation.isValidConstraint("*")).isTrue();
        assertThat(VersionValidation.isValidConstraint(">=1.0.0")).isTrue();
        assertThat(VersionValidation.isValidConstraint("^2.1.0")).isTrue();
        assertThat(VersionValidation.isValidConstraint("not-a-range")).isFalse();
        assertThat(VersionValidation.isValidConstraint(null)).isFalse();
        assertThat(VersionValidation.isValidConstraint(" ")).isFalse();
    }
}
