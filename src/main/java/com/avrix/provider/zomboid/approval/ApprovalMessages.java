package com.avrix.provider.zomboid.approval;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Loads and resolves approval window messages for one language.
 * <p>
 * The root {@code translations/messages.properties} bundle is English and acts as a fallback for every language. A
 * translation may omit keys that are not translated yet; those keys are resolved from English.
 */
public final class ApprovalMessages {

    private static final String BUNDLE_NAME = "com.avrix.provider.zomboid.approval.translations.messages";
    private static final ResourceBundle.Control CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private ApprovalLanguage language;
    private ResourceBundle bundle;

    /**
     * Creates messages for the operating system language.
     */
    public ApprovalMessages() {
        this(ApprovalLanguages.detect());
    }

    /**
     * Creates messages for an explicit language.
     *
     * @param language language to use
     */
    public ApprovalMessages(ApprovalLanguage language) {
        setLanguage(language);
    }

    /**
     * Returns the active language.
     *
     * @return active language
     */
    public ApprovalLanguage language() {
        return language;
    }

    /**
     * Switches the active language and reloads the message bundle.
     *
     * @param language language to use
     */
    public void setLanguage(ApprovalLanguage language) {
        this.language = language == null ? ApprovalLanguages.defaultLanguage() : language;
        this.bundle = load(this.language);
    }

    /**
     * Returns a message without placeholders.
     *
     * @param message message key
     * @return message text, or the property key itself when the message is missing
     */
    public String get(ApprovalMessage message) {
        try {
            return bundle.getString(message.key());
        } catch (MissingResourceException e) {
            return message.key();
        }
    }

    /**
     * Returns a message with {@code {0}}-style placeholders replaced.
     *
     * @param message message key
     * @param args    placeholder values
     * @return formatted message text
     */
    public String format(ApprovalMessage message, Object... args) {
        String pattern = get(message);
        StringBuilder result = new StringBuilder(pattern.length() + 16);
        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (current != '{') {
                result.append(current);
                continue;
            }
            int end = pattern.indexOf('}', index + 1);
            if (end < 0) {
                result.append(current);
                continue;
            }
            String token = pattern.substring(index + 1, end);
            if (!token.isEmpty() && token.chars().allMatch(Character::isDigit)) {
                int argument = Integer.parseInt(token);
                if (argument < args.length) {
                    result.append(args[argument]);
                    index = end;
                    continue;
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private static ResourceBundle load(ApprovalLanguage language) {
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, language.locale(), CONTROL);
        } catch (MissingResourceException e) {
            return ResourceBundle.getBundle(
                    BUNDLE_NAME,
                    ApprovalLanguages.defaultLanguage().locale(),
                    CONTROL
            );
        }
    }
}
