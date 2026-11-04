package com.avrix.plugins;

import com.avrix.api.PluginArchive;
import com.avrix.api.PluginSource;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Scans the configured plugins directory and provider-supplied plugin sources for plugin archives.
 */
public final class PluginScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginScanner.class);

    private final LoaderConfig config;
    private final List<PluginSource> pluginSources;

    /**
     * Constructs a new plugin scanner that only scans the plugins directory.
     *
     * @param config loader configuration providing the plugins directory and archive extension
     * @throws NullPointerException if {@code config} is null
     */
    public PluginScanner(LoaderConfig config) {
        this(config, List.of());
    }

    /**
     * Constructs a new plugin scanner with additional game-specific plugin sources.
     *
     * @param config        loader configuration providing the plugins directory and archive extension
     * @param pluginSources additional sources supplied by the active game provider
     * @throws NullPointerException if {@code config} or {@code pluginSources} is null
     */
    public PluginScanner(LoaderConfig config, List<PluginSource> pluginSources) {
        this.config = Objects.requireNonNull(config, "Loader config cannot be null");
        this.pluginSources = List.copyOf(Objects.requireNonNull(pluginSources, "Plugin sources cannot be null"));
    }

    /**
     * Scans the configured plugins directory and all provider-supplied sources for plugin archives.
     *
     * @return discovered plugin files, or an empty list when no archive is found
     */
    public List<File> scan() {
        return scanArchives().stream()
                .map(PluginArchive::file)
                .toList();
    }

    /**
     * Scans the configured plugins directory and all provider-supplied sources for plugin archives.
     *
     * @return discovered plugin archives, or an empty list when no archive is found
     */
    public List<PluginArchive> scanArchives() {
        Map<String, PluginArchive> discovered = new LinkedHashMap<>();

        for (File plugin : scanPluginsDirectory()) {
            addDiscovered(discovered, PluginArchive.trusted(plugin));
        }

        for (PluginSource source : pluginSources) {
            for (PluginArchive plugin : source.scanArchives()) {
                addDiscovered(discovered, plugin);
            }
        }

        return discovered.values().stream()
                .sorted(Comparator.comparing(archive -> archive.file().getName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private List<File> scanPluginsDirectory() {
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

    private static void addDiscovered(Map<String, PluginArchive> discovered, PluginArchive plugin) {
        String key;
        try {
            key = plugin.file().getCanonicalPath();
        } catch (IOException e) {
            key = plugin.file().getAbsolutePath();
        }
        discovered.putIfAbsent(key, plugin);
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
