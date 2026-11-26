package com.avrix.provider.zomboid;

import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginArchive;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.provider.zomboid.approval.MixinTargetInspector;
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
import java.util.Locale;
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
    @DisplayName("Should keep prompt policy on a server when the frontend is interactive")
    void shouldKeepPromptOnServerForInteractiveFrontend() {
        ZomboidPluginApprovalGate service = service(
                Environment.SERVER,
                "prompt",
                new ZomboidApprovalFrontend() {
                    /** {@inheritDoc} */
                    @Override
                    public ZomboidApprovalResponse request(List<ZomboidApprovalEntry> pending) {
                        return new ZomboidApprovalResponse(false, decisions(pending, true));
                    }

                    /** {@inheritDoc} */
                    @Override
                    public boolean supportsInteractivePrompt() {
                        return true;
                    }
                }
        );

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        service.flushPending();

        assertThat(service.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
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
    @DisplayName("Should not attribute another plugin's approval to an archive of the same mod")
    void shouldNotReusePreviousHashAcrossPluginsOfOneMod() throws Exception {
        Path alphaJar = tempDir.resolve("alpha.jar");
        Path betaJar = tempDir.resolve("beta.jar");
        Files.writeString(alphaJar, "alpha-v1");
        Files.writeString(betaJar, "beta-v1");

        AtomicReference<List<ZomboidApprovalEntry>> alphaBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate first = service(Environment.CLIENT, "prompt", allowFrontend(true, alphaBatch));
        assertThat(first.evaluate(archive(alphaJar, "shared-mod"), metadata("alpha")))
                .isEqualTo(PluginApprovalGate.Status.PENDING);
        first.flushPending();
        String alphaHash = alphaBatch.get().get(0).sha256();

        // Keep the beta decision strictly newer so a mod-wide lookup would select it for alpha.
        Thread.sleep(5);

        AtomicReference<List<ZomboidApprovalEntry>> betaBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate second = service(Environment.CLIENT, "prompt", allowFrontend(true, betaBatch));
        assertThat(second.evaluate(archive(betaJar, "shared-mod"), metadata("beta")))
                .isEqualTo(PluginApprovalGate.Status.PENDING);
        second.flushPending();
        assertThat(betaBatch.get()).hasSize(1);
        assertThat(betaBatch.get().get(0).previousSha256()).isEmpty();
        String betaHash = betaBatch.get().get(0).sha256();

        Files.writeString(alphaJar, "alpha-v2");
        AtomicReference<List<ZomboidApprovalEntry>> changedBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate third = service(Environment.CLIENT, "prompt", pending -> {
            changedBatch.set(List.copyOf(pending));
            return new ZomboidApprovalResponse(false, List.of());
        });
        assertThat(third.evaluate(archive(alphaJar, "shared-mod"), metadata("alpha")))
                .isEqualTo(PluginApprovalGate.Status.PENDING);
        third.flushPending();
        assertThat(changedBatch.get()).hasSize(1);
        assertThat(changedBatch.get().get(0).previousSha256()).isEqualTo(alphaHash);

        ZomboidPluginApprovalGate reloaded = service(Environment.CLIENT, "deny-new", null);
        assertThat(reloaded.evaluate(archive(betaJar, "shared-mod"), metadata("beta")))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(reloaded.evaluate(archive(alphaJar, "shared-mod"), metadata("alpha"), alphaHash))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(Files.readString(approvalsFile)).contains(alphaHash, betaHash);
    }

    @Test
    @DisplayName("Should load version 2 decisions and not attribute them as a previous plugin version")
    void shouldKeepLegacyDecisionWithoutPluginIdentity() throws IOException {
        Path legacyJar = tempDir.resolve("legacy.jar");
        Files.writeString(legacyJar, "legacy-v1");

        AtomicReference<List<ZomboidApprovalEntry>> firstBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate first = service(Environment.CLIENT, "prompt", allowFrontend(true, firstBatch));
        assertThat(first.evaluate(archive(legacyJar, "legacy-mod"), metadata("legacy-plugin")))
                .isEqualTo(PluginApprovalGate.Status.PENDING);
        first.flushPending();
        String legacyHash = firstBatch.get().get(0).sha256();

        // Rewrite the store in the version 2 layout, which has no plugin identity column.
        StringBuilder legacyStore = new StringBuilder("VERSION\t2\n");
        for (String line : Files.readAllLines(approvalsFile)) {
            if (line.startsWith("APPROVAL\t")) {
                String[] parts = line.split("\t", -1);
                legacyStore.append(String.join("\t", List.of(parts).subList(0, 8))).append('\n');
            }
        }
        Files.writeString(approvalsFile, legacyStore.toString());

        ZomboidPluginApprovalGate legacy = service(Environment.CLIENT, "deny-new", null);
        assertThat(legacy.evaluate(archive(legacyJar, "legacy-mod"), metadata("legacy-plugin")))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);

        Files.writeString(legacyJar, "legacy-v2");
        AtomicReference<List<ZomboidApprovalEntry>> changedBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate updated = service(Environment.CLIENT, "prompt", allowFrontend(true, changedBatch));
        assertThat(updated.evaluate(archive(legacyJar, "legacy-mod"), metadata("legacy-plugin")))
                .isEqualTo(PluginApprovalGate.Status.PENDING);
        updated.flushPending();
        assertThat(changedBatch.get()).hasSize(1);
        assertThat(changedBatch.get().get(0).previousSha256()).isEmpty();
        String updatedHash = changedBatch.get().get(0).sha256();

        assertThat(Files.readString(approvalsFile)).contains(legacyHash, updatedHash);

        ZomboidPluginApprovalGate reloaded = service(Environment.CLIENT, "deny-new", null);
        assertThat(reloaded.evaluate(archive(legacyJar, "legacy-mod"), metadata("legacy-plugin"), legacyHash))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(reloaded.evaluate(archive(legacyJar, "legacy-mod"), metadata("legacy-plugin")))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);
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
    @DisplayName("Should retry an unsaved decision after a temporary store failure even with nothing pending")
    void shouldRetryUnsavedDecisionWhenPendingIsEmpty() throws IOException {
        Path otherJar = tempDir.resolve("other.jar");
        Files.writeString(otherJar, "other-content");
        Metadata otherMetadata = metadata("other-plugin");

        AtomicReference<List<ZomboidApprovalEntry>> seedBatch = new AtomicReference<>();
        ZomboidPluginApprovalGate seed = service(Environment.CLIENT, "prompt", allowFrontend(true, seedBatch));
        assertThat(seed.evaluate(archive(otherJar, "test-mod"), otherMetadata))
                .isEqualTo(PluginApprovalGate.Status.PENDING);
        seed.flushPending();
        String otherHash = seedBatch.get().get(0).sha256();
        assertThat(approvalsFile).isRegularFile();

        // Make the store temporarily unwritable while keeping the approvals already on disk.
        Path backup = tempDir.resolve("plugin-approvals.backup");
        Files.move(approvalsFile, backup);
        Files.createDirectory(approvalsFile);

        AtomicReference<List<ZomboidApprovalEntry>> batch = new AtomicReference<>();
        ZomboidPluginApprovalGate gate = service(Environment.CLIENT, "prompt", allowFrontend(true, batch));
        assertThat(gate.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.PENDING);
        gate.flushPending();
        String hash = batch.get().get(0).sha256();
        assertThat(gate.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(approvalsFile).isDirectory();

        // The store recovers; this flush has no pending archives and must still persist the queued decision.
        Files.delete(approvalsFile);
        Files.move(backup, approvalsFile);
        gate.flushPending();

        assertThat(approvalsFile).isRegularFile();
        assertThat(Files.readString(approvalsFile)).contains(otherHash, hash);

        ZomboidPluginApprovalGate reloaded = service(Environment.CLIENT, "deny-new", null);
        assertThat(reloaded.evaluate(archive(), metadata())).isEqualTo(PluginApprovalGate.Status.APPROVED);
        assertThat(reloaded.evaluate(archive(otherJar, "test-mod"), otherMetadata))
                .isEqualTo(PluginApprovalGate.Status.APPROVED);
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

    @Test
    @DisplayName("Should expose schema 2 effective mixins for client, server, and both plugin environments")
    void shouldExposeEffectiveMixinsForActiveSide() throws IOException {
        List<String> commonMixins = List.of("test.mixin.CommonMixin");
        List<String> clientMixins = List.of("test.mixin.ClientMixin");
        List<String> serverMixins = List.of("test.mixin.ServerMixin");

        for (Environment active : List.of(Environment.CLIENT, Environment.SERVER)) {
            for (Environment pluginEnvironment : List.of(Environment.CLIENT, Environment.SERVER, Environment.BOTH)) {
                Path jar = tempDir.resolve("mixins-" + active + "-" + pluginEnvironment + ".jar");
                Files.writeString(jar, active + "-" + pluginEnvironment);

                AtomicReference<List<ZomboidApprovalEntry>> batch = new AtomicReference<>();
                ZomboidPluginApprovalGate gate = service(active, "prompt", interactiveAllowFrontend(false, batch));
                Metadata metadata = schema2Metadata(
                        "plugin-" + environmentId(active) + "-" + environmentId(pluginEnvironment),
                        pluginEnvironment,
                        commonMixins,
                        clientMixins,
                        serverMixins
                );

                assertThat(gate.evaluate(archive(jar, "test-mod"), metadata))
                        .isEqualTo(PluginApprovalGate.Status.PENDING);
                gate.flushPending();

                assertThat(batch.get()).hasSize(1);
                List<String> visibleMixins = batch.get().get(0).mixinClasses();
                assertThat(visibleMixins).isEqualTo(metadata.mixinsFor(active));
                assertThat(visibleMixins).containsAll(commonMixins);
                if (active == Environment.CLIENT) {
                    assertThat(visibleMixins).containsAll(clientMixins);
                    assertThat(visibleMixins).doesNotContainAnyElementsOf(serverMixins);
                } else {
                    assertThat(visibleMixins).containsAll(serverMixins);
                    assertThat(visibleMixins).doesNotContainAnyElementsOf(clientMixins);
                }
            }
        }
    }

    @Test
    @DisplayName("Should not inspect declarations of the side opposite to the active environment")
    void shouldNotInspectOppositeSide() throws IOException {
        record Case(Environment active, List<String> clientMixins, List<String> serverMixins,
                    List<String> excludedMixins) {
        }
        List<Case> cases = List.of(
                new Case(Environment.CLIENT, List.of(), List.of("test.mixin.ServerMixin"),
                        List.of("test.mixin.ServerMixin")),
                new Case(Environment.SERVER, List.of("test.mixin.ClientMixin"), List.of(),
                        List.of("test.mixin.ClientMixin"))
        );

        for (Case testCase : cases) {
            Path jar = tempDir.resolve("opposite-" + testCase.active() + ".jar");
            Files.writeString(jar, "opposite-side-content");

            AtomicReference<List<ZomboidApprovalEntry>> batch = new AtomicReference<>();
            ZomboidPluginApprovalGate gate = service(
                    testCase.active(),
                    "prompt",
                    interactiveAllowFrontend(false, batch)
            );
            Metadata metadata = schema2Metadata(
                    "opposite-" + environmentId(testCase.active()),
                    Environment.BOTH,
                    List.of(),
                    testCase.clientMixins(),
                    testCase.serverMixins()
            );

            assertThat(gate.evaluate(archive(jar, "test-mod"), metadata))
                    .isEqualTo(PluginApprovalGate.Status.PENDING);
            gate.flushPending();

            ZomboidApprovalEntry entry = batch.get().get(0);
            assertThat(entry.mixinClasses()).isEqualTo(metadata.mixinsFor(testCase.active()));
            assertThat(entry.mixinClasses()).doesNotContainAnyElementsOf(testCase.excludedMixins());
            Environment opposite = testCase.active() == Environment.CLIENT ? Environment.SERVER : Environment.CLIENT;
            assertThat(metadata.mixinsFor(opposite)).containsExactlyElementsOf(testCase.excludedMixins());

            // A missing archive proves the inspector does not open archives without effective mixins.
            Files.delete(jar);
            MixinTargetInspector.Result inspection =
                    MixinTargetInspector.inspect(entry.jarPath(), entry.mixinClasses());
            assertThat(inspection.mixinClasses()).isEmpty();
            assertThat(inspection.targetClasses()).isEmpty();
            assertThat(inspection.issues()).isEmpty();
        }
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

    private static ZomboidApprovalFrontend interactiveAllowFrontend(
            boolean persist,
            AtomicReference<List<ZomboidApprovalEntry>> batch
    ) {
        return new ZomboidApprovalFrontend() {
            /**
             * Captures the pending batch and approves every entry.
             *
             * @param pending archives awaiting approval
             * @return approval response for the captured batch
             */
            @Override
            public ZomboidApprovalResponse request(List<ZomboidApprovalEntry> pending) {
                batch.set(List.copyOf(pending));
                return new ZomboidApprovalResponse(persist, decisions(pending, true));
            }

            /**
             * Reports interactive support so the prompt policy stays active on a dedicated server.
             *
             * @return always {@code true}
             */
            @Override
            public boolean supportsInteractivePrompt() {
                return true;
            }
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
        return archive(pluginJar, "test-mod");
    }

    private static PluginArchive archive(Path jar, String modId) {
        return new PluginArchive(
                jar.toFile(),
                modId,
                "Test Mod",
                PluginArchive.SOURCE_MOD,
                null,
                false
        );
    }

    private Metadata metadata() {
        return metadata("test-plugin");
    }

    private static Metadata metadata(String id) {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA)
                .id(id)
                .name("Test Plugin")
                .version("1.0.0")
                .environment(Environment.BOTH)
                .build();
    }

    private static String environmentId(Environment environment) {
        return environment.name().toLowerCase(Locale.ROOT);
    }

    private static Metadata schema2Metadata(
            String id,
            Environment environment,
            List<String> commonMixins,
            List<String> clientMixins,
            List<String> serverMixins
    ) {
        return new Metadata.Builder()
                .schema(Constants.METADATA_SCHEMA_2)
                .id(id)
                .name("Test Plugin")
                .version("1.0.0")
                .environment(environment)
                .mixins(commonMixins)
                .clientMixins(clientMixins)
                .serverMixins(serverMixins)
                .build();
    }
}
