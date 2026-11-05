package com.avrix.provider.approval;

import java.util.List;

/**
 * Collects a batch of user decisions for pending Project Zomboid plugin archives.
 */
@FunctionalInterface
public interface ZomboidApprovalFrontend {

    /**
     * Asks the user for decisions on all pending archives.
     *
     * @param pending archives awaiting approval
     * @return user decisions, or an empty response when the user cancelled or the frontend is unavailable
     * @throws Exception if the frontend cannot complete the request
     */
    ZomboidApprovalResponse request(List<ZomboidApprovalEntry> pending) throws Exception;

    /**
     * Creates a frontend that approves and denies nothing.
     *
     * @return deny-only frontend
     */
    static ZomboidApprovalFrontend denyAll() {
        return pending -> new ZomboidApprovalResponse(false, List.of());
    }
}
