package com.avrix.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ZomboidLogLineParser} header parsing, level overrides, and fallback routing.
 */
@DisplayName("ZomboidLogLineParser Unit Tests")
class ZomboidLogLineParserTest {

    private List<String> errors;
    private List<String> warns;
    private List<String> infos;
    private List<String> debugs;
    private List<String> traces;
    private List<String> fallbacks;
    private ZomboidLogLineParser parser;

    @BeforeEach
    void setUp() {
        errors = new ArrayList<>();
        warns = new ArrayList<>();
        infos = new ArrayList<>();
        debugs = new ArrayList<>();
        traces = new ArrayList<>();
        fallbacks = new ArrayList<>();
        parser = new ZomboidLogLineParser(
                errors::add, warns::add, infos::add, debugs::add, traces::add, fallbacks::add);
    }

    @Test
    @DisplayName("Should route LOG headers to the info sink")
    void shouldRouteLogHeaderToInfo() {
        parser.accept("LOG : General f:1234 > hello");

        assertRoutedTo(infos, "hello");
    }

    @Test
    @DisplayName("Should route ERROR headers to the error sink")
    void shouldRouteErrorHeader() {
        parser.accept("ERROR : General f:1234 > boom");

        assertRoutedTo(errors, "boom");
    }

    @Test
    @DisplayName("Should route WARN headers to the warn sink")
    void shouldRouteWarnHeader() {
        parser.accept("WARN : General f:1234 > careful");

        assertRoutedTo(warns, "careful");
    }

    @Test
    @DisplayName("Should route DEBUG headers to the debug sink")
    void shouldRouteDebugHeader() {
        parser.accept("DEBUG : General f:1234 > details");

        assertRoutedTo(debugs, "details");
    }

    @Test
    @DisplayName("Should route TRACE headers to the trace sink")
    void shouldRouteTraceHeader() {
        parser.accept("TRACE : General f:1234 > verbose");

        assertRoutedTo(traces, "verbose");
    }

    @Test
    @DisplayName("Should override the header level with the '!' prefix")
    void shouldOverrideWithErrorPrefix() {
        parser.accept("LOG : General > [!] fatal");

        assertRoutedTo(errors, "fatal");
    }

    @Test
    @DisplayName("Should override the header level with the '?' prefix")
    void shouldOverrideWithWarnPrefix() {
        parser.accept("ERROR : General > [?] downgraded");

        assertRoutedTo(warns, "downgraded");
    }

    @Test
    @DisplayName("Should override the header level with the '$' prefix")
    void shouldOverrideWithDebugPrefix() {
        parser.accept("LOG : General > [$] diagnostic");

        assertRoutedTo(debugs, "diagnostic");
    }

    @Test
    @DisplayName("Should override the header level with the '-' prefix")
    void shouldOverrideWithTracePrefix() {
        parser.accept("LOG : General > [-] verbose");

        assertRoutedTo(traces, "verbose");
    }

    @Test
    @DisplayName("Should override the header level with the '#' prefix")
    void shouldOverrideWithInfoPrefix() {
        parser.accept("ERROR : General > [#] informational");

        assertRoutedTo(infos, "informational");
    }

    @Test
    @DisplayName("Should keep the header level when the prefix character is unknown")
    void shouldKeepHeaderLevelForUnknownPrefix() {
        parser.accept("WARN : General > [x] untouched");

        assertRoutedTo(warns, "[x] untouched");
    }

    @Test
    @DisplayName("Should remove the level prefix and surrounding whitespace")
    void shouldStripPrefixMarker() {
        parser.accept("LOG : General > [!]   spaced message");

        assertRoutedTo(errors, "spaced message");
    }

    @Test
    @DisplayName("Should strip a numeric field after the arrow")
    void shouldStripNumericFieldAfterArrow() {
        parser.accept("LOG : General > 42 hello");

        assertRoutedTo(infos, "hello");
    }

    @Test
    @DisplayName("Should parse combined f:, st:, and at fields")
    void shouldParseHeaderFields() {
        parser.accept("LOG : General f:1234,5678 st:com.example.Thing at Module > parsed");

        assertRoutedTo(infos, "parsed");
    }

    @Test
    @DisplayName("Should parse a compact header without spaces")
    void shouldParseCompactHeader() {
        parser.accept("LOG:General > tight");

        assertRoutedTo(infos, "tight");
    }

    @Test
    @DisplayName("Should ignore leading spaces and tabs before the header")
    void shouldIgnoreLeadingWhitespace() {
        parser.accept("   LOG : General > spaced");
        parser.accept("\tERROR : General > tabbed");

        assertThat(infos).containsExactly("spaced");
        assertThat(errors).containsExactly("tabbed");
        assertThat(warns).isEmpty();
        assertThat(debugs).isEmpty();
        assertThat(traces).isEmpty();
        assertThat(fallbacks).isEmpty();
    }

    @Test
    @DisplayName("Should ignore null input")
    void shouldIgnoreNullInput() {
        parser.accept(null);

        assertNothingRouted();
    }

    @Test
    @DisplayName("Should ignore blank and empty lines")
    void shouldIgnoreBlankLines() {
        parser.accept("");
        parser.accept("   ");
        parser.accept("\t \t");

        assertNothingRouted();
    }

    @Test
    @DisplayName("Should ignore headers with a blank message")
    void shouldIgnoreBlankMessage() {
        parser.accept("LOG : General > ");
        parser.accept("LOG : General >    ");

        assertNothingRouted();
    }

    @Test
    @DisplayName("Should send an unrecognized line to the fallback sink")
    void shouldSendUnrecognizedLineToFallback() {
        parser.accept("random text without a header");

        assertRoutedTo(fallbacks, "random text without a header");
    }

    @Test
    @DisplayName("Should send a headerless line containing an arrow to the fallback sink")
    void shouldSendHeaderlessArrowLineToFallback() {
        parser.accept("LOG : raw message without header > arrow");

        assertRoutedTo(fallbacks, "LOG : raw message without header > arrow");
    }

    @Test
    @DisplayName("Should not match a lowercase header token")
    void shouldNotMatchLowercaseHeader() {
        parser.accept("log : General > lowercase");

        assertRoutedTo(fallbacks, "log : General > lowercase");
    }

    @Test
    @DisplayName("Should strip trailing whitespace before falling back")
    void shouldStripTrailingWhitespaceBeforeFallback() {
        parser.accept("random text   ");

        assertRoutedTo(fallbacks, "random text");
    }

    @Test
    @DisplayName("Should strip trailing whitespace from parsed messages")
    void shouldStripTrailingWhitespaceFromMessage() {
        parser.accept("LOG : General > hello   ");

        assertRoutedTo(infos, "hello");
    }

    @Test
    @DisplayName("Should normalize whitespace before the arrow")
    void shouldNormalizeWhitespaceBeforeArrow() {
        parser.accept("LOG : General > target >value");

        assertRoutedTo(infos, "target > value");
    }

    @Test
    @DisplayName("Should leave arrows without leading whitespace untouched")
    void shouldLeaveCompactArrowUntouched() {
        parser.accept("LOG : General > a>b");

        assertRoutedTo(infos, "a>b");
    }

    @Test
    @DisplayName("Should normalize multiple arrows in one message")
    void shouldNormalizeMultipleArrows() {
        parser.accept("LOG : General > first >second >third");

        assertRoutedTo(infos, "first > second > third");
    }

    @Test
    @DisplayName("Should preserve non-ASCII message content")
    void shouldPreserveNonAsciiMessage() {
        parser.accept("LOG : General > Привет, мир! 你好");

        assertRoutedTo(infos, "Привет, мир! 你好");
    }

    @Test
    @DisplayName("Should reject null sinks")
    void shouldRejectNullSinks() {
        assertThatThrownBy(() -> new ZomboidLogLineParser(null, warns::add, infos::add,
                debugs::add, traces::add, fallbacks::add))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorSink");
        assertThatThrownBy(() -> new ZomboidLogLineParser(errors::add, null, infos::add,
                debugs::add, traces::add, fallbacks::add))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("warnSink");
        assertThatThrownBy(() -> new ZomboidLogLineParser(errors::add, warns::add, null,
                debugs::add, traces::add, fallbacks::add))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("infoSink");
        assertThatThrownBy(() -> new ZomboidLogLineParser(errors::add, warns::add, infos::add,
                null, traces::add, fallbacks::add))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("debugSink");
        assertThatThrownBy(() -> new ZomboidLogLineParser(errors::add, warns::add, infos::add,
                debugs::add, null, fallbacks::add))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("traceSink");
        assertThatThrownBy(() -> new ZomboidLogLineParser(errors::add, warns::add, infos::add,
                debugs::add, traces::add, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fallbackSink");
    }

    private void assertRoutedTo(List<String> expectedSink, String expectedMessage) {
        assertThat(expectedSink).containsExactly(expectedMessage);
        assertThat(otherSinks(expectedSink)).allSatisfy(sink -> assertThat(sink).isEmpty());
    }

    private void assertNothingRouted() {
        assertThat(allSinks()).allSatisfy(sink -> assertThat(sink).isEmpty());
    }

    private List<List<String>> allSinks() {
        return List.of(errors, warns, infos, debugs, traces, fallbacks);
    }

    private List<List<String>> otherSinks(List<String> expectedSink) {
        return allSinks().stream()
                .filter(sink -> sink != expectedSink)
                .toList();
    }
}
