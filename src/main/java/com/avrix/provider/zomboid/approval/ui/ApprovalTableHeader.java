package com.avrix.provider.zomboid.approval.ui;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Table header that draws column separators independently of the header cell renderer border.
 */
public final class ApprovalTableHeader extends JTableHeader {

    /**
     * Creates a table header for the given column model.
     *
     * @param columns column model
     */
    public ApprovalTableHeader(TableColumnModel columns) {
        super(columns);
    }

    /**
     * Paints the header and its column separators.
     *
     * @param graphics target graphics
     */
    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        TableColumnModel columns = getColumnModel();
        if (columns.getColumnCount() <= 1) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(ZomboidApprovalTheme.BORDER_STRONG);
        int x = 0;
        for (int index = 0; index < columns.getColumnCount() - 1; index++) {
            x += columns.getColumn(index).getWidth();
            g2.drawLine(x - 1, 0, x - 1, getHeight() - 1);
        }
        g2.dispose();
    }
}
