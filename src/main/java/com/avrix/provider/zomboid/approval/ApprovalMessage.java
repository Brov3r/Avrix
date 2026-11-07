package com.avrix.provider.zomboid.approval;

/**
 * Message keys used by the approval window.
 * <p>
 * English texts live in {@code translations/messages.properties}. Every other language bundle may translate any subset
 * of these keys; missing keys are resolved from English.
 */
public enum ApprovalMessage {

    /**
     * Window title.
     */
    WINDOW_TITLE("window.title"),

    /**
     * Main heading above the table.
     */
    HEADER_TITLE("header.title"),

    /**
     * Subtitle that explains the approval flow.
     */
    HEADER_SUBTITLE("header.subtitle"),

    /**
     * Note that explains what SHA-256 identifies.
     */
    HEADER_IDENTITY("header.identity"),

    /**
     * Persistent decision checkbox label.
     */
    REMEMBER_LABEL("remember.label"),

    /**
     * Persistent decision checkbox tooltip.
     */
    REMEMBER_TOOLTIP("remember.tooltip"),

    /**
     * Hint shown next to the persistent decision checkbox.
     */
    REMEMBER_HINT("remember.hint"),

    /**
     * Allow-all button label.
     */
    BUTTON_ALLOW_ALL("button.allowAll"),

    /**
     * Deny-all button label.
     */
    BUTTON_DENY_ALL("button.denyAll"),

    /**
     * Apply button label.
     */
    BUTTON_APPLY("button.apply"),

    /**
     * Cancel button label.
     */
    BUTTON_CANCEL("button.cancel"),

    /**
     * Language selector tooltip.
     */
    LANGUAGE_TOOLTIP("language.tooltip"),

    /**
     * State column header.
     */
    COLUMN_STATE("column.state"),

    /**
     * Mod name column header.
     */
    COLUMN_MOD("column.mod"),

    /**
     * Author column header.
     */
    COLUMN_AUTHOR("column.author"),

    /**
     * Size column header.
     */
    COLUMN_SIZE("column.size"),

    /**
     * Status label for a new archive.
     */
    STATE_NEW("state.new"),

    /**
     * Status label for a changed archive.
     */
    STATE_CHANGED("state.changed"),

    /**
     * Placeholder for an unknown date.
     */
    DATE_UNKNOWN("date.unknown"),

    /**
     * Workshop source prefix.
     */
    SOURCE_WORKSHOP("source.workshop"),

    /**
     * Local source label.
     */
    SOURCE_LOCAL("source.local"),

    /**
     * Tooltip that hides archive and mixin details.
     */
    TOOLTIP_DETAILS_HIDE("tooltip.details.hide"),

    /**
     * Tooltip that shows archive and mixin details.
     */
    TOOLTIP_DETAILS_SHOW("tooltip.details.show"),

    /**
     * Tooltip for a new archive state.
     */
    TOOLTIP_STATE_NEW("tooltip.state.new"),

    /**
     * Tooltip for a previously approved hash.
     */
    TOOLTIP_STATE_PREVIOUS("tooltip.state.previous"),

    /**
     * Tooltip for the select-all checkbox.
     */
    TOOLTIP_SELECT_ALL("tooltip.selectAll"),

    /**
     * Plugin identifier label.
     */
    LABEL_PLUGIN_ID("label.pluginId"),

    /**
     * Mod identifier label.
     */
    LABEL_MOD_ID("label.modId"),

    /**
     * Author label.
     */
    LABEL_AUTHOR("label.author"),

    /**
     * SHA-256 label.
     */
    LABEL_SHA256("label.sha256"),

    /**
     * Plugin metadata section title.
     */
    DETAIL_SECTION_METADATA("detail.section.metadata"),

    /**
     * Mod identifier detail line.
     */
    DETAIL_MOD_ID("detail.modId"),

    /**
     * Plugin identifier detail line.
     */
    DETAIL_PLUGIN_ID("detail.pluginId"),

    /**
     * Author detail line.
     */
    DETAIL_AUTHOR("detail.author"),

    /**
     * Source detail line.
     */
    DETAIL_SOURCE("detail.source"),

    /**
     * Size detail line.
     */
    DETAIL_SIZE("detail.size"),

    /**
     * Last modified detail line.
     */
    DETAIL_MODIFIED("detail.modified"),

    /**
     * Archive section title.
     */
    DETAIL_SECTION_ARCHIVE("detail.section.archive"),

    /**
     * Integrity section title.
     */
    DETAIL_SECTION_INTEGRITY("detail.section.integrity"),

    /**
     * Previously approved section title.
     */
    DETAIL_SECTION_PREVIOUS("detail.section.previous"),

    /**
     * Declared mixins section title.
     */
    DETAIL_SECTION_MIXINS("detail.section.mixins"),

    /**
     * Empty mixin list note.
     */
    DETAIL_MIXINS_EMPTY("detail.mixins.empty"),

    /**
     * Affected classes section title.
     */
    DETAIL_SECTION_TARGETS("detail.section.targets"),

    /**
     * Empty target list note.
     */
    DETAIL_TARGETS_EMPTY("detail.targets.empty"),

    /**
     * Inspection notes section title.
     */
    DETAIL_SECTION_NOTES("detail.section.notes");

    private final String key;

    /**
     * Creates a message key.
     *
     * @param key property key used by the message bundles
     */
    private ApprovalMessage(String key) {
        this.key = key;
    }

    /**
     * Returns the property key used by the message bundles.
     *
     * @return property key
     */
    public String key() {
        return key;
    }
}
