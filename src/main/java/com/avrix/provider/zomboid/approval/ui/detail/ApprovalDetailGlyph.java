package com.avrix.provider.zomboid.approval.ui.detail;

/**
 * Glyphs used by detail panel section headers.
 */
public enum ApprovalDetailGlyph {

    /**
     * Plugin metadata glyph.
     */
    INFO("info"),

    /**
     * Archive glyph.
     */
    ARCHIVE("document"),

    /**
     * Hash glyph.
     */
    HASH("tag"),

    /**
     * Mixin glyph.
     */
    MIXIN("grid-add"),

    /**
     * Target glyph.
     */
    TARGET("target"),

    /**
     * History glyph.
     */
    HISTORY("time"),

    /**
     * Warning glyph.
     */
    WARNING("exclamation");

    private final String icon;

    private ApprovalDetailGlyph(String icon) {
        this.icon = icon;
    }

    /**
     * Returns the vector icon name.
     *
     * @return icon name
     */
    public String icon() {
        return icon;
    }
}
