package com.avrix.provider.zomboid;

import com.avrix.core.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies SteamCMD discovery and strict handling of unreadable active-mod lists.
 */
public class ModDiscoveryLayoutTest {

    @TempDir
    private Path directory;

    /**
     * Verifies nested and ancestor Steam libraries are both scanned.
     *
     * @throws IOException if fixture files cannot be created
     */
    @Test
    public void shouldDiscoverNestedAndAncestorWorkshopContent() throws IOException {
        Path game = Files.createDirectories(directory.resolve("steamapps").resolve("common").resolve("Server"));
        Path cache = directory.resolve("cache");
        Path config = activeList(cache, Environment.SERVER);
        Files.writeString(config, "Mods=nested-mod;library-mod\n");
        Path nested = mod(game.resolve("steamapps"), "1", "nested-mod");
        Path library = mod(directory.resolve("steamapps"), "2", "library-mod");

        assertThat(source(game, cache, Environment.SERVER, true).scan())
                .containsExactlyInAnyOrder(nested.toRealPath(), library.toRealPath());
    }

    /**
     * Verifies SteamCMD installations without any steamapps ancestor.
     *
     * @throws IOException if fixture files cannot be created
     */
    @Test
    public void shouldDiscoverStandaloneSteamCmdServer() throws IOException {
        Path game = Files.createDirectories(directory.resolve("server"));
        Path cache = directory.resolve("cache");
        Files.writeString(activeList(cache, Environment.SERVER), "Mods=nested-mod\n");
        Path plugin = mod(game.resolve("steamapps"), "1", "nested-mod");

        assertThat(source(game, cache, Environment.SERVER, true).scan()).containsExactly(plugin.toRealPath());
    }

    /**
     * Verifies that malformed UTF-8 is an error rather than an empty list in strict mode.
     *
     * @param environment active-mod list format
     * @throws IOException if fixture files cannot be created
     */
    @ParameterizedTest
    @EnumSource(value = Environment.class, names = {"CLIENT", "SERVER"})
    public void shouldRejectUnreadableActiveListInStrictMode(Environment environment) throws IOException {
        Path game = Files.createDirectories(directory.resolve("game"));
        Path cache = directory.resolve("cache");
        Path config = activeList(cache, environment);
        Files.write(config, new byte[]{(byte) 0xc3, (byte) 0x28});

        assertThatThrownBy(() -> source(game, cache, environment, true).scan())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(config.toString());
        assertThat(source(game, cache, environment, false).scan()).isEmpty();
    }

    /**
     * Verifies that missing optional configuration is still allowed in strict mode.
     *
     * @param environment active-mod list format
     * @throws IOException if fixture directories cannot be created
     */
    @ParameterizedTest
    @EnumSource(value = Environment.class, names = {"CLIENT", "SERVER"})
    public void shouldAllowMissingActiveList(Environment environment) throws IOException {
        Path game = Files.createDirectories(directory.resolve("game"));
        assertThat(source(game, directory.resolve("missing-cache"), environment, true).scan()).isEmpty();
    }

    private ZomboidModPluginSource source(Path game, Path cache, Environment environment, boolean strict) {
        return ZomboidModPluginSource.forCacheRoot(game, cache, environment, "servertest", "42.21.0", strict);
    }

    private Path activeList(Path cache, Environment environment) throws IOException {
        Path file = environment == Environment.SERVER ? cache.resolve("Server").resolve("servertest.ini")
                : cache.resolve("mods").resolve("default.txt");
        Files.createDirectories(file.getParent());
        return file;
    }

    private Path mod(Path steamApps, String item, String id) throws IOException {
        Path version = steamApps.resolve("workshop").resolve("content").resolve("108600").resolve(item)
                .resolve("mods").resolve(id).resolve("42");
        Files.createDirectories(version);
        Files.writeString(version.resolve("mod.info"), "id=" + id + "\njarPlugins=plugin.jar\n");
        return Files.createFile(version.resolve("plugin.jar"));
    }
}
