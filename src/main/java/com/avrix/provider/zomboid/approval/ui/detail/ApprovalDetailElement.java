package com.avrix.provider.zomboid.approval.ui.detail;

import java.awt.Graphics2D;

/**
 * One element rendered inside an expanded approval detail panel.
 */
public interface ApprovalDetailElement {

    /**
     * Returns the element height.
     *
     * @return height in pixels
     */
    public int height();

    /**
     * Paints the element.
     *
     * @param graphics target graphics
     * @param x        left edge
     * @param y        top edge
     * @param width    available width
     */
    public void paint(Graphics2D graphics, int x, int y, int width);
}
