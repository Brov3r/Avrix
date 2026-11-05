package com.avrix.provider.approval;

import java.util.Objects;

/**
 * One allow or deny decision for a plugin archive hash.
 *
 * @param sha256 lowercase SHA-256 of the archive
 * @param allow  {@code true} to load the archive, {@code false} to block it
 */
public record ZomboidApprovalDecision(String sha256, boolean allow) {

    /**
     * Creates a validated decision.
     *
     * @throws NullPointerException if {@code sha256} is null
     */
    public ZomboidApprovalDecision {
        sha256 = Objects.requireNonNull(sha256, "SHA-256 cannot be null");
    }
}
