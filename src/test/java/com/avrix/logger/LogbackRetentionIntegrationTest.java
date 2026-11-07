package com.avrix.logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the production configuration creates a separate log file per logger lifecycle. */
class LogbackRetentionIntegrationTest {

    @TempDir
    private Path logDirectory;

    @Test
    void shouldCreateSeparateLogFileForEachLaunch() throws Exception {
        logSession("first session");
        Thread.sleep(10);
        logSession("second session");

        List<Path> logFiles = listLogFiles();
        assertThat(logFiles).hasSize(2);
        assertThat(logFiles).allSatisfy(path ->
                assertThat(path.getFileName().toString()).matches("avrix-.*\\.log"));

        List<String> contents = new ArrayList<>();
        for (Path path : logFiles) {
            contents.add(Files.readString(path));
        }
        assertThat(contents).anySatisfy(content -> assertThat(content).contains("first session"));
        assertThat(contents).anySatisfy(content -> assertThat(content).contains("second session"));
    }

    @Test
    void shouldConfigureRollingRetention() throws Exception {
        LoggerContext context = configure();
        try {
            var appender = (RollingFileAppender<?>) context.getLogger("ROOT").getAppender("FILE");
            assertThat(appender).isNotNull();
            assertThat(appender.isStarted()).isTrue();
            assertThat(appender.getFile()).contains("avrix-").endsWith(".log");

            var policy = (SizeAndTimeBasedRollingPolicy<?>) appender.getRollingPolicy();
            assertThat(policy.getMaxHistory()).isEqualTo(7);
            assertThat(policy.isCleanHistoryOnStart()).isTrue();
        } finally {
            context.stop();
        }
    }

    private List<Path> listLogFiles() throws Exception {
        try (var files = Files.list(logDirectory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("avrix-"))
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .toList();
        }
    }

    private void logSession(String message) throws Exception {
        LoggerContext context = configure();
        try {
            context.getLogger("retention-test").info(message);
        } finally {
            context.stop();
        }
    }

    private LoggerContext configure() throws Exception {
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        context.putProperty("avrix.log.dir", logDirectory.toString());
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(getClass().getClassLoader().getResource("logback.xml"));
        return context;
    }
}
