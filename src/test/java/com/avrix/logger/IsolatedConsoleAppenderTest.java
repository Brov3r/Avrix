package com.avrix.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IsolatedConsoleAppender} configuration, lifecycle, and event routing.
 * <p>
 * The appender writes directly to the raw process file descriptors, so the backing streams are
 * replaced with in-memory buffers through reflection to keep the test output clean.
 */
@DisplayName("IsolatedConsoleAppender Unit Tests")
class IsolatedConsoleAppenderTest {

    private LoggerContext context;
    private Logger logger;
    private IsolatedConsoleAppender appender;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private StubEncoder encoder;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        logger = context.getLogger("com.avrix.logger.test");
        appender = new IsolatedConsoleAppender();
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        replaceField(appender, "outStream", out);
        replaceField(appender, "errStream", err);
        appender.setContext(context);
        encoder = new StubEncoder();
    }

    @AfterEach
    void tearDown() {
        if (appender.isStarted()) {
            appender.stop();
        }
    }

    @Test
    @DisplayName("Should expose WARN as the default error level and no encoder")
    void shouldExposeDefaults() {
        assertThat(appender.getErrorLevel()).isEqualTo("WARN");
        assertThat(appender.getEncoder()).isNull();
    }

    @Test
    @DisplayName("Should return the encoder that was set")
    void shouldReturnConfiguredEncoder() {
        appender.setEncoder(encoder);

        assertThat(appender.getEncoder()).isSameAs(encoder);

        appender.setEncoder(null);

        assertThat(appender.getEncoder()).isNull();
    }

    @Test
    @DisplayName("Should accept a valid error level name")
    void shouldAcceptValidErrorLevel() {
        appender.setErrorLevel("ERROR");

        assertThat(appender.getErrorLevel()).isEqualTo("ERROR");

        appender.setErrorLevel("debug");

        assertThat(appender.getErrorLevel()).isEqualTo("DEBUG");
    }

    @Test
    @DisplayName("Should fall back to WARN for null, blank, or unknown error levels")
    void shouldFallBackToWarnForInvalidErrorLevel() {
        appender.setErrorLevel("ERROR");

        appender.setErrorLevel(null);
        assertThat(appender.getErrorLevel()).isEqualTo("WARN");

        appender.setErrorLevel("ERROR");
        appender.setErrorLevel("   ");
        assertThat(appender.getErrorLevel()).isEqualTo("WARN");

        appender.setErrorLevel("ERROR");
        appender.setErrorLevel("NOT_A_LEVEL");
        assertThat(appender.getErrorLevel()).isEqualTo("WARN");
    }

    @Test
    @DisplayName("Should not start without an encoder and report an error status")
    void shouldNotStartWithoutEncoder() {
        appender.start();

        assertThat(appender.isStarted()).isFalse();
        assertThat(errorStatusMessages())
                .anySatisfy(message -> assertThat(message).contains("No encoder configured"));
    }

    @Test
    @DisplayName("Should start and stop together with a layout encoder")
    void shouldStartAndStopWithLayoutEncoder() {
        PatternLayoutEncoder layoutEncoder = patternEncoder("%level %msg%n");
        appender.setEncoder(layoutEncoder);

        appender.start();

        assertThat(appender.isStarted()).isTrue();
        assertThat(layoutEncoder.isStarted()).isTrue();
        assertThat(layoutEncoder.getCharset()).isNotNull();

        appender.stop();

        assertThat(appender.isStarted()).isFalse();
        assertThat(layoutEncoder.isStarted()).isFalse();
    }

    @Test
    @DisplayName("Should start the encoder only once across repeated start calls")
    void shouldStartEncoderOnce() {
        appender.setEncoder(encoder);

        appender.start();
        appender.start();

        assertThat(encoder.startCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should route events to stdout and stderr using the default threshold")
    void shouldRouteEventsUsingDefaultThreshold() {
        encoder.payload = event -> (event.getLevel() + ":" + event.getFormattedMessage())
                .getBytes(StandardCharsets.UTF_8);
        appender.setEncoder(encoder);
        appender.start();

        appender.append(event(Level.INFO, "info message"));
        appender.append(event(Level.WARN, "warn message"));
        appender.append(event(Level.ERROR, "error message"));
        appender.append(event(Level.DEBUG, "debug message"));

        assertThat(outText()).isEqualTo("INFO:info messageDEBUG:debug message");
        assertThat(errText()).isEqualTo("WARN:warn messageERROR:error message");
        assertThat(encoder.encodeCount).isEqualTo(4);
    }

    @Test
    @DisplayName("Should honor a custom error level threshold")
    void shouldHonorCustomErrorLevel() {
        encoder.payload = event -> event.getLevel().toString().getBytes(StandardCharsets.UTF_8);
        appender.setEncoder(encoder);
        appender.setErrorLevel("ERROR");
        appender.start();

        appender.append(event(Level.INFO, "info"));
        appender.append(event(Level.WARN, "warn"));
        appender.append(event(Level.ERROR, "error"));

        assertThat(outText()).isEqualTo("INFOWARN");
        assertThat(errText()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should skip writing when the encoder returns null")
    void shouldSkipNullEncodedBytes() {
        encoder.payload = event -> null;
        appender.setEncoder(encoder);
        appender.start();

        appender.append(event(Level.ERROR, "ignored"));

        assertThat(encoder.encodeCount).isEqualTo(1);
        assertThat(outText()).isEmpty();
        assertThat(errText()).isEmpty();
        assertThat(errorStatusMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should skip writing when the encoder returns an empty array")
    void shouldSkipEmptyEncodedBytes() {
        encoder.payload = event -> new byte[0];
        appender.setEncoder(encoder);
        appender.start();

        appender.append(event(Level.ERROR, "ignored"));

        assertThat(outText()).isEmpty();
        assertThat(errText()).isEmpty();
    }

    @Test
    @DisplayName("Should ignore a null event")
    void shouldIgnoreNullEvent() {
        appender.setEncoder(encoder);
        appender.start();

        appender.append(null);

        assertThat(encoder.encodeCount).isZero();
        assertThat(outText()).isEmpty();
        assertThat(errText()).isEmpty();
    }

    @Test
    @DisplayName("Should ignore events while the appender is not started")
    void shouldIgnoreEventsWhenNotStarted() {
        appender.setEncoder(encoder);

        appender.append(event(Level.ERROR, "ignored"));

        assertThat(encoder.encodeCount).isZero();
        assertThat(outText()).isEmpty();
        assertThat(errText()).isEmpty();
    }

    @Test
    @DisplayName("Should ignore events after the encoder is removed")
    void shouldIgnoreEventsWithoutEncoder() {
        appender.setEncoder(encoder);
        appender.start();

        appender.setEncoder(null);
        appender.append(event(Level.ERROR, "ignored"));

        assertThat(encoder.encodeCount).isZero();
        assertThat(outText()).isEmpty();
        assertThat(errText()).isEmpty();
    }

    @Test
    @DisplayName("Should write encoder header on start and footer on stop")
    void shouldWriteHeaderAndFooter() {
        encoder.header = "HEAD".getBytes(StandardCharsets.UTF_8);
        encoder.footer = "FOOT".getBytes(StandardCharsets.UTF_8);
        appender.setEncoder(encoder);

        appender.start();

        assertThat(outText()).isEqualTo("HEAD");

        appender.stop();

        assertThat(outText()).isEqualTo("HEADFOOT");
        assertThat(encoder.stopCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not write a header when the encoder has none")
    void shouldNotWriteMissingHeader() {
        appender.setEncoder(encoder);

        appender.start();

        assertThat(outText()).isEmpty();
    }

    @Test
    @DisplayName("Should format a real logging event through a pattern encoder")
    void shouldFormatEventThroughPatternEncoder() {
        appender.setEncoder(patternEncoder("%-5level %msg%n"));
        appender.start();

        appender.append(event(Level.INFO, "pattern message"));

        assertThat(outText()).isEqualTo("INFO  pattern message" + System.lineSeparator());
    }

    @Test
    @DisplayName("Should report an error status when the target stream rejects the write")
    void shouldReportWriteFailure() {
        replaceField(appender, "outStream", new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("rejected");
            }
        });
        encoder.payload = event -> "payload".getBytes(StandardCharsets.UTF_8);
        appender.setEncoder(encoder);
        appender.start();

        appender.append(event(Level.INFO, "failing"));

        assertThat(errorStatusMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Failed to write log event directly to isolated console descriptors"));
    }

    private LoggingEvent event(Level level, String message) {
        return new LoggingEvent(
                "com.avrix.logger.IsolatedConsoleAppenderTest", logger, level, message, null, null);
    }

    private PatternLayoutEncoder patternEncoder(String pattern) {
        PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
        layoutEncoder.setContext(context);
        layoutEncoder.setPattern(pattern);
        return layoutEncoder;
    }

    private String outText() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String errText() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private List<String> errorStatusMessages() {
        return context.getStatusManager().getCopyOfStatusList().stream()
                .filter(status -> status.getLevel() == Status.ERROR)
                .map(Status::getMessage)
                .toList();
    }

    private static void replaceField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to replace field " + name, e);
        }
    }

    /**
     * Minimal configurable encoder used to observe appender behavior without touching real console streams.
     */
    private static final class StubEncoder extends ContextAwareBase implements Encoder<ILoggingEvent> {

        private byte[] header;
        private byte[] footer;
        private Function<ILoggingEvent, byte[]> payload = event -> null;
        private boolean started;
        private int encodeCount;
        private int startCount;
        private int stopCount;

        @Override
        public byte[] headerBytes() {
            return header;
        }

        @Override
        public byte[] encode(ILoggingEvent event) {
            encodeCount++;
            return payload.apply(event);
        }

        @Override
        public byte[] footerBytes() {
            return footer;
        }

        @Override
        public void start() {
            startCount++;
            started = true;
        }

        @Override
        public void stop() {
            stopCount++;
            started = false;
        }

        @Override
        public boolean isStarted() {
            return started;
        }
    }
}
