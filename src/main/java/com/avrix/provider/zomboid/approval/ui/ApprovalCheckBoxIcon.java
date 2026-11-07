package com.avrix.provider.zomboid.approval.ui;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Draws a rounded checkbox with a vector check mark.
 */
public final class ApprovalCheckBoxIcon implements Icon {

    /**
     * Creates the checkbox icon.
     */
    public ApprovalCheckBoxIcon() {
    }

    /**
     * Paints the checkbox state for the current button model.
     *
     * @param component button that owns the icon
     * @param graphics  target graphics
     * @param x         left edge
     * @param y         top edge
     */
    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        AbstractButton button = (AbstractButton) component;
        ZomboidApprovalTheme.paintCheckBox(
                (Graphics2D) graphics,
                x,
                y,
                button.isSelected(),
                button.getModel().isRollover(),
                button.isEnabled()
        );
    }

    /**
     * Returns the checkbox icon width.
     *
     * @return icon width in pixels
     */
    @Override
    public int getIconWidth() {
        return ZomboidApprovalTheme.CHECKBOX_SIZE;
    }

    /**
     * Returns the checkbox icon height.
     *
     * @return icon height in pixels
     */
    @Override
    public int getIconHeight() {
        return ZomboidApprovalTheme.CHECKBOX_SIZE;
    }
}
