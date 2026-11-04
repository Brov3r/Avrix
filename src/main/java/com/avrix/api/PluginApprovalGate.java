package com.avrix.api;

import com.avrix.core.Metadata;

/**
 * Decides whether a discovered plugin archive may be loaded.
 * <p>
 * The loader core uses an allow-all gate by default. Game providers may supply a stricter implementation when
 * their distribution model requires user approval.
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
     * Persists any pending approval entries.
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
