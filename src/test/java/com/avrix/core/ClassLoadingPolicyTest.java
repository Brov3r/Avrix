package com.avrix.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ClassLoadingPolicy}.
 */
@DisplayName("ClassLoadingPolicy Unit Tests")
class ClassLoadingPolicyTest {

    @Test
    @DisplayName("Should provide default parent-delegated prefixes")
    void shouldProvideDefaults() {
        ClassLoadingPolicy policy = ClassLoadingPolicy.defaults();

        assertThat(policy.delegatesToParent("java.lang.String")).isTrue();
        assertThat(policy.delegatesToParent("org.slf4j.Logger")).isTrue();
        assertThat(policy.delegatesToParent("ch.qos.logback.classic.Logger")).isTrue();
        assertThat(policy.delegatesToParent("com.avrix.Main")).isTrue();
        assertThat(policy.delegatesToParent("zombie.characters.IsoPlayer")).isFalse();
        assertThat(policy.parentDelegatedPrefixes())
                .containsExactlyInAnyOrder("java.", "jdk.", "org.slf4j.", "ch.qos.logback.", "com.avrix.");
    }

    @Test
    @DisplayName("Should build a custom policy")
    void shouldBuildCustomPolicy() {
        ClassLoadingPolicy policy = ClassLoadingPolicy.builder()
                .delegateToParent("java.", "custom.")
                .build();

        assertThat(policy.delegatesToParent("java.lang.String")).isTrue();
        assertThat(policy.delegatesToParent("custom.api.Service")).isTrue();
        assertThat(policy.delegatesToParent("org.slf4j.Logger")).isFalse();
    }

    @Test
    @DisplayName("Should reject null and blank prefixes")
    void shouldRejectInvalidPrefixes() {
        assertThatThrownBy(() -> ClassLoadingPolicy.builder().delegateToParent((String[]) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Prefixes");

        assertThatThrownBy(() -> ClassLoadingPolicy.builder().delegateToParent((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Prefix");

        assertThatThrownBy(() -> ClassLoadingPolicy.builder().delegateToParent(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefix");
    }

    @Test
    @DisplayName("Should reject null class names")
    void shouldRejectNullClassName() {
        assertThatThrownBy(() -> ClassLoadingPolicy.defaults().delegatesToParent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Class name");
    }
}
