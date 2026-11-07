package com.avrix.provider.zomboid;

import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginArchive;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.provider.zomboid.approval.ZomboidApprovalDecision;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import com.avrix.provider.zomboid.approval.ZomboidApprovalFrontend;
import com.avrix.provider.zomboid.approval.ZomboidApprovalResponse;
import com.avrix.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZomboidPluginApprovalGate}.
 */
@DisplayName("ZomboidPluginApprovalGate Unit Tests")
class ZomboidPluginApprovalGateTest {

    @TempDir
    Path tempDir;

    private Path configRoot;
    private Path pluginJar;
    private Path approvalsFile;

    @BeforeEach
    void setUp() throws IOException {
        configRoot = tempDir.resolve(".avrix");
        pluginJar = tempDir.resolve("plugin.jar");
        Files.writeString(pluginJar, "initial-content");
        approvalsFile = configRoot.resolve("plugin-approvals.txt");
    }

    @Test
    @DisplayName("Should show one pending batch and load the archive after allow")
    void shouldLoadAfterAllow() throws IOException {
        AtomicReference<List<ZomboidApprovalEntry>> batch = new AtomicReference<>();
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                allowFrontend(true, batch)
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(batch.get()).hasSize(1);
        String hash = batch.get().get(0).sha256();
        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(approvalsFile).isRegularFile();
        assertThat(Files.readString(approvalsFile)).contains(hash, "\ttrue\t");
    }

    @Test
    @DisplayName("Should create the store only inside the .avrix directory")
    void shouldUseOnlyTheConfiguredStoreFile() {
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                allowFrontend(true, new AtomicReference<>())
        );

        service.evaluate(archive(), metadata());
        service.flushPending();

        assertThat(approvalsFile).isRegularFile();
        assertThat(tempDir.resolve("avrix/plugin-approvals.txt")).doesNotExist();
    }

    @Test
    @DisplayName("Should skip an archive when the frontend returns no decisions")
    void shouldSkipWhenFrontendCancels() {
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                pending -> new ZomboidApprovalResponse(false, List.of())
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        assertThat(approvalsFile).doesNotExist();
    }

    @Test
    @DisplayName("Should persist an explicit denial and block the archive")
    void shouldPersistDenial() throws IOException {
        AtomicReference<List<ZomboidApprovalEntry>> batch = new AtomicReference<>();
        ZomboidPluginApprovalGate service = service(Environment.CLIENT, "prompt", pending -> {
            batch.set(List.copyOf(pending));
            return new ZomboidApprovalResponse(true, decisions(pending, false));
        });

        service.evaluate(archive(), metadata());
        service.flushPending();

        String hash = batch.get().get(0).sha256();
        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.DENIED);
        assertThat(Files.readString(approvalsFile)).contains(hash, "\tfalse\t");
    }

    @Test
    @DisplayName("Should include the source mod author and id in the pending entry")
    void shouldIncludeModAuthorAndId() {
        AtomicReference<List<ZomboidApprovalEntry>> batch = new AtomicReference<>();
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                allowFrontend(false, batch)
        );

        PluginArchive archive = new PluginArchive(
                pluginJar.toFile(),
                "test-mod",
                "Test Mod",
                PluginArchive.SOURCE_WORKSHOP,
                Map.of(PluginArchive.ATTRIBUTE_AUTHOR, "Mod Author", PluginArchive.ATTRIBUTE_WORKSHOP_ID, "123"),
                false
        );

        service.evaluate(archive, metadata());
        service.flushPending();

        assertThat(batch.get()).hasSize(1);
        assertThat(batch.get().get(0).modId()).isEqualTo("test-mod");
        assertThat(batch.get().get(0).modAuthor()).isEqualTo("Mod Author");
    }

    @Test
    @DisplayName("Should keep a non-persisted decision for the current gate only")
    void shouldKeepSessionOnlyDecision() {
        ZomboidPluginApprovalGate firstRun = service(
                Environment.CLIENT,
                "prompt",
                allowFrontend(false, new AtomicReference<>())
        );
        firstRun.evaluate(archive(), metadata());
        firstRun.flushPending();

        assertThat(firstRun.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(approvalsFile).doesNotExist();

        ZomboidPluginApprovalGate secondRun = service(
                Environment.CLIENT,
                "prompt",
                ZomboidApprovalFrontend.denyAll()
        );
        assertThat(secondRun.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
    }

    @Test
    @DisplayName("Should deny unknown plugins under deny-new without calling the frontend")
    void shouldDenyUnknownUnderDenyNew() {
        AtomicBoolean called = new AtomicBoolean();
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "deny-new",
                pending -> {
                    called.set(true);
                    return new ZomboidApprovalResponse(false, List.of());
                }
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.DENIED);
        service.flushPending();

        assertThat(called).isFalse();
        assertThat(approvalsFile).doesNotExist();
    }

    @Test
    @DisplayName("Should load everything under allow-all without persisting")
    void shouldAllowAll() {
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "allow-all",
                ZomboidApprovalFrontend.denyAll()
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(approvalsFile).doesNotExist();
    }

    @Test
    @DisplayName("Should bypass approval for trusted plugins-directory archives")
    void shouldTrustPluginsDirectoryArchives() {
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                ZomboidApprovalFrontend.denyAll()
        );
        PluginArchive trusted = PluginArchive.trusted(pluginJar.toFile());

        assertThat(service.evaluate(trusted, metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
    }

    @Test
    @DisplayName("Should require approval again after the archive hash changes")
    void shouldRequireApprovalAfterHashChange() throws IOException {
        AtomicReference<List<ZomboidApprovalEntry>> firstBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate firstRun = service(
                Environment.CLIENT,
                "prompt",
                allowFrontend(true, firstBatch)
        );
        firstRun.evaluate(archive(), metadata());
        firstRun.flushPending();
        String oldHash = firstBatch.get().get(0).sha256();

        Files.writeString(pluginJar, "changed-content");

        AtomicReference<List<ZomboidApprovalEntry>> secondBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate secondRun = service(
                Environment.CLIENT,
                "prompt",
                pending -> {
                    secondBatch.set(List.copyOf(pending));
                    return new ZomboidApprovalResponse(false, List.of());
                }
        );

        assertThat(secondRun.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        secondRun.flushPending();
        assertThat(secondBatch.get()).hasSize(1);
        assertThat(secondBatch.get().get(0).previousSha256()).isEqualTo(oldHash);
    }

    @Test
    @DisplayName("Should treat prompt as deny-new on a dedicated server")
    void shouldFallbackToDenyNewOnServer() {
        ZomboidPluginApprovalGate service = service(
                Environment.SERVER,
                "prompt",
                ZomboidApprovalFrontend.denyAll()
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.DENIED);
    }

    @Test
    @DisplayName("Should keep decisions session-only when the store is unavailable")
    void shouldKeepSessionOnlyWhenStoreIsUnavailable() throws IOException {
        Path blocked = tempDir.resolve("config-as-file");
        Files.writeString(blocked, "not a directory");

        ZomboidPluginApprovalGate service = new ZomboidPluginApprovalGate(
                blocked,
                Environment.CLIENT,
                "prompt",
                allowFrontend(true, new AtomicReference<>())
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(blocked).isRegularFile();
    }

    @Test
    @DisplayName("Should reject decisions for hashes outside the pending batch")
    void shouldRejectUnknownDecision() {
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                pending -> new ZomboidApprovalResponse(
                        false,
                        List.of(new ZomboidApprovalDecision("deadbeef", true))
                )
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
    }

    @Test
    @DisplayName("Should fail closed when the frontend throws")
    void shouldFailClosedWhenFrontendThrows() {
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                pending -> {
                    throw new IOException("frontend failed");
                }
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
    }

    @Test
    @DisplayName("Should not call the frontend when nothing is pending")
    void shouldNotCallFrontendWithoutPending() {
        AtomicBoolean called = new AtomicBoolean();
        ZomboidPluginApprovalGate service = service(
                Environment.CLIENT,
                "prompt",
                pending -> {
                    called.set(true);
                    return new ZomboidApprovalResponse(false, List.of());
                }
        );

        service.flushPending();

        assertThat(called).isFalse();
    }

    private ZomboidPluginApprovalGate service(
            Environment environment,
            String policy,
            ZomboidApprovalFrontend frontend
    ) {
        return new ZomboidPluginApprovalGate(
                configRoot,
                environment,
                policy,
                frontend
        );
    }

    private static ZomboidApprovalFrontend allowFrontend(
            boolean persist,
            AtomicReference<List<ZomboidApprovalEntry>> batch
    ) {
        return pending -> {
            batch.set(List.copyOf(pending));
            return new ZomboidApprovalResponse(persist, decisions(pending, true));
        };
    }

    private static List<ZomboidApprovalDecision> decisions(List<ZomboidApprovalEntry> entries, boolean allow) {
        List<ZomboidApprovalDecision> decisions = new ArrayList<>(entries.size());
        for (ZomboidApprovalEntry entry : entries) {
            decisions.add(new ZomboidApprovalDecision(entry.sha256(), allow));
        }
        return decisions;
    }

    private PluginArchive archive() {
        return new PluginArchive(
                pluginJar.toFile(),
                "test-mod",
                "Test Mod",
                PluginArchive.SOURCE_MOD,
                null,
                false
        );
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
}
