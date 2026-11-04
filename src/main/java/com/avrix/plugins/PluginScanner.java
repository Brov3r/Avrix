package com.avrix.plugins;

import com.avrix.core.LoaderConfig;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Scans the configured plugins directory for plugin archives.
 */
public final class PluginScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginScanner.class);

    private final LoaderConfig config;

    /**
     * Constructs a new plugin scanner.
     *
     * @param config loader configuration providing the plugins directory and archive extension
     * @throws NullPointerException if {@code config} is null
     */
    public PluginScanner(LoaderConfig config) {
        this.config = Objects.requireNonNull(config, "Loader config cannot be null");
    }

    /**
     * Scans the configured plugins directory for regular files with the configured extension.
     *
     * @return discovered plugin files, or an empty list when the directory is missing or cannot be read
     */
    public List<File> scan() {
        Path pluginsDir = config.pluginsDirectory().toAbsolutePath().normalize();
        if (!Files.isDirectory(pluginsDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(pluginsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(config.pluginExtension()))
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                    ))
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            LOGGER.error("Failed to enumerate plugin archives in [{}]", pluginsDir, e);
            return List.of();
        }
    }

    /**
     * Reads disabled plugin entries from {@code disabled.txt} in the plugins directory.
     * <p>
     * Each non-blank line is an entry. Lines starting with {@code #} and inline comments are ignored. Entries may be
     * plugin identifiers or plugin archive names and are matched case-insensitively.
     *
     * @return immutable set of normalized disabled entries, or an empty set when the file is missing or unreadable
     */
    public Set<String> readDisabledPlugins() {
        Path disabledFile = config.pluginsDirectory().resolve(Constants.DISABLED_PLUGINS_FILE_NAME);
        if (!Files.isRegularFile(disabledFile)) {
            return Set.of();
        }

        Set<String> disabledPlugins = new TreeSet<>();
        try {
            for (String line : Files.readAllLines(disabledFile)) {
                String entry = line;
                if (!entry.isEmpty() && entry.charAt(0) == '\uFEFF') {
                    entry = entry.substring(1);
                }
                int commentStart = entry.indexOf('#');
                if (commentStart >= 0) {
                    entry = entry.substring(0, commentStart);
                }
                entry = entry.strip();
                if (!entry.isEmpty()) {
                    disabledPlugins.add(entry.toLowerCase(Locale.ROOT));
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read disabled plugin list [{}]", disabledFile, e);
            return Set.of();
        }

        return Collections.unmodifiableSet(disabledPlugins);
    }
}
