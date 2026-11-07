package com.avrix.provider.zomboid.approval.ui;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Renderer for the merged mod name column: expand chevron plus plugin name, or a detail block for detail rows.
 */
public final class ApprovalModNameRenderer extends JComponent implements TableCellRenderer {
    /**
     * Rendered mod name.
     */
    private String text = "";

    /**
     * Whether the row is expanded.
     */
    private boolean expanded;

    /**
     * Whether the row is selected.
     */
    private boolean selected;

    /**
     * Current display row index.
     */
    private int row;

    /**
     * Creates a mod name renderer.
     */
    public ApprovalModNameRenderer() {
    }

    /**
     * Configures the mod name renderer for one cell.
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
        if (!(value instanceof String)) {
            ApprovalTableModel model = (ApprovalTableModel) table.getModel();
            return ((ApprovalTable) table).detailPanel(model.entryIndex(row));
        }

        this.text = value.toString();
        this.selected = isSelected;
        this.row = row;
        ApprovalTableModel model = (ApprovalTableModel) table.getModel();
        this.expanded = !model.isDetailRow(row) && model.isExpanded(model.entryIndex(row));
        return this;
    }

    /**
     * Paints the expand chevron and mod name.
     *
     * @param graphics target graphics
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(selected
                ? ZomboidApprovalTheme.SELECTION
                : (row % 2 == 0 ? ZomboidApprovalTheme.SURFACE : ZomboidApprovalTheme.SURFACE_ALT));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int size = 20;
        int x = 8;
        int y = (getHeight() - size) / 2;
        g2.setColor(ZomboidApprovalTheme.HEADER);
        g2.fillRoundRect(x, y, size, size, 7, 7);
        g2.setColor(ZomboidApprovalTheme.BORDER);
        g2.drawRoundRect(x, y, size, size, 7, 7);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        ZomboidApprovalIcons.paint(
                g2,
                expanded ? "arrow-down" : "arrow-right",
                centerX - 7,
                centerY - 7,
                14,
                ZomboidApprovalTheme.TEXT
        );

        g2.setColor(ZomboidApprovalTheme.TEXT);
        g2.setFont(ZomboidApprovalTheme.BOLD_FONT);
        FontMetrics metrics = g2.getFontMetrics();
        int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.drawString(text, x + size + 10, baseline);
        g2.dispose();
    }
}
