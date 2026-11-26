package com.avrix.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DisplayText}.
 */
@DisplayName("DisplayText Unit Tests")
class DisplayTextTest {

    private static final String FORBIDDEN_CONTROLS = "\u0000\u0001\u0007\b\t\n\u000B\f\r\u001B\u007F\u0085"
            + "\u061C\u200E\u200F\u2028\u2029\u202A\u202B\u202C\u202D\u202E\u2066\u2067\u2068\u2069";

    @Test
    @DisplayName("Should turn null into an empty string")
    void shouldHandleNull() {
        assertThat(DisplayText.sanitizeSingleLine(null)).isEmpty();
        assertThat(DisplayText.sanitizeMultiline(null)).isEmpty();
    }

    @Test
    @DisplayName("Should replace control, bidi, and separator characters and collapse whitespace")
    void shouldSanitizeSingleLine() {
        String input = "  Evil\u0000\tName\r\n\u001B\u202E with\nignored\u2028space  ";

        assertThat(DisplayText.sanitizeSingleLine(input)).isEqualTo("Evil Name with ignored space");
    }

    @Test
    @DisplayName("Should remove every documented control category from single-line text")
    void shouldRemoveForbiddenControlsFromSingleLine() {
        for (int index = 0; index < FORBIDDEN_CONTROLS.length(); index++) {
            String input = "left" + FORBIDDEN_CONTROLS.charAt(index) + "right";

            assertThat(DisplayText.sanitizeSingleLine(input))
                    .as("control U+%04X", (int) FORBIDDEN_CONTROLS.charAt(index))
                    .isEqualTo("left right");
        }
    }

    @Test
    @DisplayName("Should return an empty string when only control or whitespace characters remain")
    void shouldHandleBlankSingleLine() {
        assertThat(DisplayText.sanitizeSingleLine("\u0000\u001B\u202E \t\r\n")).isEmpty();
        assertThat(DisplayText.sanitizeSingleLine(" \t ")).isEmpty();
    }

    @Test
    @DisplayName("Should preserve Cyrillic, CJK, emoji, variation selectors, and combining marks")
    void shouldPreserveUnicodeInSingleLine() {
        String input = "Кириллица 中文 😀\uFE0F 👨\u200D👩\u200D👧 e\u0301";

        assertThat(DisplayText.sanitizeSingleLine(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("Should normalize CRLF and lone CR while preserving tabs and line feeds")
    void shouldSanitizeMultilineLineEndings() {
        String input = "line1\r\nline2\rline3\n\tline4";

        assertThat(DisplayText.sanitizeMultiline(input)).isEqualTo("line1\nline2\nline3\n\tline4");
    }

    @Test
    @DisplayName("Should replace unsafe controls in multiline text without collapsing whitespace")
    void shouldSanitizeMultilineControls() {
        String input = "a  \u0000b\u001Bc\u202Ed\n\u2028e";

        String sanitized = DisplayText.sanitizeMultiline(input);

        assertThat(sanitized).isEqualTo("a   b c d\n e");
        assertThat(sanitized).doesNotContain("\u0000", "\u001B", "\u202E", "\u2028", "\r");
    }

    @Test
    @DisplayName("Should preserve Cyrillic, CJK, emoji, tabs, and combining marks in multiline text")
    void shouldPreserveUnicodeInMultiline() {
        String input = "Кириллица\t中文 😀 👨\u200D👩\u200D👧 e\u0301\nsecond";

        assertThat(DisplayText.sanitizeMultiline(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("Should keep leading, trailing, and repeated line breaks in multiline text")
    void shouldKeepMultilineLayout() {
        assertThat(DisplayText.sanitizeMultiline("\n\nfirst\n\nsecond\n\n"))
                .isEqualTo("\n\nfirst\n\nsecond\n\n");
    }
}
