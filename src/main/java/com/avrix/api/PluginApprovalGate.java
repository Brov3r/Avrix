package com.avrix.api;

import com.avrix.core.Metadata;

/**
 * Decides whether a discovered plugin archive may be loaded.
 * <p>
 * The loader core uses a trusted-only gate by default: archives from the manually managed {@code plugins/}
 * directory load, while archives discovered from game mods are denied unless the provider supplies a stricter gate.
 */
public interface PluginApprovalGate {

    /**
     * Evaluates one archive against the active policy.
     *
     * @param archive  discovered plugin archive
     * @param metadata parsed plugin metadata
     * @return approval status
     */
    Status evaluate(PluginArchive archive, Metadata metadata);

    /**
     * Evaluates one archive against the active policy using a pre-computed content hash.
     * <p>
     * The default implementation ignores {@code sha256} and delegates to {@link #evaluate(PluginArchive, Metadata)}
     * so existing gates keep working. Gates that persist hash-bound decisions should override this method and use the
     * supplied value instead of hashing the archive again.
     *
     * @param archive  discovered plugin archive
     * @param metadata parsed plugin metadata
     * @param sha256   lowercase SHA-256 of the archive, or {@code null} when the archive is trusted
     * @return approval status
     */
    default Status evaluate(PluginArchive archive, Metadata metadata, String sha256) {
        return evaluate(archive, metadata);
    }

    /**
     * Resolves and persists any pending approval entries.
     * <p>
     * Implementations may block while a user interface collects decisions. After this method returns,
     * {@link #evaluate(PluginArchive, Metadata, String)} must be idempotent for the same archive and hash so callers
     * can retry pending archives in the same process without prompting twice.
     */
    default void flushPending() {
    }

    /**
     * Creates a gate that approves every archive.
     *
     * @return allow-all approval gate
     */
    static PluginApprovalGate allowAll() {
        return (archive, metadata) -> Status.APPROVED;
    }

    /**
     * Creates a gate that denies every archive.
     *
     * @return deny-all approval gate
     */
    static PluginApprovalGate denyAll() {
        return (archive, metadata) -> Status.DENIED;
    }

    /**
     * Creates a gate that approves trusted archives and denies everything discovered from game mods.
     *
     * @return trusted-only approval gate
     */
    static PluginApprovalGate trustedOnly() {
        return (archive, metadata) -> archive.trusted() ? Status.APPROVED : Status.DENIED;
    }

    /**
     * Plugin approval status.
     */
    enum Status {
        /**
         * Archive may be loaded.
         */
        APPROVED,
        /**
         * Archive is waiting for an external decision.
         */
        PENDING,
        /**
         * Archive must not be loaded.
         */
        DENIED
    }
}
