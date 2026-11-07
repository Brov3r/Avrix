package com.avrix.provider.zomboid.approval.ui;

/**
 * Column and row layout constants used by the approval table.
 */
public final class ApprovalColumns {

    /**
     * State column index.
     */
    public static final int STATE = 0;

    /**
     * Mod name column index.
     */
    public static final int MOD = 1;

    /**
     * Author column index.
     */
    public static final int AUTHOR = 2;

    /**
     * Size column index.
     */
    public static final int SIZE = 3;

    /**
     * Base table row height in pixels.
     */
    public static final int BASE_ROW_HEIGHT = 34;

    private ApprovalColumns() {
        throw new UnsupportedOperationException("ApprovalColumns utility class cannot be instantiated");
    }
}
