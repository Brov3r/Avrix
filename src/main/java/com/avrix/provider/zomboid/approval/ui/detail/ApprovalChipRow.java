package com.avrix.provider.zomboid.approval.ui.detail;

import java.util.List;

/**
 * One row of chips in the detail panel.
 *
 * @param chips chips in this row
 */
public record ApprovalChipRow(List<ApprovalChip> chips) {

    /**
     * Creates a row with an immutable chip list.
     */
    public ApprovalChipRow {
        chips = List.copyOf(chips);
    }
}
