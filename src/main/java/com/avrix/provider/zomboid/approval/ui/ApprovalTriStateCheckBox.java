package com.avrix.provider.zomboid.approval.ui;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Header checkbox that selects or clears all plugins and shows an indeterminate state for a partial selection.
 */
public final class ApprovalTriStateCheckBox extends JCheckBox {
    /**
     * Approval table model.
     */
    private final ApprovalTableModel model;

    /**
     * Callback invoked after the selection changes.
     */
    private Runnable selectionChanged = () -> {
    };

    /**
     * Whether the model contains a partial selection.
     */
    private boolean partial;

    /**
     * Creates a select-all checkbox for the given model.
     *
     * @param model approval table model
     */
    public ApprovalTriStateCheckBox(ApprovalTableModel model) {
        this.model = model;
        ZomboidApprovalTheme.styleCheckBox(this);
        setOpaque(false);
        setIcon(new TriStateIcon());
        setSelectedIcon(new TriStateIcon());
        addActionListener(event -> {
            boolean allSelected = model.entryCount() > 0
                    && model.allowedCount() == model.entryCount();
            model.setAll(!allSelected);
            updateState();
            selectionChanged.run();
        });
        updateState();
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
     * Recomputes the checked and indeterminate state from the model.
     */
    public void updateState() {
        int total = model.entryCount();
        int allowed = model.allowedCount();
        partial = total > 0 && allowed > 0 && allowed < total;
        setSelected(total > 0 && allowed == total);
        repaint();
    }

    /**
     * Paints the checked, unchecked, or indeterminate checkbox icon.
     */
    private final class TriStateIcon implements Icon {
        /**
         * Paints the tri-state checkbox icon.
         *
         * @param component component that owns the icon
         * @param graphics  target graphics
         * @param x         left edge
         * @param y         top edge
         */
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            ZomboidApprovalTheme.paintCheckBox(
                    (Graphics2D) graphics,
                    x,
                    y,
                    isSelected(),
                    partial,
                    getModel().isRollover(),
                    isEnabled()
            );
        }

        /**
         * Returns the icon width.
         *
         * @return icon width in pixels
         */
        @Override
        public int getIconWidth() {
            return ZomboidApprovalTheme.CHECKBOX_SIZE;
        }

        /**
         * Returns the icon height.
         *
         * @return icon height in pixels
         */
        @Override
        public int getIconHeight() {
            return ZomboidApprovalTheme.CHECKBOX_SIZE;
        }
    }
}
