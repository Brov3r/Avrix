package com.avrix.provider.zomboid.approval;

import java.util.Locale;
import java.util.Objects;

/**
 * One language available in the approval window.
 *
 * @param code        locale tag used in the bundle file name, such as {@code en} or {@code pt_BR}
 * @param locale      locale used for message bundle lookup
 * @param displayName native language name shown in the selector
 */
public record ApprovalLanguage(String code, Locale locale, String displayName) {

    /**
     * Creates a validated language descriptor.
     */
    public ApprovalLanguage {
        code = Objects.requireNonNull(code, "Language code cannot be null");
        locale = Objects.requireNonNull(locale, "Language locale cannot be null");
        displayName = Objects.requireNonNullElse(displayName, code);
    }

    /**
     * Returns the native language name for default Swing renderers.
     *
     * @return native display name
     */
    @Override
    public String toString() {
        return displayName;
    }
}
