package com.avrix.provider.zomboid;

import com.avrix.api.PluginApprovalGate;
import com.avrix.api.PluginApprovalGate.Status;
import com.avrix.api.PluginArchive;
import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import com.avrix.provider.zomboid.approval.ZomboidApprovalDecision;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;
import com.avrix.provider.zomboid.approval.ZomboidApprovalFrontend;
import com.avrix.provider.zomboid.approval.ZomboidApprovalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Applies the {@code avrix.plugin.policy} policy to plugin archives discovered from active Project Zomboid mods.
 * <p>
 * Decisions are bound to the SHA-256 of the archive and stored in one per-user file under
 * {@link com.avrix.api.GameProvider#getConfigRoot()}. Pending archives are collected in memory and resolved through a
 * {@link ZomboidApprovalFrontend} before the loader retries them in the same process launch.
 */
public final class ZomboidPluginApprovalGate implements PluginApprovalGate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidPluginApprovalGate.class);

    private static final String VERSION_LINE = "VERSION\t2";
    private static final String APPROVALS_FILE_NAME = "plugin-approvals.txt";

    private static final String POLICY_PROMPT = "prompt";
    private static final String POLICY_DENY_NEW = "deny-new";
    private static final String POLICY_ALLOW_ALL = "allow-all";

    private final Path approvalsFile;
    private final Policy policy;
    private final ZomboidApprovalFrontend frontend;

    private final Map<String, StoredDecision> approvals = new HashMap<>();
    private final Map<String, Boolean> sessionDecisions = new HashMap<>();
    private final Set<String> resolvedThisSession = new HashSet<>();
    private final List<ZomboidApprovalEntry> pending = new ArrayList<>();
    private final Set<String> pendingHashes = new HashSet<>();

    private boolean storageAvailable = true;
    private boolean storeDirty;

    /**
     * Creates an approval gate.
     *
     * @param configRoot  per-user directory for the persistent approval store
     * @param environment active runtime environment
     * @param policyValue configured policy value, or {@code null} for the environment default
     * @param frontend    frontend used to resolve pending archives
     * @throws NullPointerException if {@code configRoot} is null
     */
    public ZomboidPluginApprovalGate(
            Path configRoot,
            Environment environment,
            String policyValue,
            ZomboidApprovalFrontend frontend
    ) {
        Path normalizedConfigRoot = Objects.requireNonNull(configRoot, "Config root cannot be null")
                .toAbsolutePath()
                .normalize();

        this.approvalsFile = normalizedConfigRoot.resolve(APPROVALS_FILE_NAME);
        this.policy = resolvePolicy(environment == null ? Environment.CLIENT : environment, policyValue);
        this.frontend = frontend != null ? frontend : ZomboidApprovalFrontend.denyAll();

        prepareStorage();
        loadApprovals();
        LOGGER.info("Workshop plugin approval policy: [{}]", policy.displayName);
    }

    /**
     * Evaluates one archive against the configured policy.
     *
     * @param archive  discovered plugin archive
     * @param metadata parsed plugin metadata
     * @return approval status
     */
    @Override
    public Status evaluate(PluginArchive archive, Metadata metadata) {
        return evaluateInternal(archive, metadata, null);
    }

    /**
     * Evaluates one archive against the configured policy using a pre-computed content hash.
     *
     * @param archive     discovered plugin archive
     * @param metadata    parsed plugin metadata
     * @param contentHash lowercase SHA-256 of the archive, or {@code null} to compute it
     * @return approval status
     */
    @Override
    public Status evaluate(PluginArchive archive, Metadata metadata, String contentHash) {
        return evaluateInternal(archive, metadata, contentHash);
    }

    /**
     * Resolves all pending archives through the configured frontend and persists decisions when requested.
     */
    @Override
    public void flushPending() {
        if (pending.isEmpty()) {
            return;
        }

        List<ZomboidApprovalEntry> batch = List.copyOf(pending);
        LOGGER.info("Waiting for user approval of {} plugin archive(s)", batch.size());
        ZomboidApprovalResponse response = null;
        try {
            response = frontend.request(batch);
        } catch (Throwable throwable) {
            LOGGER.error("Approval frontend failed; pending plugins will be skipped", throwable);
        }

        applyResponse(batch, response);
        pending.clear();
        pendingHashes.clear();
        for (ZomboidApprovalEntry entry : batch) {
            resolvedThisSession.add(entry.sha256());
        }

        if (storeDirty && saveApprovals()) {
            storeDirty = false;
        }
    }

    private Status evaluateInternal(PluginArchive archive, Metadata metadata, String contentHash) {
        if (archive.trusted()) {
            return Status.APPROVED;
        }

        if (policy == Policy.ALLOW_ALL) {
            return Status.APPROVED;
        }

        String modId = archive.sourceId();
        if (modId == null || modId.isBlank()) {
            LOGGER.warn("Plugin archive [{}] has no source mod ID; denying", archive.file());
            return Status.DENIED;
        }

        String sha256;
        if (contentHash != null && !contentHash.isBlank()) {
            sha256 = contentHash;
        } else {
            try {
                sha256 = sha256(archive.file().toPath());
            } catch (IOException e) {
                LOGGER.warn("Failed to hash plugin archive [{}]; denying", archive.file(), e);
                return Status.DENIED;
            }
        }

        Boolean sessionDecision = sessionDecisions.get(sha256);
        if (sessionDecision != null) {
            return sessionDecision ? Status.APPROVED : Status.DENIED;
        }

        StoredDecision stored = approvals.get(sha256);
        if (stored != null) {
            return stored.allow() ? Status.APPROVED : Status.DENIED;
        }

        if (policy == Policy.DENY_NEW) {
            LOGGER.info(
                    "Skipping unapproved plugin [{}] from mod [{}] (policy=deny-new, sha256={})",
                    pluginId(metadata),
                    modId,
                    sha256
            );
            return Status.DENIED;
        }

        if (resolvedThisSession.contains(sha256)) {
            return Status.PENDING;
        }

        if (pendingHashes.add(sha256)) {
            pending.add(createEntry(archive, metadata, modId, sha256));
            LOGGER.info(
                    "Plugin [{}] from mod [{}] is pending approval sha256={}",
                    pluginId(metadata),
                    modId,
                    sha256
            );
        }

        return Status.PENDING;
    }

    private ZomboidApprovalEntry createEntry(
            PluginArchive archive,
            Metadata metadata,
            String modId,
            String sha256
    ) {
        Path jarPath = archive.file().toPath().toAbsolutePath().normalize();
        long size = 0L;
        long modified = 0L;
        try {
            size = Files.size(jarPath);
            modified = Files.getLastModifiedTime(jarPath).toMillis();
        } catch (IOException e) {
            LOGGER.debug("Failed to read plugin archive attributes [{}]", jarPath, e);
        }

        String displayName = metadata != null && metadata.name() != null && !metadata.name().isBlank()
                ? metadata.name()
                : archive.file().getName();
        String modAuthor = archive.attributes().getOrDefault(PluginArchive.ATTRIBUTE_AUTHOR, "");
        if (modAuthor.isBlank() && metadata != null && !metadata.authors().isEmpty()) {
            modAuthor = String.join(", ", metadata.authors());
        }
        String workshopId = archive.attributes().getOrDefault(PluginArchive.ATTRIBUTE_WORKSHOP_ID, "");

        return new ZomboidApprovalEntry(
                modId,
                pluginId(metadata),
                modAuthor,
                displayName,
                archive.sourceType(),
                archive.sourceName(),
                workshopId,
                jarPath,
                sha256,
                size,
                modified,
                previousHash(modId, sha256),
                metadata == null ? List.of() : metadata.mixins()
        );
    }

    private String previousHash(String modId, String currentHash) {
        return approvals.values().stream()
                .filter(StoredDecision::allow)
                .filter(decision -> decision.modId().equals(modId))
                .filter(decision -> !decision.sha256().equals(currentHash))
                .max(Comparator.comparingLong(StoredDecision::time))
                .map(StoredDecision::sha256)
                .orElse("");
    }

    private void applyResponse(List<ZomboidApprovalEntry> batch, ZomboidApprovalResponse response) {
        if (response == null || response.decisions().isEmpty()) {
            LOGGER.info("No plugin approval decisions were returned; pending archives will be skipped");
            return;
        }

        Map<String, ZomboidApprovalEntry> batchByHash = new HashMap<>();
        for (ZomboidApprovalEntry entry : batch) {
            batchByHash.put(entry.sha256(), entry);
        }

        Set<String> seen = new HashSet<>();
        for (ZomboidApprovalDecision decision : response.decisions()) {
            if (decision == null || decision.sha256().isBlank() || !batchByHash.containsKey(decision.sha256())
                    || !seen.add(decision.sha256())) {
                LOGGER.error("Rejecting invalid plugin approval response for batch of {} archive(s)", batch.size());
                return;
            }
        }

        for (ZomboidApprovalDecision decision : response.decisions()) {
            sessionDecisions.put(decision.sha256(), decision.allow());
            if (!response.persist()) {
                continue;
            }
            if (!storageAvailable) {
                LOGGER.warn("Approval store is unavailable; decision for {} is session-only", decision.sha256());
                continue;
            }

            ZomboidApprovalEntry entry = batchByHash.get(decision.sha256());
            approvals.put(decision.sha256(), new StoredDecision(
                    decision.sha256(),
                    decision.allow(),
                    System.currentTimeMillis(),
                    entry.modId(),
                    entry.sourceType(),
                    entry.sourceName(),
                    entry.workshopId()
            ));
            storeDirty = true;
        }
    }

    private void prepareStorage() {
        try {
            Files.createDirectories(approvalsFile.getParent());
        } catch (IOException e) {
            storageAvailable = false;
            LOGGER.warn("Failed to create plugin approval directory [{}]", approvalsFile.getParent(), e);
        }
    }

    private void loadApprovals() {
        if (!Files.isRegularFile(approvalsFile)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(approvalsFile, StandardCharsets.UTF_8)) {
                parseLine(line.split("\t", -1));
            }
            LOGGER.info("Loaded {} plugin approval decision(s) from [{}]", approvals.size(), approvalsFile);
        } catch (IOException e) {
            LOGGER.warn("Failed to read plugin approvals [{}]; starting with an empty store", approvalsFile, e);
            approvals.clear();
        }
    }

    private void parseLine(String[] parts) {
        if (parts.length < 8 || !"APPROVAL".equals(parts[0])) {
            return;
        }

        String sha256 = parts[1];
        Boolean allow = parseBoolean(parts[2]);
        long time = parseLong(parts[3]);
        String modId = decode(parts[4]);
        String sourceType = decode(parts[5]);
        String sourceName = decode(parts[6]);
        String workshopId = decode(parts[7]);
        if (sha256.isBlank() || allow == null) {
            return;
        }

        approvals.put(sha256, new StoredDecision(
                sha256,
                allow,
                time,
                modId,
                sourceType,
                sourceName,
                workshopId
        ));
    }

    private boolean saveApprovals() {
        if (!storageAvailable) {
            return false;
        }

        StringBuilder content = new StringBuilder(VERSION_LINE).append('\n');
        approvals.values().stream()
                .sorted(Comparator.comparing(StoredDecision::sha256))
                .forEach(decision -> content.append("APPROVAL")
                        .append('\t').append(decision.sha256())
                        .append('\t').append(decision.allow())
                        .append('\t').append(decision.time())
                        .append('\t').append(encode(decision.modId()))
                        .append('\t').append(encode(decision.sourceType()))
                        .append('\t').append(encode(decision.sourceName()))
                        .append('\t').append(encode(decision.workshopId()))
                        .append('\n'));

        try {
            writeAtomically(approvalsFile, content.toString());
            LOGGER.info("Saved {} plugin approval decision(s) to [{}]", approvals.size(), approvalsFile);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Failed to save plugin approval store [{}]", approvalsFile, e);
            return false;
        }
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }

        byte[] buffer = new byte[8192];
        try (InputStream input = Files.newInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }

        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest.digest()) {
            hex.append(String.format("%02x", value & 0xFF));
        }
        return hex.toString();
    }

    private static Policy resolvePolicy(Environment environment, String policyValue) {
        Policy fallback = environment == Environment.SERVER ? Policy.DENY_NEW : Policy.PROMPT;
        if (policyValue == null || policyValue.isBlank()) {
            return fallback;
        }

        Policy resolved = switch (policyValue.trim().toLowerCase(Locale.ROOT)) {
            case POLICY_PROMPT -> Policy.PROMPT;
            case POLICY_DENY_NEW -> Policy.DENY_NEW;
            case POLICY_ALLOW_ALL -> Policy.ALLOW_ALL;
            default -> {
                LOGGER.warn("Unknown plugin policy [{}]; using [{}]", policyValue, fallback.displayName);
                yield fallback;
            }
        };

        if (resolved == Policy.PROMPT && environment == Environment.SERVER) {
            LOGGER.warn("Plugin policy [prompt] has no client UI on a dedicated server; using [deny-new]");
            return Policy.DENY_NEW;
        }
        if (resolved == Policy.ALLOW_ALL) {
            LOGGER.warn("Plugin policy [allow-all] is enabled; Workshop Java plugins will load without approval");
        }
        return resolved;
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String encode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String pluginId(Metadata metadata) {
        return metadata == null ? "unknown" : metadata.id();
    }

    private static void writeAtomically(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private enum Policy {
        PROMPT("prompt"),
        DENY_NEW("deny-new"),
        ALLOW_ALL("allow-all");

        private final String displayName;

        private Policy(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * Persistent allow or deny decision bound to one archive hash.
     */
    private record StoredDecision(
            String sha256,
            boolean allow,
            long time,
            String modId,
            String sourceType,
            String sourceName,
            String workshopId
    ) {
    }
}
