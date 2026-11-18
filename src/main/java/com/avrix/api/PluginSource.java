package com.avrix.api;

import java.util.List;

/**
 * Discovers plugin archives from a game-specific location.
 * <p>
 * Implementations are supplied by a {@link GameProvider} so the loader core can remain independent of any
 * particular game distribution format.
 */
public interface PluginSource {

    /**
     * Scans the source for plugin archives.
     *
     * @return discovered plugin archives, or an empty list when none are found
     */
    List<PluginArchive> scanArchives();
}
