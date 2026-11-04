package com.avrix.provider;

import com.avrix.core.Environment;
import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Project Zomboid client/server environment detection.
 */
@DisplayName("ZomboidGameProvider Environment Detection Tests")
class ZomboidGameProviderDetectionTest {

    @TempDir
    Path tempDir;

    private String previousGameDir;

    @AfterEach
    void tearDown() {
        if (previousGameDir == null) {
            System.clearProperty("avrix.game.dir");
        } else {
            System.setProperty("avrix.game.dir", previousGameDir);
        }
    }

    @Test
    @DisplayName("Should detect the client by executable markers")
    void shouldDetectClientByExecutableMarker() throws Exception {
        Files.createFile(tempDir.resolve("ProjectZomboid64.exe"));
        Files.createDirectories(tempDir.resolve("jre64"));
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isFalse();
        assertThat(provider.getEnvironment()).isEqualTo(Environment.CLIENT);
        assertThat(provider.getEntrypoint()).isEqualTo("zombie.gameStates.MainScreenState");
        assertThat(provider.getName()).isEqualTo("Project Zomboid");
    }

    @Test
    @DisplayName("Should detect the dedicated server by start scripts")
    void shouldDetectServerByStartScript() throws Exception {
        Files.createFile(tempDir.resolve("StartServer64.bat"));
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isTrue();
        assertThat(provider.getEnvironment()).isEqualTo(Environment.SERVER);
        assertThat(provider.getEntrypoint()).isEqualTo("zombie.network.GameServer");
        assertThat(provider.getName()).isEqualTo("Project Zomboid Dedicated Server");
    }

    @Test
    @DisplayName("Should detect the dedicated server by java archive and native libraries")
    void shouldDetectServerByLayout() throws Exception {
        Files.createDirectories(tempDir.resolve("java"));
        Files.createDirectories(tempDir.resolve("natives"));
        Files.createFile(tempDir.resolve("java").resolve("projectzomboid.jar"));
        Files.createFile(tempDir.resolve("natives").resolve("PZBullet64.dll"));
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isTrue();
        assertThat(provider.getEnvironment()).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should not treat empty server directories as a dedicated server")
    void shouldNotDetectServerFromEmptyLayout() throws Exception {
        Files.createDirectories(tempDir.resolve("java"));
        Files.createDirectories(tempDir.resolve("natives"));
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isFalse();
        assertThat(provider.getEnvironment()).isEqualTo(Environment.CLIENT);
    }

    @Test
    @DisplayName("Should prefer the client launch configuration over server-looking directories")
    void shouldPreferClientLaunchConfigOverServerLayout() throws Exception {
        Files.createDirectories(tempDir.resolve("java"));
        Files.createDirectories(tempDir.resolve("natives"));
        Files.writeString(
                tempDir.resolve("ProjectZomboid64.json"),
                "{\"mainClass\":\"zombie/gameStates/MainScreenState\"}"
        );
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isFalse();
        assertThat(provider.getEnvironment()).isEqualTo(Environment.CLIENT);
    }

    @Test
    @DisplayName("Should detect the server from a dot-separated launch configuration main class")
    void shouldDetectServerFromDotSeparatedMainClass() throws Exception {
        Files.writeString(
                tempDir.resolve("ProjectZomboid64.json"),
                "{\"mainClass\":\"zombie.network.GameServer\"}"
        );
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isTrue();
    }

    @Test
    @DisplayName("Should ignore server class mentions outside mainClass")
    void shouldIgnoreServerClassOutsideMainClass() throws Exception {
        Files.writeString(
                tempDir.resolve("ProjectZomboid64.json"),
                "{\"mainClass\":\"zombie/gameStates/MainScreenState\","
                        + "\"comment\":\"zombie/network/GameServer\"}"
        );
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isFalse();
    }

    @Test
    @DisplayName("Should resolve a macOS application bundle to its game directory")
    void shouldResolveMacBundleGameDirectory() throws Exception {
        Path bundle = tempDir.resolve("Project Zomboid.app");
        Path gameRoot = bundle.resolve("Contents").resolve("Resources").resolve("Java");
        Files.createDirectories(gameRoot);
        Files.createFile(gameRoot.resolve("projectzomboid.jar"));

        previousGameDir = System.getProperty("avrix.game.dir");
        System.setProperty("avrix.game.dir", bundle.toAbsolutePath().toString());

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.getLaunchDirectory()).isEqualTo(gameRoot.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("Should resolve the version directly from the game archive")
    void shouldResolveVersionFromGameArchive() throws Exception {
        VersionJarFixture.createGameArchive(tempDir, 42, 21, "", 0, "4a0e9546ec");
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.getRawVersion()).isEqualTo("42.21.0 4a0e9546ec");
        assertThat(provider.getNormalizedVersion()).isEqualTo("42.21.0");
    }

    @Test
    @DisplayName("Should prefer server markers when both layouts are present")
    void shouldPreferServerMarkers() throws Exception {
        Files.createFile(tempDir.resolve("ProjectZomboid64.exe"));
        Files.createFile(tempDir.resolve("StartServer64.bat"));
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isTrue();
        assertThat(provider.getProviderArgs()).containsEntry("java.awt.headless", "true");
    }

    @Test
    @DisplayName("Should detect the server from the launch configuration main class")
    void shouldDetectServerFromLaunchConfig() throws Exception {
        Files.writeString(
                tempDir.resolve("ProjectZomboid64.json"),
                "{\"mainClass\":\"zombie/network/GameServer\"}"
        );
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isTrue();
    }

    @Test
    @DisplayName("Should detect the client from the launch configuration main class")
    void shouldDetectClientFromLaunchConfig() throws Exception {
        Files.writeString(
                tempDir.resolve("ProjectZomboid64.json"),
                "{\"mainClass\":\"zombie/gameStates/MainScreenState\"}"
        );
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isFalse();
    }

    @Test
    @DisplayName("Should fall back to the dedicated-server directory name")
    void shouldFallBackToDirectoryName() throws Exception {
        Path serverDirectory = tempDir.resolve("Project Zomboid Dedicated Server");
        Files.createDirectories(serverDirectory);
        useGameDirectory(serverDirectory);

        ZomboidGameProvider provider = new ZomboidGameProvider();

        assertThat(provider.isServer()).isTrue();
    }

    @Test
    @DisplayName("Should reject a directory that does not look like a game installation")
    void shouldRejectInvalidInstallation() throws Exception {
        Path emptyDirectory = Files.createDirectories(tempDir.resolve("empty"));
        useGameDirectory(emptyDirectory);

        try (KnotClassLoader classLoader = new KnotClassLoader(new URL[0], getClass().getClassLoader())) {
            ZomboidGameProvider provider = new ZomboidGameProvider();

            assertThatThrownBy(() -> provider.init(classLoader))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("does not look like a Project Zomboid installation");
        }
    }

    @Test
    @DisplayName("Should use client arguments for a client installation")
    void shouldUseClientArgumentsForClient() throws Exception {
        Files.createFile(tempDir.resolve("ProjectZomboid64.exe"));
        useGameDirectory(tempDir);

        ZomboidGameProvider provider = new ZomboidGameProvider();
        Map<String, String> properties = provider.getProviderArgs();

        assertThat(properties)
                .containsEntry("zomboid.steam", "1")
                .containsEntry("zomboid.znetlog", "1")
                .doesNotContainKey("java.awt.headless");
    }

    private void useGameDirectory(Path directory) {
        previousGameDir = System.getProperty("avrix.game.dir");
        System.setProperty("avrix.game.dir", directory.toAbsolutePath().toString());
    }
}
