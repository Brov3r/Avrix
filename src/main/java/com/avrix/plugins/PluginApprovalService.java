package com.avrix.plugins;

import com.avrix.core.Environment;
import com.avrix.core.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Applies the {@code avrix.plugin.policy} policy to plugin archives discovered from active Project Zomboid mods.
 * <p>
 * Approval files are stored in the game directory under {@code avrix/}. A Lua-readable mirror of the pending list
 * and a decision hand-off file are kept in the Project Zomboid cache directory under {@code Lua/avrix/} because the
 * Project Zomboid Lua sandbox cannot access arbitrary paths outside that directory. The cache directory follows the
 * game {@code -cachedir=} override when present.
 */
public final class PluginApprovalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginApprovalService.class);

    private static final String VERSION_LINE = "VERSION\t1";
    private static final String APPROVALS_FILE_NAME = "plugin-approvals.txt";
    private static final String PENDING_FILE_NAME = "plugin-pending.txt";
    private static final String DECISIONS_FILE_NAME = "plugin-decisions.txt";

    private static final String POLICY_PROMPT = "prompt";
    private static final String POLICY_DENY_NEW = "deny-new";
    private static final String POLICY_ALLOW_ALL = "allow-all";

    private final Path gameDataDirectory;
    private final Path luaDirectory;
    private final Policy policy;
    private final Map<String, Map<String, Boolean>> approvals = new TreeMap<>();
    private final List<PendingEntry> pending = new ArrayList<>();
    private final Set<String> pendingKeys = new HashSet<>();

    private boolean storageAvailable = true;
    private boolean luaStorageAvailable = true;

    /**
     * Creates an approval service and loads persisted decisions.
     *
     * @param gameRoot    Project Zomboid installation root
     * @param cacheRoot   Project Zomboid user cache directory that contains the {@code Lua} folder
     * @param environment active runtime environment
     * @param policyValue configured policy value, or {@code null} for the environment default
     * @throws NullPointerException if {@code gameRoot} or {@code cacheRoot} is null
     */
    public PluginApprovalService(Path gameRoot, Path cacheRoot, Environment environment, String policyValue) {
        Path normalizedGameRoot = java.util.Objects.requireNonNull(gameRoot, "Game root cannot be null")
                .toAbsolutePath()
                .normalize();
        Path normalizedCacheRoot = java.util.Objects.requireNonNull(cacheRoot, "Cache root cannot be null")
                .toAbsolutePath()
                .normalize();

        this.gameDataDirectory = normalizedGameRoot.resolve("avrix");
        this.luaDirectory = normalizedCacheRoot.resolve("Lua").resolve("avrix");
        this.policy = resolvePolicy(environment == null ? Environment.CLIENT : environment, policyValue);

        prepareStorage();
        loadApprovals();
        processDecisions();

        LOGGER.info("Workshop plugin approval policy: [{}]", policy.displayName);
    }

    /**
     * Evaluates one archive against the configured policy.
     *
     * @param archive  discovered plugin archive
     * @param metadata parsed plugin metadata
     * @return approval status
     */
    public Status evaluate(PluginArchive archive, Metadata metadata) {
        if (archive.trusted()) {
            return Status.APPROVED;
        }

        if (policy == Policy.ALLOW_ALL) {
            return Status.APPROVED;
        }

        if (!storageAvailable) {
            LOGGER.warn(
                    "Plugin approval storage is unavailable; skipping unapproved plugin [{}] from mod [{}]",
                    pluginId(metadata),
                    archive.modId()
            );
            return Status.DENIED;
        }

        String modId = archive.modId();
        if (modId == null || modId.isBlank()) {
            LOGGER.warn("Plugin archive [{}] has no source mod ID; denying", archive.file());
            return Status.DENIED;
        }

        String sha256;
        try {
            sha256 = sha256(archive.file().toPath());
        } catch (IOException e) {
            LOGGER.warn("Failed to hash plugin archive [{}]; denying", archive.file(), e);
            return Status.DENIED;
        }

        Boolean allowed = approvals.getOrDefault(modId, Map.of()).get(sha256);
        if (allowed != null) {
            if (allowed) {
                LOGGER.info(
                        "Approved plugin [{}] from mod [{}] sha256={}",
                        pluginId(metadata),
                        modId,
                        sha256
                );
                return Status.APPROVED;
            }

            LOGGER.info(
                    "Denied plugin [{}] from mod [{}] by stored decision sha256={}",
                    pluginId(metadata),
                    modId,
                    sha256
            );
            return Status.DENIED;
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

        String key = modId + "\t" + sha256;
        if (pendingKeys.add(key)) {
            boolean changed = approvals.containsKey(modId) && !approvals.get(modId).isEmpty();
            pending.add(new PendingEntry(
                    modId,
                    pluginId(metadata),
                    pluginName(metadata),
                    archive.file().getPath(),
                    sha256,
                    changed ? "CHANGED" : "NEW"
            ));
            LOGGER.info(
                    "Plugin [{}] from mod [{}] is pending approval sha256={}",
                    pluginId(metadata),
                    modId,
                    sha256
            );
        }

        return Status.PENDING;
    }

    /**
     * Writes the pending list for the Lua approval UI.
     */
    public void flushPending() {
        if (!storageAvailable) {
            return;
        }

        StringBuilder content = new StringBuilder(VERSION_LINE).append('\n');
        for (PendingEntry entry : pending) {
            content.append("ENTRY")
                    .append('\t').append(sanitize(entry.modId()))
                    .append('\t').append(sanitize(entry.pluginId()))
                    .append('\t').append(sanitize(entry.state()))
                    .append('\t').append(sanitize(entry.sha256()))
                    .append('\t').append(sanitize(entry.jarPath()))
                    .append('\t').append(sanitize(entry.pluginName()))
                    .append('\n');
        }

        try {
            writeAtomically(gameDataDirectory.resolve(PENDING_FILE_NAME), content.toString());
        } catch (UncheckedIOException e) {
            storageAvailable = false;
            LOGGER.warn("Failed to write plugin pending list; unapproved plugins will be skipped", e);
            return;
        }

        if (luaStorageAvailable) {
            try {
                writeAtomically(luaDirectory.resolve(PENDING_FILE_NAME), content.toString());
            } catch (UncheckedIOException e) {
                luaStorageAvailable = false;
                LOGGER.warn("Failed to write Lua plugin pending list [{}]", luaDirectory, e);
            }
        }
    }

    private void prepareStorage() {
        try {
            Files.createDirectories(gameDataDirectory);
        } catch (IOException e) {
            storageAvailable = false;
            LOGGER.warn("Failed to create plugin approval directory [{}]", gameDataDirectory, e);
        }

        try {
            Files.createDirectories(luaDirectory);
        } catch (IOException e) {
            luaStorageAvailable = false;
            LOGGER.warn("Failed to create Lua approval directory [{}]", luaDirectory, e);
        }
    }

    private void loadApprovals() {
        Path file = gameDataDirectory.resolve(APPROVALS_FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", -1);
                if (parts.length < 4 || !"APPROVAL".equals(parts[0])) {
                    continue;
                }

                String modId = parts[1];
                String sha256 = parts[2];
                Boolean allow = parseBoolean(parts[3]);
                if (modId.isBlank() || sha256.isBlank() || allow == null) {
                    continue;
                }

                approvals.computeIfAbsent(modId, ignored -> new TreeMap<>()).put(sha256, allow);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read plugin approvals [{}]; starting with an empty store", file, e);
            approvals.clear();
        }
    }

    private void saveApprovals() {
        if (!storageAvailable) {
            return;
        }

        StringBuilder content = new StringBuilder(VERSION_LINE).append('\n');
        for (Map.Entry<String, Map<String, Boolean>> modEntry : approvals.entrySet()) {
            for (Map.Entry<String, Boolean> hashEntry : modEntry.getValue().entrySet()) {
                content.append("APPROVAL")
                        .append('\t').append(sanitize(modEntry.getKey()))
                        .append('\t').append(sanitize(hashEntry.getKey()))
                        .append('\t').append(hashEntry.getValue())
                        .append('\n');
            }
        }

        try {
            writeAtomically(gameDataDirectory.resolve(APPROVALS_FILE_NAME), content.toString());
        } catch (UncheckedIOException e) {
            LOGGER.warn("Failed to write plugin approval store [{}]", gameDataDirectory, e);
        }
    }

    private void processDecisions() {
        boolean changed = processDecisionsFile(gameDataDirectory.resolve(DECISIONS_FILE_NAME));
        changed |= processDecisionsFile(luaDirectory.resolve(DECISIONS_FILE_NAME));
        if (changed) {
            saveApprovals();
        }
    }

    private boolean processDecisionsFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return false;
        }

        boolean changed = false;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", -1);
                if (parts.length < 4 || !"DECISION".equals(parts[0])) {
                    continue;
                }

                String modId = parts[1];
                String sha256 = parts[2];
                Boolean allow = parseBoolean(parts[3]);
                if (modId.isBlank() || sha256.isBlank() || allow == null) {
                    continue;
                }

                approvals.computeIfAbsent(modId, ignored -> new TreeMap<>()).put(sha256, allow);
                changed = true;
                LOGGER.info(
                        "Applied plugin approval decision: mod [{}] sha256={} allow={}",
                        modId,
                        sha256,
                        allow
                );
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read plugin decisions [{}]", file, e);
            return false;
        } finally {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                LOGGER.debug("Failed to remove processed decisions file [{}]", file, e);
            }
        }

        return changed;
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

    private static String pluginId(Metadata metadata) {
        return metadata == null ? "unknown" : metadata.id();
    }

    private static String pluginName(Metadata metadata) {
        return metadata == null ? "unknown" : metadata.name();
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static void writeAtomically(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
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
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write plugin approval file: " + file, e);
        }
    }

    /**
     * Plugin approval status.
     */
    public enum Status {
        /**
         * Archive may be loaded.
         */
        APPROVED,
        /**
         * Archive is waiting for a Lua UI decision.
         */
        PENDING,
        /**
         * Archive must not be loaded.
         */
        DENIED
    }

    private enum Policy {
        PROMPT("prompt"),
        DENY_NEW("deny-new"),
        ALLOW_ALL("allow-all");

        private final String displayName;

        Policy(String displayName) {
            this.displayName = displayName;
        }
    }

    private record PendingEntry(
            String modId,
            String pluginId,
            String pluginName,
            String jarPath,
            String sha256,
            String state
    ) {
    }
}
