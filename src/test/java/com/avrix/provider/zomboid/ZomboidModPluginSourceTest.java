package com.avrix.provider.zomboid;

import com.avrix.core.Environment;
import com.avrix.api.PluginArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit and stress tests for {@link ZomboidModPluginSource}.
 */
@DisplayName("ZomboidModPluginSource Unit Tests")
class ZomboidModPluginSourceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should load jars only from active local mods and support comma/semicolon separators")
    void shouldLoadJarsFromActiveLocalMods() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");

        Path mods = home.resolve("Zomboid/mods");
        writeMod(mods, "ActiveMod", "active-mod",
                "media/java/a.jar; media/java/b.jar",
                "media/java/a.jar", "media/java/b.jar");
        writeMod(mods, "InactiveMod", "inactive-mod",
                "media/java/c.jar",
                "media/java/c.jar");

        List<Path> result = new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder("a.jar", "b.jar");
    }

    @Test
    @DisplayName("Should ignore inactive mods even when they declare plugins")
    void shouldIgnoreInactiveMods() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");

        Path mods = home.resolve("Zomboid/mods");
        writeMod(mods, "InactiveMod", "inactive-mod",
                "media/java/inactive.jar",
                "media/java/inactive.jar");

        assertThat(new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan()).isEmpty();
    }

    @Test
    @DisplayName("Should expose the mod author in archive attributes")
    void shouldExposeModAuthor() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "author-mod");

        Path mods = home.resolve("Zomboid/mods");
        writeMod(mods, "AuthorMod", "author-mod",
                "media/java/author.jar",
                "media/java/author.jar");

        List<PluginArchive> archives = new ZomboidModPluginSource(
                gameRoot,
                home,
                Environment.CLIENT,
                null
        ).scanArchives();

        assertThat(archives).hasSize(1);
        assertThat(archives.get(0).attributes())
                .containsEntry(PluginArchive.ATTRIBUTE_AUTHOR, "Test Author");
    }

    @Test
    @DisplayName("Should discover plugins from Steam Workshop mods")
    void shouldDiscoverWorkshopJars() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "workshop-mod");

        Path workshopMods = tempDir.resolve("steamapps/workshop/content/108600/123/mods");
        writeMod(workshopMods, "WorkshopMod", "workshop-mod",
                "media/java/workshop.jar",
                "media/java/workshop.jar");

        List<Path> result = new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("workshop.jar");
    }

    @Test
    @DisplayName("Should resolve common mod.info against a version-specific jar directory")
    void shouldSupportCommonModInfoWithVersionJar() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "common-mod");

        Path modRoot = home.resolve("Zomboid/mods/CommonMod");
        Path common = modRoot.resolve("common");
        Files.createDirectories(common);
        Files.writeString(common.resolve("mod.info"), """
                id=common-mod
                jarPlugins=media/java/common.jar
                """);

        Path versionJar = modRoot.resolve("42/media/java/common.jar");
        Files.createDirectories(versionJar.getParent());
        Files.createFile(versionJar);

        List<Path> result = new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("common.jar");
    }

    @Test
    @DisplayName("Should reject jarPlugins paths outside the mod directory")
    void shouldRejectPathTraversal() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");

        Path mods = home.resolve("Zomboid/mods");
        writeMod(mods, "TraversalMod", "active-mod", "../../outside.jar");
        Files.createFile(mods.resolve("outside.jar"));

        assertThat(new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan()).isEmpty();
    }

    @Test
    @DisplayName("Should reject any parent traversal even when it stays inside the mod directory")
    void shouldRejectParentTraversalInsideMod() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");

        Path modRoot = home.resolve("Zomboid/mods/ActiveMod");
        Path version = modRoot.resolve("42");
        Files.createDirectories(version);
        Files.writeString(version.resolve("mod.info"), """
                id=active-mod
                jarPlugins=../media/java/inside.jar
                """);

        Path insideJar = modRoot.resolve("media/java/inside.jar");
        Files.createDirectories(insideJar.getParent());
        Files.createFile(insideJar);

        assertThat(new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan()).isEmpty();

        assertThatThrownBy(() -> new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, "42.21.0", true
        ).scan())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not contain '..'");
    }

    @Test
    @DisplayName("Should reject absolute jarPlugins paths")
    void shouldRejectAbsolutePaths() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");

        Path version = home.resolve("Zomboid/mods/ActiveMod/42");
        Files.createDirectories(version);
        Path outsideJar = tempDir.resolve("outside.jar");
        Files.createFile(outsideJar);
        Files.writeString(version.resolve("mod.info"), """
                id=active-mod
                jarPlugins=%s
                """.formatted(outsideJar.toAbsolutePath()));

        assertThat(new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan()).isEmpty();

        assertThatThrownBy(() -> new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, "42.21.0", true
        ).scan())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Absolute or rooted");
    }

    @Test
    @DisplayName("Should read active mods from the dedicated-server Mods line")
    void shouldReadServerActiveMods() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();

        Path serverDir = home.resolve("Zomboid/Server");
        Files.createDirectories(serverDir);
        Files.writeString(serverDir.resolve("myserver.ini"), """
                PublicName=Test
                Mods=active-mod;other-mod
                """);

        Path mods = home.resolve("Zomboid/mods");
        writeMod(mods, "ActiveMod", "active-mod",
                "media/java/server.jar",
                "media/java/server.jar");

        List<Path> result = new ZomboidModPluginSource(gameRoot, home, Environment.SERVER, "myserver").scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("server.jar");
    }

    @Test
    @DisplayName("Should return empty results when no active mods are configured")
    void shouldReturnEmptyWithoutActiveMods() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeMod(home.resolve("Zomboid/mods"), "InactiveMod", "inactive-mod",
                "media/java/inactive.jar",
                "media/java/inactive.jar");

        assertThat(new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan()).isEmpty();
    }

    @Test
    @DisplayName("Should deduplicate the same jar listed twice")
    void shouldDeduplicateJars() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");

        Path mods = home.resolve("Zomboid/mods");
        writeMod(mods, "DuplicateMod", "active-mod",
                "media/java/a.jar, media/java/a.jar",
                "media/java/a.jar");

        assertThat(new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan())
                .hasSize(1);
    }

    @Test
    @DisplayName("Should select the highest version directory at or below the game version")
    void shouldSelectVersionByGameVersion() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "versioned-mod");

        Path modRoot = home.resolve("Zomboid/mods/VersionedMod");
        writeVersionMod(modRoot, "42.20", "versioned-mod",
                "media/java/42.20.jar", "media/java/42.20.jar");
        writeVersionMod(modRoot, "42.21", "versioned-mod",
                "media/java/42.21.jar", "media/java/42.21.jar");

        List<Path> result = new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, "42.20.0", false
        ).scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("42.20.jar");
    }

    @Test
    @DisplayName("Should use the nearest newer version directory when no compatible version exists")
    void shouldUseNearestNewerVersion() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "versioned-mod");

        Path modRoot = home.resolve("Zomboid/mods/VersionedMod");
        writeVersionMod(modRoot, "42.20", "versioned-mod",
                "media/java/42.20.jar", "media/java/42.20.jar");
        writeVersionMod(modRoot, "42.21", "versioned-mod",
                "media/java/42.21.jar", "media/java/42.21.jar");

        List<Path> result = new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, "42.19.0", false
        ).scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("42.20.jar");
    }

    @Test
    @DisplayName("Should use the newest version directory when the game version is unknown")
    void shouldUseNewestVersionWhenGameVersionUnknown() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "versioned-mod");

        Path modRoot = home.resolve("Zomboid/mods/VersionedMod");
        writeVersionMod(modRoot, "42.20", "versioned-mod",
                "media/java/42.20.jar", "media/java/42.20.jar");
        writeVersionMod(modRoot, "42.21", "versioned-mod",
                "media/java/42.21.jar", "media/java/42.21.jar");

        List<Path> result = new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, null, false
        ).scan();

        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .containsExactly("42.21.jar");
    }

    @Test
    @DisplayName("Should abort in strict mode when an active mod declares a missing jar")
    void shouldAbortInStrictModeForMissingJar() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();
        writeClientActiveMods(home, "active-mod");
        writeMod(home.resolve("Zomboid/mods"), "MissingMod", "active-mod",
                "media/java/missing.jar");

        assertThatThrownBy(() -> new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, "42.21.0", true
        ).scan())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing.jar");

        assertThat(new ZomboidModPluginSource(
                gameRoot, home, Environment.CLIENT, null, "42.21.0", false
        ).scan()).isEmpty();
    }

    @Test
    @Timeout(60)
    @DisplayName("Should scan more than 1000 mods and load plugins only from active ones")
    void shouldScanManyMods() throws Exception {
        Path home = createUserHome();
        Path gameRoot = createGameRoot();

        String[] activeModIds = new String[300];
        for (int index = 0; index < activeModIds.length; index++) {
            activeModIds[index] = "active-" + String.format("%04d", index);
        }
        writeClientActiveMods(home, activeModIds);

        Path mods = home.resolve("Zomboid/mods");
        int expectedJars = 0;

        for (int index = 0; index < 1200; index++) {
            boolean active = index < 300;
            String modId = (active ? "active-" : "inactive-") + String.format("%04d", index);
            String folder = (active ? "ActiveMod" : "InactiveMod") + String.format("%04d", index);

            String prefix = active ? "plugin-" : "inactive-";
            if (index < 100) {
                String firstJar = "media/java/" + prefix + index + "-a.jar";
                String secondJar = "media/java/" + prefix + index + "-b.jar";
                writeMod(mods, folder, modId, firstJar + ";" + secondJar, firstJar, secondJar);
                if (active) {
                    expectedJars += 2;
                }
            } else {
                String jar = "media/java/" + prefix + index + ".jar";
                writeMod(mods, folder, modId, jar, jar);
                if (active) {
                    expectedJars++;
                }
            }
        }

        List<Path> result = new ZomboidModPluginSource(gameRoot, home, Environment.CLIENT, null).scan();

        assertThat(result).hasSize(expectedJars);
        assertThat(result)
                .extracting(path -> path.getFileName().toString())
                .noneMatch(name -> name.startsWith("inactive-"));
    }

    private Path createUserHome() throws IOException {
        Path home = tempDir.resolve("home");
        Files.createDirectories(home);
        return home;
    }

    private Path createGameRoot() throws IOException {
        Path root = tempDir.resolve("steamapps/common/ProjectZomboid");
        Files.createDirectories(root);
        return root;
    }

    private static void writeClientActiveMods(Path home, String... modIds) throws IOException {
        Path mods = home.resolve("Zomboid/mods");
        Files.createDirectories(mods);

        StringBuilder text = new StringBuilder("VERSION = 1,\n\nmods\n{\n");
        for (String modId : modIds) {
            text.append("    mod = ").append(modId).append(",\n");
        }
        text.append("}\n\nmaps\n{\n}\n");
        Files.writeString(mods.resolve("default.txt"), text.toString());
    }

    private static Path writeMod(
            Path modsRoot,
            String folder,
            String id,
            String jarPlugins,
            String... jars
    ) throws IOException {
        Path version = modsRoot.resolve(folder).resolve("42");
        Files.createDirectories(version);
        Files.writeString(version.resolve("mod.info"), """
                id=%s
                author=Test Author
                jarPlugins=%s
                """.formatted(id, jarPlugins));

        for (String jar : jars) {
            Path jarPath = version.resolve(jar);
            Files.createDirectories(jarPath.getParent());
            Files.createFile(jarPath);
        }

        return version;
    }

    private static Path writeVersionMod(
            Path modRoot,
            String versionDirectory,
            String id,
            String jarPlugins,
            String... jars
    ) throws IOException {
        Path version = modRoot.resolve(versionDirectory);
        Files.createDirectories(version);
        Files.writeString(version.resolve("mod.info"), """
                id=%s
                author=Test Author
                jarPlugins=%s
                """.formatted(id, jarPlugins));

        for (String jar : jars) {
            Path jarPath = version.resolve(jar);
            Files.createDirectories(jarPath.getParent());
            Files.createFile(jarPath);
        }

        return version;
    }
}
