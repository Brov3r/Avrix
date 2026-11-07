package com.avrix.provider.zomboid.approval.ui;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Table UI that paints detail rows across the full table width while keeping BasicTableUI interaction.
 */
public final class ApprovalTableUI extends BasicTableUI {

    /**
     * Creates the table UI.
     */
    public ApprovalTableUI() {
    }

    /**
     * Paints the table, including full-width detail rows.
     *
     * @param graphics  target graphics
     * @param component table being painted
     */
    @Override
    public void paint(Graphics graphics, JComponent component) {
        Rectangle clip = graphics.getClipBounds();
        if (clip == null) {
            return;
        }

        rendererPane.removeAll();
        graphics.setColor(table.getBackground());
        graphics.fillRect(clip.x, clip.y, clip.width, clip.height);

        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        if (rowCount == 0 || columnCount == 0) {
            return;
        }

        int totalWidth = table.getColumnModel().getTotalColumnWidth();
        ApprovalTableModel model = table.getModel() instanceof ApprovalTableModel candidate ? candidate : null;

        for (int row = 0; row < rowCount; row++) {
            Rectangle rowRect = table.getCellRect(row, 0, true);
            if (!rowRect.intersects(clip)) {
                continue;
            }

            if (model != null && model.isDetailRow(row)) {
                if (table instanceof ApprovalTable approvalTable) {
                    ApprovalDetailPanel panel = approvalTable.detailPanel(model.entryIndex(row));
                    rendererPane.paintComponent(graphics, panel, table,
                            0, rowRect.y, Math.max(totalWidth, table.getWidth()), rowRect.height, true);
                }
                continue;
            }

            for (int column = 0; column < columnCount; column++) {
                Rectangle cellRect = table.getCellRect(row, column, true);
                if (!cellRect.intersects(clip)) {
                    continue;
                }

                if (table.isEditing() && table.getEditingRow() == row
                        && table.getEditingColumn() == column) {
                    Component editor = table.getEditorComponent();
                    if (editor != null) {
                        editor.setBounds(cellRect);
                        editor.validate();
                    }
                    continue;
                }

                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component rendererComponent = table.prepareRenderer(renderer, row, column);
                rendererPane.paintComponent(graphics, rendererComponent, table,
                        cellRect.x, cellRect.y, cellRect.width, cellRect.height, true);
            }
        }

        if (table.getShowVerticalLines()) {
            graphics.setColor(table.getGridColor());
            for (int row = 0; row < rowCount; row++) {
                if (model != null && model.isDetailRow(row)) {
                    continue;
                }
                Rectangle rowRect = table.getCellRect(row, 0, true);
                if (!rowRect.intersects(clip)) {
                    continue;
                }
                for (int column = 0; column < columnCount; column++) {
                    int x = table.getCellRect(row, column, true).x;
                    graphics.drawLine(x, rowRect.y, x, rowRect.y + rowRect.height - 1);
                }
                graphics.drawLine(totalWidth - 1, rowRect.y, totalWidth - 1, rowRect.y + rowRect.height - 1);
            }
        }

        if (table.getShowHorizontalLines()) {
            graphics.setColor(table.getGridColor());
            for (int row = 0; row < rowCount; row++) {
                Rectangle rowRect = table.getCellRect(row, 0, true);
                int y = rowRect.y + rowRect.height - 1;
                if (y >= clip.y && y < clip.y + clip.height) {
                    graphics.drawLine(clip.x, y, clip.x + clip.width - 1, y);
                }
            }
        }

        rendererPane.removeAll();
    }
}
