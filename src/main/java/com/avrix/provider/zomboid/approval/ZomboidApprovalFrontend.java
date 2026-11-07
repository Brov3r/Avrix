package com.avrix.provider.zomboid.approval;

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
    public ZomboidApprovalResponse request(List<ZomboidApprovalEntry> pending) throws Exception;

    /**
     * Returns whether this frontend can ask the user interactively.
     *
     * @return {@code true} when interactive approval is supported
     */
    public default boolean supportsInteractivePrompt() {
        return false;
    }

    /**
     * Creates a frontend that approves and denies nothing.
     *
     * @return deny-only frontend
     */
    public static ZomboidApprovalFrontend denyAll() {
        return pending -> new ZomboidApprovalResponse(false, List.of());
    }
}
