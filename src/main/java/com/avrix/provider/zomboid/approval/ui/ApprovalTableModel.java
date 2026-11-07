package com.avrix.provider.zomboid.approval.ui;

import com.avrix.provider.zomboid.approval.ApprovalMessage;
import com.avrix.provider.zomboid.approval.ApprovalMessages;
import com.avrix.provider.zomboid.approval.MixinTargetInspector;
import com.avrix.provider.zomboid.approval.ZomboidApprovalDecision;
import com.avrix.provider.zomboid.approval.ZomboidApprovalEntry;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table model backed by the pending approval entries.
 */
public final class ApprovalTableModel extends AbstractTableModel {

    private static final ApprovalMessage[] COLUMN_KEYS = {
            ApprovalMessage.COLUMN_STATE,
            ApprovalMessage.COLUMN_MOD,
            ApprovalMessage.COLUMN_AUTHOR,
            ApprovalMessage.COLUMN_SIZE
    };

    /**
     * Pending approval entries.
     */
    private final List<ZomboidApprovalEntry> entries;

    /**
     * Localized messages.
     */
    private final ApprovalMessages messages;

    /**
     * Per-entry allow flags.
     */
    private final boolean[] allow;

    /**
     * Per-entry expanded flags.
     */
    private final boolean[] expanded;

    /**
     * Cached mixin inspection results keyed by entry index.
     */
    private final Map<Integer, MixinTargetInspector.Result> inspections = new HashMap<>();

    /**
     * Current display rows.
     */
    private List<DisplayRow> rows;

    /**
     * Creates a table model for the pending entries.
     *
     * @param entries  pending approval entries
     * @param messages localized messages
     */
    public ApprovalTableModel(List<ZomboidApprovalEntry> entries, ApprovalMessages messages) {
        this.entries = List.copyOf(entries);
        this.messages = messages;
        this.allow = new boolean[entries.size()];
        this.expanded = new boolean[entries.size()];
        this.rows = buildRows();
    }

    /**
     * Returns the number of display rows, including expanded detail rows.
     *
     * @return display row count
     */
    @Override
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Returns the number of table columns.
     *
     * @return column count
     */
    @Override
    public int getColumnCount() {
        return COLUMN_KEYS.length;
    }

    /**
     * Returns the localized column name.
     *
     * @param column column index
     * @return localized column name
     */
    @Override
    public String getColumnName(int column) {
        return messages.get(COLUMN_KEYS[column]);
    }

    /**
     * Returns the value class for a column.
     *
     * @param columnIndex column index
     * @return column value class
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == ApprovalColumns.MOD ? Object.class : String.class;
    }

    /**
     * Returns the display value for one cell.
     *
     * @param rowIndex    display row index
     * @param columnIndex column index
     * @return cell value
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DisplayRow row = rows.get(rowIndex);
        if (row.detail()) {
            return columnIndex == ApprovalColumns.MOD ? row : "";
        }

        int entryIndex = row.entryIndex();
        ZomboidApprovalEntry entry = entries.get(entryIndex);
        return switch (columnIndex) {
            case ApprovalColumns.STATE -> entry.previousSha256().isBlank() ? "NEW" : "CHANGED";
            case ApprovalColumns.MOD -> entry.displayName().isBlank() ? entry.pluginId() : entry.displayName();
            case ApprovalColumns.AUTHOR -> ApprovalFormatting.blankToDash(entry.modAuthor());
            case ApprovalColumns.SIZE -> ApprovalFormatting.formatSize(entry.size());
            default -> "";
        };
    }

    /**
     * Applies one allow value to every entry.
     *
     * @param value allow value
     */
    public void setAll(boolean value) {
        for (int index = 0; index < allow.length; index++) {
            allow[index] = value;
        }
        if (getRowCount() > 0) {
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }

    /**
     * Returns the number of approval entries.
     *
     * @return entry count
     */
    public int entryCount() {
        return entries.size();
    }

    /**
     * Returns the number of allowed entries.
     *
     * @return allowed entry count
     */
    public int allowedCount() {
        int count = 0;
        for (boolean value : allow) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns whether an entry is allowed.
     *
     * @param entryIndex entry index
     * @return {@code true} when allowed
     */
    public boolean isAllowed(int entryIndex) {
        return allow[entryIndex];
    }

    /**
     * Toggles the allow state of one entry.
     *
     * @param entryIndex entry index
     */
    public void toggleAllowed(int entryIndex) {
        allow[entryIndex] = !allow[entryIndex];
    }

    /**
     * Toggles the expanded state of one display row.
     *
     * @param rowIndex display row index
     */
    public void toggleExpanded(int rowIndex) {
        DisplayRow row = rows.get(rowIndex);
        if (row.detail()) {
            return;
        }
        expanded[row.entryIndex()] = !expanded[row.entryIndex()];
        rows = buildRows();
        fireTableDataChanged();
    }

    /**
     * Returns whether an entry is expanded.
     *
     * @param entryIndex entry index
     * @return {@code true} when expanded
     */
    public boolean isExpanded(int entryIndex) {
        return expanded[entryIndex];
    }

    /**
     * Returns whether a display row is a detail row.
     *
     * @param rowIndex display row index
     * @return {@code true} for a detail row
     */
    public boolean isDetailRow(int rowIndex) {
        return rows.get(rowIndex).detail();
    }

    /**
     * Returns the entry index for a display row.
     *
     * @param rowIndex display row index
     * @return entry index
     */
    public int entryIndex(int rowIndex) {
        return rows.get(rowIndex).entryIndex();
    }

    /**
     * Returns the inspected mixin data for an entry.
     *
     * @param entryIndex entry index
     * @return inspection result
     */
    public MixinTargetInspector.Result inspection(int entryIndex) {
        return inspections.computeIfAbsent(entryIndex, index -> MixinTargetInspector.inspect(
                entries.get(index).jarPath(),
                entries.get(index).mixinClasses()
        ));
    }

    /**
     * Returns the decisions collected from the model.
     *
     * @return approval decisions
     */
    public List<ZomboidApprovalDecision> decisions() {
        List<ZomboidApprovalDecision> decisions = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            decisions.add(new ZomboidApprovalDecision(entries.get(index).sha256(), allow[index]));
        }
        return decisions;
    }

    /**
     * Returns the entry at the given index.
     *
     * @param entryIndex entry index
     * @return approval entry
     */
    public ZomboidApprovalEntry entryAt(int entryIndex) {
        return entries.get(entryIndex);
    }

    private List<DisplayRow> buildRows() {
        List<DisplayRow> result = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            result.add(new DisplayRow(index, false));
            if (expanded[index]) {
                result.add(new DisplayRow(index, true));
            }
        }
        return result;
    }

    private record DisplayRow(int entryIndex, boolean detail) {
    }
}
