package com.avrix.provider.zomboid.approval;

import com.avrix.provider.zomboid.ZomboidConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the approval wait timeout shared by the Swing and console frontends.
 */
public final class ApprovalTimeouts {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalTimeouts.class);
    private static final int DEFAULT_SECONDS = 600;
    private static final int MIN_SECONDS = 30;

    /**
     * Maximum approval timeout in seconds.
     */
    public static final int MAX_SECONDS = 3600;

    private ApprovalTimeouts() {
        throw new UnsupportedOperationException("ApprovalTimeouts utility class cannot be instantiated");
    }

    /**
     * Reads the configured approval timeout.
     *
     * @return timeout in seconds, clamped to the supported range
     */
    public static int readSeconds() {
        String value = System.getProperty(ZomboidConstants.APPROVAL_TIMEOUT_PROPERTY);
        if (value == null || value.isBlank()) {
            return DEFAULT_SECONDS;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, parsed));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid {} value [{}]; using {} seconds",
                    ZomboidConstants.APPROVAL_TIMEOUT_PROPERTY, value, DEFAULT_SECONDS);
            return DEFAULT_SECONDS;
        }
    }
}
