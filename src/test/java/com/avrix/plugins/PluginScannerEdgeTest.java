package com.avrix.plugins;

import com.avrix.core.LoaderConfig;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge-case tests for {@link PluginScanner} disabled-list parsing and filesystem guards.
 */
@DisplayName("PluginScanner Edge Case Tests")
class PluginScannerEdgeTest {

    @TempDir
    Path tempDir;

    private PluginScanner scanner() {
        return new PluginScanner(LoaderConfig.defaults().withPluginsDirectory(tempDir));
    }

    @Test
    @DisplayName("Should return an empty set when disabled.txt is malformed UTF-8")
    void shouldReturnEmptyForMalformedDisabledFile() throws IOException {
        // The reader uses UTF-8; an incomplete multi-byte sequence fails the read with an IOException.
        Files.write(
                tempDir.resolve(Constants.DISABLED_PLUGINS_FILE_NAME),
                new byte[]{'p', 'l', 'u', 'g', 'i', 'n', (byte) 0xC3}
        );

        assertThat(scanner().readDisabledPlugins()).isEmpty();
    }

    @Test
    @DisplayName("Should treat a disabled.txt directory as a missing file")
    void shouldIgnoreDisabledFileWhenItIsADirectory() throws IOException {
        Files.createDirectories(tempDir.resolve(Constants.DISABLED_PLUGINS_FILE_NAME));

        assertThat(scanner().readDisabledPlugins()).isEmpty();
    }

    @Test
    @DisplayName("Should ignore comment-only and whitespace-only disabled lines")
    void shouldIgnoreCommentOnlyDisabledFile() throws IOException {
        Files.writeString(
                tempDir.resolve(Constants.DISABLED_PLUGINS_FILE_NAME),
                "#\n   #   \n\t# trailing comment\n",
                StandardCharsets.UTF_8
        );

        assertThat(scanner().readDisabledPlugins()).isEmpty();
    }

    @Test
    @DisplayName("Should keep parsing entries after a UTF-8 BOM-prefixed first line")
    void shouldKeepParsingAfterBomPrefixedLine() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = "# header\nPlugin-ID\n".getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, content, 0, bom.length);
        System.arraycopy(body, 0, content, bom.length, body.length);

        Files.write(tempDir.resolve(Constants.DISABLED_PLUGINS_FILE_NAME), content);

        // Files.readAllLines does not strip a BOM, so only later entries are guaranteed to be clean.
        assertThat(scanner().readDisabledPlugins()).contains("plugin-id");
    }

    @Test
    @DisplayName("Should return an empty list for a readable empty plugins directory")
    void shouldReturnEmptyForEmptyPluginsDirectory() {
        assertThat(scanner().scan()).isEmpty();
    }
}
