package com.avrix.provider.zomboid.approval;

import java.util.List;

/**
 * Decisions produced by an approval frontend.
 *
 * @param persist   whether the decisions should be written to the persistent approval store
 * @param decisions allow or deny entries, one per pending archive hash
 */
public record ZomboidApprovalResponse(boolean persist, List<ZomboidApprovalDecision> decisions) {

    /**
     * Creates a response with an immutable decision list.
     */
    public ZomboidApprovalResponse {
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
    }
}
