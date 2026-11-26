package com.avrix.provider.zomboid.approval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies presentation sanitization and truncation of {@link ZomboidApprovalEntry}.
 */
@DisplayName("ZomboidApprovalEntry sanitization tests")
class ZomboidApprovalEntrySanitizationTest {

    @Test
    @DisplayName("Should sanitize display fields before truncation and keep identity fields unchanged")
    void shouldSanitizeDisplayFieldsOnly() {
        String identityHash = "a".repeat(64);
        ZomboidApprovalEntry entry = new ZomboidApprovalEntry(
                "mod-id",
                "plugin-id",
                "Auth\r\n\u001B or",
                "Dis\u0000play\n\rName\u202E",
                "mod",
                "Sou\u001Brce\r\nName",
                "12345",
                Path.of("plugin.jar"),
                identityHash,
                42L,
                7L,
                "b".repeat(64),
                List.of("test.mixin.ExampleMixin")
        );

        assertThat(entry.modAuthor()).isEqualTo("Auth or");
        assertThat(entry.displayName()).isEqualTo("Dis play Name");
        assertThat(entry.sourceName()).isEqualTo("Sou rce Name");
        assertThat(entry.modAuthor()).doesNotContain("\n", "\r", "\u001B");
        assertThat(entry.displayName()).doesNotContain("\n", "\r", "\u001B", "\u202E", "\u0000");
        assertThat(entry.sourceName()).doesNotContain("\n", "\r", "\u001B");

        assertThat(entry.modId()).isEqualTo("mod-id");
        assertThat(entry.pluginId()).isEqualTo("plugin-id");
        assertThat(entry.sourceType()).isEqualTo("mod");
        assertThat(entry.workshopId()).isEqualTo("12345");
        assertThat(entry.jarPath()).isEqualTo(Path.of("plugin.jar"));
        assertThat(entry.sha256()).isEqualTo(identityHash);
        assertThat(entry.size()).isEqualTo(42L);
        assertThat(entry.modifiedEpochMillis()).isEqualTo(7L);
        assertThat(entry.previousSha256()).isEqualTo("b".repeat(64));
        assertThat(entry.mixinClasses()).containsExactly("test.mixin.ExampleMixin");
    }

    @Test
    @DisplayName("Should sanitize the display mod ID without changing the identity mod ID")
    void shouldSanitizeOnlyTheDisplayModId() {
        String rawModId = "mod\u0000\r\nID\u001B\u202E";
        ZomboidApprovalEntry entry = new ZomboidApprovalEntry(
                rawModId,
                "plugin-id",
                "Author",
                "Plugin",
                "mod",
                "Mod",
                "",
                Path.of("plugin.jar"),
                "a".repeat(64),
                0L,
                0L,
                "",
                List.of()
        );

        assertThat(entry.displayModId()).isEqualTo("mod ID");
        assertThat(entry.displayModId()).doesNotContain("\u0000", "\r", "\n", "\u001B", "\u202E");
        assertThat(entry.modId()).isEqualTo(rawModId);
    }

    @Test
    @DisplayName("Should truncate sanitized display fields to the approval presentation limit")
    void shouldTruncateSanitizedDisplayFields() {
        String oversized = "x".repeat(ApprovalLimits.MAX_DISPLAY_CHARACTERS + 100) + "\u001B";

        ZomboidApprovalEntry entry = new ZomboidApprovalEntry(
                "mod-id",
                "plugin-id",
                oversized,
                oversized,
                "mod",
                oversized,
                "",
                Path.of("plugin.jar"),
                "a".repeat(64),
                0L,
                0L,
                "",
                List.of()
        );

        assertThat(entry.modAuthor())
                .hasSize(ApprovalLimits.MAX_DISPLAY_CHARACTERS)
                .endsWith("…")
                .doesNotContain("\u001B");
        assertThat(entry.displayName())
                .hasSize(ApprovalLimits.MAX_DISPLAY_CHARACTERS)
                .endsWith("…")
                .doesNotContain("\u001B");
        assertThat(entry.sourceName())
                .hasSize(ApprovalLimits.MAX_DISPLAY_CHARACTERS)
                .endsWith("…")
                .doesNotContain("\u001B");
    }
}
