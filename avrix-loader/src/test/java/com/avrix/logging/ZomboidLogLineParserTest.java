package com.avrix.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZomboidLogLineParserTest {

    private PrintStream originalOut;
    private PrintStream originalErr;

    private ByteArrayOutputStream outBuffer;
    private ByteArrayOutputStream errBuffer;

    @BeforeEach
    public void setUp() {
        originalOut = System.out;
        originalErr = System.err;

        outBuffer = new ByteArrayOutputStream();
        errBuffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void routesLevelsAndMessages_asExpected() {
        ZomboidLogLineParser parser = new ZomboidLogLineParser(Logger::info);

        parser.accept("LOG  : Custom       f:0, t:1769544320272> [!] Error message");
        parser.accept("LOG  : General      f:0, t:1769544306381> texturepack: loading WeatherFx");
        parser.accept("ERROR: General      f:0, t:1769544307853> FluidContainerScript.load           > Sanitizing container name 'Large Bucket', name may not contain whitespaces.");
        parser.accept("WARN : Lua          f:0, t:1769544309880> LuaManager$GlobalObject.require     > require(\"ISUI/ISInventoryPaneContextMenu\") failed");
        parser.accept("TRACE  : Lua          f:0, t:1769544312127> OnLoadSoundbanks");
        parser.accept("LOG  : General      f:0, t:1769544313939> 1769544313939 fmod: Create resampler: OK");
        parser.accept("DEBUG  : General      f:0, t:1769544312315> texturepack: loading Clock2x");
        parser.accept("LOG  : General      f:0, t:1769547129639, st:211 777 625> Backup made in 83 ms");

        System.out.flush();
        System.err.flush();

        String all = outBuffer.toString(StandardCharsets.UTF_8) + errBuffer.toString(StandardCharsets.UTF_8);

        assertContainsLogLine(all, "ERROR", "Error message");
        assertContainsLogLine(all, "INFO", "texturepack: loading WeatherFx");
        assertContainsLogLine(all, "ERROR", "FluidContainerScript.load > Sanitizing container name 'Large Bucket', name may not contain whitespaces.");
        assertContainsLogLine(all, "WARN", "LuaManager$GlobalObject.require > require(\"ISUI/ISInventoryPaneContextMenu\") failed");
        assertContainsLogLine(all, "TRACE", "OnLoadSoundbanks");
        assertContainsLogLine(all, "INFO", "fmod: Create resampler: OK");
        assertContainsLogLine(all, "DEBUG", "texturepack: loading Clock2x");
        assertContainsLogLine(all, "INFO", "Backup made in 83 ms");
    }

    @Test
    public void unknownFormat_goesToFallbackSink() {
        ZomboidLogLineParser parser = new ZomboidLogLineParser(Logger::info);

        parser.accept("some random line");
        System.out.flush();

        String all = outBuffer.toString(StandardCharsets.UTF_8) + errBuffer.toString(StandardCharsets.UTF_8);
        assertContainsLogLine(all, "INFO", "some random line");
    }

    @Test
    public void nullOrBlank_isIgnored() {
        ZomboidLogLineParser parser = new ZomboidLogLineParser(Logger::info);

        parser.accept(null);
        parser.accept("");
        parser.accept("   ");

        System.out.flush();
        System.err.flush();

        String all = outBuffer.toString(StandardCharsets.UTF_8) + errBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(all.isBlank(), "Expected no output for null/blank input");
    }

    private static void assertContainsLogLine(String allOutput, String level, String expectedMessageLiteral) {
        // Example line:
        // 27-01-2026 23:48:26.794 [ Test worker ] WARN  > message
        String pattern =
                "(?m)^\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[ .*? \\] "
                        + Pattern.quote(level)
                        + "\\s+>\\s+"
                        + Pattern.quote(expectedMessageLiteral)
                        + "\\s*$";

        assertTrue(Pattern.compile(pattern).matcher(allOutput).find(),
                "Expected log line with level=" + level + " and message=" + expectedMessageLiteral
                        + "\nActual output:\n" + allOutput);
    }
}
