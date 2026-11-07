package com.avrix.provider.zomboid.approval.ui;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Basic button UI that paints a rounded background.
 */
public final class ApprovalButtonUi extends BasicButtonUI {
    private static final int ARC = 12;

    /**
     * Creates the rounded button UI.
     */
    public ApprovalButtonUi() {
    }

    /**
     * Paints the rounded button background before the standard button content.
     *
     * @param graphics  target graphics
     * @param component button being painted
     */
    @Override
    public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background = button.isEnabled()
                ? button.getBackground()
                : ZomboidApprovalTheme.blend(button.getBackground(), ZomboidApprovalTheme.WINDOW, 0.55f);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, component.getWidth() - 1, component.getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paint(graphics, component);
    }
}
