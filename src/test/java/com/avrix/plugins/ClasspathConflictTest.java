package com.avrix.plugins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the immutable {@link ClasspathConflict} value object.
 */
@DisplayName("ClasspathConflict tests")
class ClasspathConflictTest {

    @Test
    @DisplayName("Should expose conflict details")
    void shouldExposeConflictDetails() {
        ClasspathConflict conflict = new ClasspathConflict(
                "com/example/Shared.class",
                List.of("alpha", "beta"),
                "alpha"
        );

        assertThat(conflict.resourcePath()).isEqualTo("com/example/Shared.class");
        assertThat(conflict.pluginIds()).containsExactly("alpha", "beta");
        assertThat(conflict.winnerPluginId()).isEqualTo("alpha");
    }

    @Test
    @DisplayName("Should defensively copy plugin identifiers")
    void shouldCopyPluginIds() {
        List<String> pluginIds = new ArrayList<>(List.of("alpha", "beta"));
        ClasspathConflict conflict = new ClasspathConflict("a/A.class", pluginIds, "alpha");

        pluginIds.add("gamma");

        assertThat(conflict.pluginIds()).containsExactly("alpha", "beta");
        assertThatThrownBy(() -> conflict.pluginIds().add("delta"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should reject invalid conflict descriptions")
    void shouldRejectInvalidDescriptions() {
        assertThatThrownBy(() -> new ClasspathConflict(null, List.of("alpha"), "alpha"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClasspathConflict("a/A.class", null, "alpha"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClasspathConflict("a/A.class", List.of("alpha"), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ClasspathConflict("a/A.class", List.of(), "alpha"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ClasspathConflict("a/A.class", List.of("alpha"), "beta"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
