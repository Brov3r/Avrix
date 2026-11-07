package com.avrix.provider.zomboid.approval.ui;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Small chevron used by the language selector.
 */
public final class ApprovalChevronIcon implements Icon {
    private static final int SIZE = 12;

    /**
     * Creates the chevron icon.
     */
    public ApprovalChevronIcon() {
    }

    /**
     * Paints the dropdown chevron.
     *
     * @param component component that owns the icon
     * @param graphics  target graphics
     * @param x         left edge
     * @param y         top edge
     */
    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        ZomboidApprovalIcons.paint(g2, "arrow-down", x, y, SIZE, ZomboidApprovalTheme.MUTED);
        g2.dispose();
    }

    /**
     * Returns the chevron width.
     *
     * @return icon width in pixels
     */
    @Override
    public int getIconWidth() {
        return SIZE;
    }

    /**
     * Returns the chevron height.
     *
     * @return icon height in pixels
     */
    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
