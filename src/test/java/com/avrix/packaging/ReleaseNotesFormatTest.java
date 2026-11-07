package com.avrix.packaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that release subjects are newline-terminated for the workflow's shell read loop.
 */
public class ReleaseNotesFormatTest {

    @TempDir
    private Path repository;

    /**
     * Verifies the workflow's actual git format for both single-commit and multi-commit releases.
     *
     * @throws Exception if the temporary repository or git commands fail
     */
    @Test
    public void shouldTerminateEveryReleaseSubject() throws Exception {
        String workflow = Files.readString(Path.of(".github", "workflows", "release.yml"));
        var matcher = Pattern.compile("git log --no-merges --pretty=([^\\s]+)").matcher(workflow);
        assertThat(matcher.find()).isTrue();
        String pretty = matcher.group(1).replace("'", "");
        git("init", "-b", "main");
        for (int index = 1; index <= 3; index++) {
            git("-c", "user.name=Avrix Tests", "-c", "user.email=tests@example.invalid", "-c", "commit.gpgsign=false",
                    "commit", "--allow-empty", "--no-verify", "-m", "fix: release change " + index);
        }
        for (int count : new int[]{1, 3}) {
            String output = git("log", "--no-merges", "--pretty=" + pretty, "--max-count=" + count);
            assertThat(output).endsWith("\n");
            assertThat(output.lines()).hasSize(count);
            assertThat(output).contains("fix: release change 3");
        }
    }

    private String git(String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(arguments));
        Path output = Files.createTempFile(repository, "git-output-", ".txt");
        Process process = new ProcessBuilder(command).directory(repository.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertThat(process.waitFor(15, TimeUnit.SECONDS)).isTrue();
            String text = Files.readString(output);
            assertThat(process.exitValue()).withFailMessage(text).isZero();
            return text;
        } finally {
            process.destroyForcibly();
        }
    }
}
