package com.avrix.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Branch-focused tests for {@link IsolatedConsoleAppender} lifecycle and routing edge cases.
 * <p>
 * The real stdout/stderr descriptors are replaced with in-memory buffers through reflection so
 * that the tests never write to the process console.
 */
@DisplayName("IsolatedConsoleAppender Branch Tests")
class IsolatedConsoleAppenderBranchTest {

    private LoggerContext context;
    private Logger logger;
    private IsolatedConsoleAppender appender;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private StubEncoder encoder;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        logger = context.getLogger("com.avrix.logger.branch");
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
    @DisplayName("Should route events below, at, and above the error threshold")
    void shouldRouteEventsAroundErrorThreshold() {
        encoder.payload = event -> event.getLevel().toString().getBytes(StandardCharsets.UTF_8);
        appender.setEncoder(encoder);
        appender.setErrorLevel("ERROR");
        appender.start();

        appender.append(event(Level.TRACE, "trace"));
        appender.append(event(Level.DEBUG, "debug"));
        appender.append(event(Level.INFO, "info"));
        appender.append(event(Level.WARN, "warn"));
        appender.append(event(Level.ERROR, "error"));

        assertThat(outText()).isEqualTo("TRACEDEBUGINFOWARN");
        assertThat(errText()).isEqualTo("ERROR");
        assertThat(encoder.encodeCount).isEqualTo(5);
    }

    @Test
    @DisplayName("Should skip null and empty encoder output")
    void shouldSkipNullAndEmptyEncoderOutput() {
        appender.setEncoder(encoder);
        appender.start();

        encoder.payload = event -> null;
        appender.append(event(Level.ERROR, "null payload"));

        encoder.payload = event -> new byte[0];
        appender.append(event(Level.ERROR, "empty payload"));

        assertThat(encoder.encodeCount).isEqualTo(2);
        assertThat(outText()).isEmpty();
        assertThat(errText()).isEmpty();
        assertThat(errorStatusMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should not restart an encoder that is already started")
    void shouldNotRestartStartedEncoder() {
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        assertThat(encoder.startCount).isEqualTo(1);
        assertThat(appender.isStarted()).isTrue();
    }

    @Test
    @DisplayName("Should ignore stop when the appender is not started")
    void shouldIgnoreStopWhenNotStarted() {
        appender.setEncoder(encoder);

        assertThatCode(appender::stop).doesNotThrowAnyException();
        assertThatCode(appender::stop).doesNotThrowAnyException();

        assertThat(appender.isStarted()).isFalse();
        assertThat(encoder.stopCount).isZero();
    }

    @Test
    @DisplayName("Should skip encoder stop when the encoder is not started")
    void shouldSkipStoppingAlreadyStoppedEncoder() {
        StubEncoder replacement = new StubEncoder();
        appender.setEncoder(encoder);
        appender.start();
        appender.setEncoder(replacement);

        appender.stop();

        assertThat(replacement.stopCount).isZero();
        assertThat(appender.isStarted()).isFalse();
    }

    @Test
    @DisplayName("Should report a flush failure during shutdown")
    void shouldReportFlushFailureDuringStop() {
        appender.setEncoder(encoder);
        appender.start();
        replaceField(appender, "outStream", new OutputStream() {
            @Override
            public void write(int value) {
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("flush failure");
            }
        });

        appender.stop();

        assertThat(appender.isStarted()).isFalse();
        assertThat(errorStatusMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Failed to flush output streams during appender shutdown"));
    }

    @Test
    @DisplayName("Should report a header write failure without aborting start")
    void shouldReportHeaderWriteFailure() {
        encoder.header = "HEAD".getBytes(StandardCharsets.UTF_8);
        appender.setEncoder(encoder);
        replaceField(appender, "outStream", new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("write failure");
            }
        });

        appender.start();

        assertThat(appender.isStarted()).isTrue();
        assertThat(errorStatusMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Failed to write metadata bytes to output stream"));
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
    @DisplayName("Should report a write failure during append")
    void shouldReportWriteFailureDuringAppend() {
        appender.setEncoder(encoder);
        appender.start();
        replaceField(appender, "errStream", new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("write failure");
            }
        });
        encoder.payload = event -> "payload".getBytes(StandardCharsets.UTF_8);

        appender.append(event(Level.ERROR, "failing"));

        assertThat(errorStatusMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Failed to write log event directly to isolated console descriptors"));
    }

    @Test
    @DisplayName("Should fall back to WARN for null, blank, and invalid error levels")
    void shouldFallBackToWarnForInvalidErrorLevels() {
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

    private LoggingEvent event(Level level, String message) {
        return new LoggingEvent(
                "com.avrix.logger.IsolatedConsoleAppenderBranchTest",
                logger, level, message, null, null);
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
