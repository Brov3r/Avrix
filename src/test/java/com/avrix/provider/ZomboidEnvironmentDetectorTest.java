package com.avrix.provider;

import com.avrix.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidEnvironmentDetector}.
 */
@DisplayName("ZomboidEnvironmentDetector Tests")
class ZomboidEnvironmentDetectorTest {

    @TempDir
    Path tempDir;

    private String previousEnvironment;

    @AfterEach
    void tearDown() {
        if (previousEnvironment == null) {
            System.clearProperty("avrix.environment");
        } else {
            System.setProperty("avrix.environment", previousEnvironment);
        }
    }

    @Test
    @DisplayName("Should detect a Linux server by start-server.sh")
    void shouldDetectLinuxServerByStartScript() throws Exception {
        Files.createFile(tempDir.resolve("start-server.sh"));

        Environment environment = new ZomboidEnvironmentDetector("Linux").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should detect a Linux client by its shell launcher")
    void shouldDetectLinuxClientByShellLauncher() throws Exception {
        Files.createFile(tempDir.resolve("ProjectZomboid64.sh"));

        Environment environment = new ZomboidEnvironmentDetector("Linux").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.CLIENT);
    }

    @Test
    @DisplayName("Should detect a macOS client by its application bundle")
    void shouldDetectMacClientByApplicationBundle() throws Exception {
        Path bundle = tempDir.resolve("Project Zomboid.app");
        Files.createDirectories(bundle.resolve("Contents"));
        Files.createFile(bundle.resolve("Contents").resolve("Info.plist"));

        Environment environment = new ZomboidEnvironmentDetector("Mac OS X").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.CLIENT);
    }

    @Test
    @DisplayName("Should detect the server from an escaped mainClass value")
    void shouldDetectServerFromEscapedMainClass() throws Exception {
        Files.writeString(
                tempDir.resolve("ProjectZomboid64.json"),
                "{\"mainClass\":\"zombie\\/network\\/GameServer\"}"
        );

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should detect the server from a custom StartServer script content")
    void shouldDetectServerFromCustomStartScriptContent() throws Exception {
        Files.writeString(
                tempDir.resolve("StartServerCustom.bat"),
                "\"jre64/bin/java.exe\" -cp java/projectzomboid.jar zombie.network.GameServer"
        );

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should not treat ProjectZomboidServer.bat as a dedicated server marker")
    void shouldNotTreatClientServerScriptAsDedicatedServer() throws Exception {
        Files.writeString(
                tempDir.resolve("ProjectZomboidServer.bat"),
                "\"jre64/bin/java.exe\" -cp projectzomboid.jar zombie.network.GameServer"
        );
        Files.createFile(tempDir.resolve("projectzomboid.jar"));
        Files.createDirectories(tempDir.resolve("jre64"));

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.CLIENT);
    }

    @Test
    @DisplayName("Should fall back to the pzserver directory name")
    void shouldFallBackToPzServerDirectoryName() throws Exception {
        Path serverDirectory = tempDir.resolve("pzserver");
        Files.createDirectories(serverDirectory);

        Environment environment = new ZomboidEnvironmentDetector("Linux").detect(serverDirectory);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should match marker files without regard to case")
    void shouldMatchMarkerFilesIgnoringCase() throws Exception {
        Files.createFile(tempDir.resolve("STARTSERVER64.BAT"));

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should honor an explicit environment override")
    void shouldHonorExplicitEnvironmentOverride() throws Exception {
        previousEnvironment = System.getProperty("avrix.environment");
        System.setProperty("avrix.environment", "server");

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(tempDir);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }

    @Test
    @DisplayName("Should detect a client from the Steam app manifest")
    void shouldDetectClientFromSteamManifest() throws Exception {
        Path install = tempDir.resolve("steamapps").resolve("common").resolve("ProjectZomboid");
        Files.createDirectories(install);
        Files.writeString(
                tempDir.resolve("steamapps").resolve("appmanifest_108600.acf"),
                "\"AppState\"\n{\n\t\"installdir\"\t\t\"ProjectZomboid\"\n}\n"
        );

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(install);

        assertThat(environment).isEqualTo(Environment.CLIENT);
    }

    @Test
    @DisplayName("Should detect a server from the Steam app manifest")
    void shouldDetectServerFromSteamManifest() throws Exception {
        Path install = tempDir.resolve("steamapps").resolve("common")
                .resolve("Project Zomboid Dedicated Server");
        Files.createDirectories(install);
        Files.writeString(
                tempDir.resolve("steamapps").resolve("appmanifest_380870.acf"),
                "\"AppState\"\n{\n\t\"installdir\"\t\t\"Project Zomboid Dedicated Server\"\n}\n"
        );

        Environment environment = new ZomboidEnvironmentDetector("Windows 11").detect(install);

        assertThat(environment).isEqualTo(Environment.SERVER);
    }
}
