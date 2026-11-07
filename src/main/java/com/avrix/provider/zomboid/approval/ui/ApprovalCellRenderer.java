package com.avrix.provider.zomboid.approval.ui;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Dark cell renderer with alternating row colors and column-specific emphasis.
 */
public final class ApprovalCellRenderer extends DefaultTableCellRenderer {

    /**
     * Creates a plain-text renderer that never interprets metadata as HTML.
     */
    public ApprovalCellRenderer() {
        putClientProperty("html.disable", Boolean.TRUE);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    }

    /**
     * Configures the renderer for one table cell.
     *
     * @param table      table being rendered
     * @param value      cell value
     * @param isSelected whether the cell is selected
     * @param hasFocus   whether the cell has focus
     * @param row        row index
     * @param column     column index
     * @return configured renderer component
     */
    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        setFont(ZomboidApprovalTheme.BASE_FONT);

        if (isSelected) {
            setBackground(ZomboidApprovalTheme.SELECTION);
            setForeground(ZomboidApprovalTheme.TEXT);
        } else {
            setBackground(row % 2 == 0 ? ZomboidApprovalTheme.SURFACE : ZomboidApprovalTheme.SURFACE_ALT);
            setForeground(foregroundFor(column, value));
        }
        return this;
    }

    private Color foregroundFor(int column, Object value) {
        if (column == ApprovalColumns.STATE) {
            return "CHANGED".equals(value) ? ZomboidApprovalTheme.ACCENT : ZomboidApprovalTheme.MUTED;
        }
        if (column == ApprovalColumns.MOD) {
            return ZomboidApprovalTheme.TEXT;
        }
        return ZomboidApprovalTheme.MUTED;
    }
}
