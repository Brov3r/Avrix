package com.avrix.provider.zomboid.approval.ui;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Row header that owns the per-plugin allow checkbox outside the table's cell renderer pipeline.
 */
public final class ApprovalRowHeader extends JComponent {

    /**
     * Row header width in pixels.
     */
    public static final int WIDTH = 52;

    /**
     * Table that owns the row header.
     */
    private final ApprovalTable table;

    /**
     * Table model.
     */
    private final ApprovalTableModel model;

    /**
     * Callback invoked after the selection changes.
     */
    private Runnable selectionChanged = () -> {
    };

    /**
     * Row under the pointer, or {@code -1} when no row is hovered.
     */
    private int hoverRow = -1;

    /**
     * Creates a row header for the given table and model.
     *
     * @param table approval table
     * @param model approval table model
     */
    public ApprovalRowHeader(ApprovalTable table, ApprovalTableModel model) {
        this.table = table;
        this.model = model;
        setOpaque(true);
        setBackground(ZomboidApprovalTheme.SURFACE);
        addMouseListener(new MouseAdapter() {
            /**
             * Toggles the allow checkbox for the clicked row.
             *
             * @param event mouse event
             */
            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                int row = rowAt(event.getY());
                if (row >= 0 && !model.isDetailRow(row)) {
                    model.toggleAllowed(model.entryIndex(row));
                    selectionChanged.run();
                    repaint();
                }
            }

            /**
             * Clears the hovered row when the pointer leaves.
             *
             * @param event mouse event
             */
            @Override
            public void mouseExited(MouseEvent event) {
                if (hoverRow != -1) {
                    hoverRow = -1;
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            /**
             * Tracks the hovered row and updates the cursor.
             *
             * @param event mouse event
             */
            @Override
            public void mouseMoved(MouseEvent event) {
                int row = rowAt(event.getY());
                if (row != hoverRow) {
                    hoverRow = row;
                    repaint();
                }
                boolean actionable = row >= 0 && !model.isDetailRow(row);
                setCursor(Cursor.getPredefinedCursor(
                        actionable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR
                ));
            }
        });
    }

    /**
     * Registers a callback invoked after the selection changes.
     *
     * @param selectionChanged callback, or {@code null} to clear it
     */
    public void setSelectionChanged(Runnable selectionChanged) {
        this.selectionChanged = selectionChanged == null ? () -> {
        } : selectionChanged;
    }

    /**
     * Returns the preferred size matching the table height.
     *
     * @return preferred row header size
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, Math.max(0, table.getPreferredSize().height));
    }

    /**
     * Paints the row header checkboxes and separators.
     *
     * @param graphics target graphics
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Rectangle clip = graphics.getClipBounds();
        if (clip == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(ZomboidApprovalTheme.SURFACE);
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);

        for (int row = 0; row < table.getRowCount(); row++) {
            Rectangle rowRect = table.getCellRect(row, 0, true);
            if (!rowRect.intersects(clip)) {
                continue;
            }

            boolean detail = model.isDetailRow(row);
            g2.setColor(detail
                    ? ZomboidApprovalTheme.DETAIL_BG
                    : (row % 2 == 0 ? ZomboidApprovalTheme.SURFACE : ZomboidApprovalTheme.SURFACE_ALT));
            g2.fillRect(0, rowRect.y, WIDTH, rowRect.height);

            if (!detail) {
                int x = (WIDTH - ZomboidApprovalTheme.CHECKBOX_SIZE) / 2;
                int y = rowRect.y + (rowRect.height - ZomboidApprovalTheme.CHECKBOX_SIZE) / 2;
                ZomboidApprovalTheme.paintCheckBox(
                        g2,
                        x,
                        y,
                        model.isAllowed(model.entryIndex(row)),
                        hoverRow == row,
                        true
                );
            }

            g2.setColor(ZomboidApprovalTheme.BORDER);
            int lineY = rowRect.y + rowRect.height - 1;
            g2.drawLine(0, lineY, WIDTH - 1, lineY);
        }

        g2.setColor(ZomboidApprovalTheme.BORDER);
        g2.drawLine(WIDTH - 1, clip.y, WIDTH - 1, clip.y + clip.height - 1);
        g2.dispose();
    }

    private int rowAt(int y) {
        for (int row = 0; row < table.getRowCount(); row++) {
            Rectangle rowRect = table.getCellRect(row, 0, true);
            if (y >= rowRect.y && y < rowRect.y + rowRect.height) {
                return row;
            }
        }
        return -1;
    }
}
