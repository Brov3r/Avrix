package com.avrix.logger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LineReadingOutputStream} byte buffering, delimiter handling, and decoding.
 */
@DisplayName("LineReadingOutputStream Unit Tests")
class LineReadingOutputStreamTest {

    private final List<String> lines = new ArrayList<>();

    /** Verifies that automatic PrintStream flushes preserve a logical line and its final remainder. */
    @Test
    public void shouldPreserveLinesAcrossAutoFlushingPrintCalls() {
        try (var stream = new PrintStream(new LineReadingOutputStream(lines::add, StandardCharsets.UTF_8, false),
                true, StandardCharsets.UTF_8)) {
            stream.print("hello ");
            stream.flush();
            assertThat(lines).isEmpty();
            stream.println("world");
            stream.print("tail");
            assertThat(lines).containsExactly("hello world");
        }
        assertThat(lines).containsExactly("hello world", "tail");
    }

    /** Verifies split UTF-8 characters and CRLF delimiters through an auto-flushing PrintStream. */
    @Test
    public void shouldPreserveUtf8AndCrLfAcrossAutoFlushingWrites() {
        try (var stream = new PrintStream(new LineReadingOutputStream(lines::add, StandardCharsets.UTF_8, false),
                true, StandardCharsets.UTF_8)) {
            byte[] bytes = "Привет 🌍\r\n".getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < bytes.length; i++) {
                stream.write(bytes, i, 1);
            }
        }
        assertThat(lines).containsExactly("Привет 🌍");
    }

    @Test
    @DisplayName("Should emit a line when a single LF byte is written")
    void shouldEmitLineOnLf() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        "hello".chars().forEach(stream::write);
        assertThat(lines).isEmpty();

        stream.write('\n');

        assertThat(lines).containsExactly("hello");
    }

    @Test
    @DisplayName("Should emit a line when a single CR byte is written")
    void shouldEmitLineOnCr() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        "hello".chars().forEach(stream::write);
        stream.write('\r');

        assertThat(lines).containsExactly("hello");
    }

    @Test
    @DisplayName("Should treat a CRLF pair written byte by byte as one delimiter")
    void shouldTreatCrLfAsSingleDelimiter() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        "hello".chars().forEach(stream::write);
        stream.write('\r');
        stream.write('\n');

        assertThat(lines).containsExactly("hello");
    }

    @Test
    @DisplayName("Should not emit a line without a delimiter until flushed")
    void shouldKeepPartialLineUntilFlush() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write('a');
        stream.write('b');

        assertThat(lines).isEmpty();

        stream.flush();

        assertThat(lines).containsExactly("ab");
    }

    @Test
    @DisplayName("Should emit an empty line for a standalone delimiter")
    void shouldEmitEmptyLineForDelimiter() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write('\n');

        assertThat(lines).containsExactly("");
    }

    @Test
    @DisplayName("Should emit one empty line per empty line in a batch")
    void shouldEmitEmptyLinesFromBatch() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("\n\n".getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly("", "");
    }

    @Test
    @DisplayName("Should collapse CRLF pairs into single empty lines in a batch")
    void shouldCollapseCrLfPairsInBatch() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("\r\n\r\n".getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly("", "");
    }

    @Test
    @DisplayName("Should emit every complete line from a single batch write")
    void shouldEmitMultipleLinesFromSingleWrite() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("one\ntwo\nthree".getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly("one", "two");

        stream.flush();

        assertThat(lines).containsExactly("one", "two", "three");
    }

    @Test
    @DisplayName("Should not emit a phantom line for a trailing delimiter")
    void shouldNotEmitPhantomLineAfterTrailingDelimiter() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("only line\n".getBytes(StandardCharsets.UTF_8));
        stream.flush();
        stream.close();

        assertThat(lines).containsExactly("only line");
    }

    @Test
    @DisplayName("Should handle mixed delimiters in a single batch write")
    void shouldHandleMixedDelimitersInSingleWrite() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("one\ntwo\r\nthree\rfour".getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly("one", "two", "three");

        stream.flush();

        assertThat(lines).containsExactly("one", "two", "three", "four");
    }

    @Test
    @DisplayName("Should join CRLF split across two batch writes into one delimiter")
    void shouldJoinCrLfSplitAcrossWrites() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("abc\r".getBytes(StandardCharsets.UTF_8));
        stream.write("\ndef".getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly("abc");

        stream.flush();

        assertThat(lines).containsExactly("abc", "def");
    }

    @Test
    @DisplayName("Should start a new line after a CR at the end of a previous write")
    void shouldStartNewLineAfterPreviousCr() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("abc\r".getBytes(StandardCharsets.UTF_8));
        stream.write("def\n".getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly("abc", "def");
    }

    @Test
    @DisplayName("Should respect offset and length of a batch write")
    void shouldRespectOffsetAndLength() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);
        byte[] payload = "XXline\nYY".getBytes(StandardCharsets.UTF_8);

        stream.write(payload, 2, 5);

        assertThat(lines).containsExactly("line");
    }

    @Test
    @DisplayName("Should ignore zero-length batch writes")
    void shouldIgnoreZeroLengthWrite() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write(new byte[0], 0, 0);
        stream.flush();

        assertThat(lines).isEmpty();
    }

    @Test
    @DisplayName("Should emit the buffered remainder exactly once on flush")
    void shouldEmitRemainderOnceOnFlush() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("tail".getBytes(StandardCharsets.UTF_8));
        stream.flush();
        stream.flush();

        assertThat(lines).containsExactly("tail");
    }

    @Test
    @DisplayName("Should do nothing when flushing an empty stream")
    void shouldDoNothingOnEmptyFlush() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.flush();

        assertThat(lines).isEmpty();
    }

    @Test
    @DisplayName("Should emit the buffered remainder on close and stay idempotent")
    void shouldEmitRemainderOnClose() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("tail".getBytes(StandardCharsets.UTF_8));
        stream.close();
        stream.close();

        assertThat(lines).containsExactly("tail");
    }

    @Test
    @DisplayName("Should not emit anything when closing after a complete line")
    void shouldNotEmitOnCloseAfterCompleteLine() throws IOException {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write("complete\r\n".getBytes(StandardCharsets.UTF_8));
        stream.close();

        assertThat(lines).containsExactly("complete");
    }

    @Test
    @DisplayName("Should decode UTF-8 lines including non-ASCII text")
    void shouldDecodeUtf8Text() throws IOException {
        String text = "Привет, мир! 你好 🌍";
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write((text + "\n").getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly(text);
    }

    @Test
    @DisplayName("Should decode a multi-byte character split across writes")
    void shouldDecodeMultibyteCharacterSplitAcrossWrites() {
        String text = "A🌍B";
        byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        for (byte value : encoded) {
            stream.write(value & 0xFF);
        }
        stream.write('\n');

        assertThat(lines).containsExactly(text);
    }

    @Test
    @DisplayName("Should use the configured charset for decoding")
    void shouldUseConfiguredCharset() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add, StandardCharsets.ISO_8859_1);

        stream.write(0xE9);
        stream.write('\n');

        assertThat(lines).containsExactly("é");
    }

    @Test
    @DisplayName("Should grow the internal buffer for long lines")
    void shouldGrowBufferForLongLines() throws IOException {
        String longLine = "x".repeat(10_000);
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        stream.write((longLine + "\n").getBytes(StandardCharsets.UTF_8));

        assertThat(lines).containsExactly(longLine);
    }

    @Test
    @DisplayName("Should reject a null consumer")
    void shouldRejectNullConsumer() {
        assertThatThrownBy(() -> new LineReadingOutputStream(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lineConsumer");
    }

    @Test
    @DisplayName("Should reject a null charset")
    void shouldRejectNullCharset() {
        assertThatThrownBy(() -> new LineReadingOutputStream(lines::add, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("charset");
    }

    @Test
    @DisplayName("Should reject a null byte array")
    void shouldRejectNullByteArray() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);

        assertThatThrownBy(() -> stream.write(null, 0, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("bytes");
    }

    @Test
    @DisplayName("Should reject invalid offset and length ranges")
    void shouldRejectInvalidRanges() {
        LineReadingOutputStream stream = new LineReadingOutputStream(lines::add);
        byte[] payload = "abc".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> stream.write(payload, -1, 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> stream.write(payload, 1, -1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> stream.write(payload, 0, payload.length + 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("Should emit intact lines from concurrent writers")
    void shouldEmitIntactLinesFromConcurrentWriters() throws Exception {
        int writerCount = 6;
        int linesPerWriter = 200;
        List<String> collected = Collections.synchronizedList(new ArrayList<>());
        LineReadingOutputStream stream = new LineReadingOutputStream(collected::add);
        List<String> expected = new ArrayList<>();

        for (int writer = 0; writer < writerCount; writer++) {
            for (int index = 0; index < linesPerWriter; index++) {
                expected.add("writer-" + writer + "-line-" + index);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(writerCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int writer = 0; writer < writerCount; writer++) {
                int writerId = writer;
                futures.add(executor.submit(() -> {
                    startSignal.await();
                    for (int index = 0; index < linesPerWriter; index++) {
                        String line = "writer-" + writerId + "-line-" + index;
                        stream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                    return null;
                }));
            }

            startSignal.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(collected).containsExactlyInAnyOrderElementsOf(expected);
    }
}
