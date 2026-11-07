package com.avrix.provider.zomboid.approval.ui.detail;

import com.avrix.provider.zomboid.approval.ui.ZomboidApprovalTheme;

import java.awt.Graphics2D;

/**
 * Detail panel divider line.
 */
public final class ApprovalDividerElement implements ApprovalDetailElement {

    /**
     * Creates a divider element.
     */
    public ApprovalDividerElement() {
    }

    /**
     * Returns the divider height.
     *
     * @return height in pixels
     */
    @Override
    public int height() {
        return 9;
    }

    /**
     * Paints the divider line.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param width    available width
     */
    @Override
    public void paint(Graphics2D graphics, int x, int y, int width) {
        graphics.setColor(ZomboidApprovalTheme.BORDER);
        graphics.drawLine(x, y + 4, x + width, y + 4);
    }
}
