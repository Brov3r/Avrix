package com.avrix.provider;

import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginArchive;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidPluginApprovalGate}.
 */
@DisplayName("ZomboidPluginApprovalGate Unit Tests")
class ZomboidPluginApprovalGateTest {

    @TempDir
    Path tempDir;

    private Path gameRoot;
    private Path userHome;
    private Path pluginJar;

    private Path gamePending;
    private Path luaPending;
    private Path luaDecisions;
    private Path gameApprovals;

    @BeforeEach
    void setUp() throws IOException {
        gameRoot = tempDir.resolve("game");
        userHome = tempDir.resolve("home");
        pluginJar = tempDir.resolve("plugin.jar");
        Files.createDirectories(gameRoot);
        Files.writeString(pluginJar, "initial-content");

        Path gameData = gameRoot.resolve("avrix");
        Path luaData = userHome.resolve("Zomboid/Lua/avrix");
        gamePending = gameData.resolve("plugin-pending.txt");
        luaPending = luaData.resolve("plugin-pending.txt");
        luaDecisions = luaData.resolve("plugin-decisions.txt");
        gameApprovals = gameData.resolve("plugin-approvals.txt");
    }

    @Test
    @DisplayName("Should write pending list to the game folder and Lua mirror")
    void shouldWritePendingToGameFolderAndLuaMirror() throws IOException {
        ZomboidPluginApprovalGate service = service(Environment.CLIENT, "prompt");

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(gamePending).isRegularFile();
        assertThat(luaPending).isRegularFile();
        assertThat(Files.readString(gamePending))
                .contains("ENTRY\t", "test-mod", "NEW", "test-plugin");
        assertThat(Files.readString(luaPending)).isEqualTo(Files.readString(gamePending));
        assertThat(gameApprovals).doesNotExist();
    }

    @Test
    @DisplayName("Should load an approved hash after a Lua decision and remove the decision file")
    void shouldApplyLuaDecision() throws IOException {
        ZomboidPluginApprovalGate firstRun = service(Environment.CLIENT, "prompt");
        assertThat(firstRun.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        firstRun.flushPending();

        String hash = pendingHash();
        Files.writeString(luaDecisions, "VERSION\t1\nDECISION\ttest-mod\t" + hash + "\ttrue\n");

        ZomboidPluginApprovalGate secondRun = service(Environment.CLIENT, "prompt");
        assertThat(secondRun.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);

        assertThat(gameApprovals).isRegularFile();
        assertThat(Files.readString(gameApprovals)).contains("APPROVAL", "test-mod", hash, "true");
        assertThat(luaDecisions).doesNotExist();
    }

    @Test
    @DisplayName("Should deny unknown plugins under deny-new policy without a pending entry")
    void shouldDenyUnknownUnderDenyNew() throws IOException {
        ZomboidPluginApprovalGate service = service(Environment.CLIENT, "deny-new");

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.DENIED);
        service.flushPending();

        assertThat(gamePending).isRegularFile();
        assertThat(Files.readString(gamePending)).doesNotContain("ENTRY");
    }

    @Test
    @DisplayName("Should load everything under allow-all policy")
    void shouldAllowAll() {
        ZomboidPluginApprovalGate service = service(Environment.CLIENT, "allow-all");

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
    }

    @Test
    @DisplayName("Should bypass approval for trusted plugins-directory archives")
    void shouldTrustPluginsDirectoryArchives() {
        ZomboidPluginApprovalGate service = service(Environment.CLIENT, "prompt");
        PluginArchive trusted = PluginArchive.trusted(pluginJar.toFile());

        assertThat(service.evaluate(trusted, metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
    }

    @Test
    @DisplayName("Should require approval again after the archive hash changes")
    void shouldRequireApprovalAfterHashChange() throws IOException {
        ZomboidPluginApprovalGate firstRun = service(Environment.CLIENT, "prompt");
        firstRun.evaluate(archive(), metadata());
        firstRun.flushPending();

        String hash = pendingHash();
        Files.writeString(luaDecisions, "VERSION\t1\nDECISION\ttest-mod\t" + hash + "\ttrue\n");
        assertThat(service(Environment.CLIENT, "prompt").evaluate(archive(), metadata()))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);

        Files.writeString(pluginJar, "changed-content");

        ZomboidPluginApprovalGate changedRun = service(Environment.CLIENT, "prompt");
        assertThat(changedRun.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        changedRun.flushPending();

        assertThat(Files.readString(gamePending)).contains("ENTRY", "CHANGED");
    }

    @Test
    @DisplayName("Should treat prompt as deny-new on a dedicated server")
    void shouldFallbackToDenyNewOnServer() {
        ZomboidPluginApprovalGate service = service(Environment.SERVER, "prompt");

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.DENIED);
    }

    @Test
    @DisplayName("Should write the Lua mirror under the configured cache root")
    void shouldWriteLuaMirrorUnderConfiguredCacheRoot() {
        Path customCacheRoot = tempDir.resolve("custom-cache");
        ZomboidPluginApprovalGate service = new ZomboidPluginApprovalGate(
                gameRoot, customCacheRoot, Environment.CLIENT, "prompt"
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(customCacheRoot.resolve("Lua/avrix/plugin-pending.txt")).isRegularFile();
        assertThat(userHome.resolve("Zomboid/Lua/avrix/plugin-pending.txt")).doesNotExist();
    }

    private ZomboidPluginApprovalGate service(Environment environment, String policy) {
        return new ZomboidPluginApprovalGate(gameRoot, userHome.resolve("Zomboid"), environment, policy);
    }

    private PluginArchive archive() {
        return new PluginArchive(pluginJar.toFile(), "test-mod", "Test Mod", PluginArchive.SOURCE_MOD, null, false);
    }

    private Metadata metadata() {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .build();
    }

    private String pendingHash() throws IOException {
        List<String> lines = Files.readAllLines(gamePending);
        for (String line : lines) {
            String[] parts = line.split("\t", -1);
            if (parts.length >= 5 && "ENTRY".equals(parts[0])) {
                return parts[4];
            }
        }
        throw new AssertionError("No pending entry found in " + gamePending);
    }
}
